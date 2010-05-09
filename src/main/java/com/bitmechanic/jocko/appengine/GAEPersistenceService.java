package com.bitmechanic.jocko.appengine;

import com.bitmechanic.jocko.AbstractBlobBackedPersistenceService;
import com.bitmechanic.jocko.FilterOption;
import com.bitmechanic.jocko.Infrastructure;
import com.bitmechanic.jocko.PaginatedResult;
import com.bitmechanic.jocko.PaginationState;
import com.bitmechanic.jocko.PersistUtil;
import com.bitmechanic.jocko.PersistableEntity;
import com.bitmechanic.jocko.PersistenceService;
import com.bitmechanic.jocko.Query;
import com.bitmechanic.util.Contract;
import com.bitmechanic.util.DesCipher;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 28, 2010
 */
public class GAEPersistenceService extends AbstractBlobBackedPersistenceService {

    private static final Logger log = Logger.getLogger(GAEPersistenceService.class);

    private interface AddEntityCallback {
        void addEntity(List list, Entity entity);
    }

    public GAEPersistenceService() {
        cipherKey = null;
    }

    @Override
    public void deleteMetadata(Collection<String> uuids) {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        Key keys[] = new Key[uuids.size()];
        int i = 0;
        for (String uuid : uuids) {
            keys[i] = EntityMetadata.createKeyForUUID(uuid);
            i++;
        }
        ds.delete(keys);
    }

    @Override
    public void putMetadata(PersistableEntity entity, boolean isNewEntity) {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

        EntityMetadata meta = new EntityMetadata(entity);
        Entity dsEntity = meta.toDatastoreEntity();
        ds.put(dsEntity);
    }

    @Override
    public String put(PersistableEntity entity) {
        Contract.notNull(infrastructure, "infrastructure cannot be null");

        String uuid = entity.getId();
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            entity.setId(uuid);
        }

        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

        EntityMetadata meta = new EntityMetadata(entity);
        Entity dsEntity = meta.toDatastoreEntity();
        Key key = ds.put(dsEntity);

        try {
            byte data[] = PersistUtil.objectToJson(entity);
            if (cipherKey != null) {
                DesCipher cipher = new DesCipher(cipherKey);
                data = cipher.encryptBytes(data);
            }

            infrastructure.getBlobService().put(uuid, "text/plain", false, data);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        return key.getName();
    }

    @Override
    public <T> PaginatedResult<String> queryForIdsPaginated(Query<T> query) {
        return queryForIdsInternal(query, true);
    }

    private <T> PaginatedResult<String> queryForIdsInternal(Query<T> query, boolean paginated) {
        Contract.notNull(infrastructure, "infrastructure cannot be null");
        
        return (PaginatedResult<String>)queryInternal(query, paginated, new AddEntityCallback() {
            @Override
            public void addEntity(List list, Entity entity) {
                list.add(entity.getKey().getName());
            }
        });
    }

    @Override
    public int queryCount(Query<? extends PersistableEntity> query) {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = ds.prepare(toDatastoreQuery(query, true));
        return pq.countEntities();
    }

    @Override
    public <T> PaginatedResult<T> queryPaginated(Query<T> query) {
        return queryInternal(query, true);
    }

    private <T> PaginatedResult<T> queryInternal(Query<T> query, boolean paginated) {
        return queryInternal(query, paginated, new AddEntityCallback() {
            @Override
            public void addEntity(List list, Entity entity) {
                list.add(get(entity.getKey().getName()));
            }
        });
    }

    private <T> PaginatedResult<T> queryInternal(Query<T> query, boolean paginated, AddEntityCallback callback) {
        if (paginated && !query.hasSortProperty()) {
            throw new IllegalArgumentException("Paginated queries must define a sort property");
        }

        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = ds.prepare(toDatastoreQuery(query, false));

        PaginationState paginationState = query.getPaginationState();
        int addToMaxResults   = paginated ? 1 : 0;

        List<Entity> entities = pq.asList(toFetchOptions(query, addToMaxResults));
        ArrayList<T> peList = new ArrayList<T>(entities.size());
        for (Entity entity : entities) {
            if (peList.size() < query.getMaxResults()) {
                callback.addEntity(peList, entity);
            }
            else if (paginated) {
                Object value = entity.getProperty(query.getSortProperty());
                paginationState.addToken(toTokenString(value));
            }
        }
        
        return new PaginatedResult(peList, paginationState);
    }

    @Override
    public <T> List<T> query(Query<T> query) {
        return queryInternal(query, false).getCurrentPage();
    }

    @Override
    public <T> List<String> queryForIds(Query<T> query) {
        return queryForIdsInternal(query, false).getCurrentPage();
    }

    private com.google.appengine.api.datastore.Query toDatastoreQuery(Query query, boolean keysOnly) {
        com.google.appengine.api.datastore.Query dsQuery = new com.google.appengine.api.datastore.Query(EntityMetadata.ENTITY_KIND);

        if (keysOnly)
            dsQuery.setKeysOnly();

        dsQuery.addFilter(EntityMetadata.KEY_ENTITY_CLASS, com.google.appengine.api.datastore.Query.FilterOperator.EQUAL, query.getType().getCanonicalName());
        
        String sortProp = query.getSortProperty();
        if (sortProp != null) {
            com.google.appengine.api.datastore.Query.SortDirection dir = com.google.appengine.api.datastore.Query.SortDirection.ASCENDING;
            if (query.getSortDir() == Query.Direction.descending)
                dir = com.google.appengine.api.datastore.Query.SortDirection.DESCENDING;

            dsQuery.addSort(sortProp, dir);

            PaginationState state = query.getPaginationState();
            if (state.hasCurrentPageToken()) {
                Object token = fromTokenString(state.getCurrentPageToken());
                if (query.getSortDir() == Query.Direction.descending)
                    query.lessThanOrEquals(sortProp, token);
                else
                    query.greaterThanOrEquals(sortProp, token);
            }
        }

        List<FilterOption> filters = query.getFilters();
        for (FilterOption filter : filters) {
            String property = filter.getProperty();
            Object value    = filter.getValue();

            if (filter.getOperator() == FilterOption.Operator.EQUALS) {
                dsQuery.addFilter(property, com.google.appengine.api.datastore.Query.FilterOperator.EQUAL, value);
            }
            else if (filter.getOperator() == FilterOption.Operator.NOT_EQUALS) {
                dsQuery.addFilter(property, com.google.appengine.api.datastore.Query.FilterOperator.NOT_EQUAL, value);
            }
            else if (filter.getOperator() == FilterOption.Operator.CONTAINS) {
                dsQuery.addFilter(property, com.google.appengine.api.datastore.Query.FilterOperator.IN, value);
            }
            else if (filter.getOperator() == FilterOption.Operator.LESS_THAN) {
                dsQuery.addFilter(property, com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN, value);
            }
            else if (filter.getOperator() == FilterOption.Operator.LESS_THAN_EQUALS) {
                dsQuery.addFilter(property, com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN_OR_EQUAL, value);
            }
            else if (filter.getOperator() == FilterOption.Operator.GREATER_THAN) {
                dsQuery.addFilter(property, com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN, value);
            }
            else if (filter.getOperator() == FilterOption.Operator.GREATER_THAN_EQUALS) {
                dsQuery.addFilter(property, com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL, value);
            }
            else {
                throw new IllegalArgumentException("Unsupported operator: " + filter.getOperator());
            }
        }

        return dsQuery;
    }

    private EntityMetadata getEntityMetadataById(String uuid) {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        try {
            Entity entity = ds.get(EntityMetadata.createKeyForUUID(uuid));
            return new EntityMetadata(entity);
        }
        catch (EntityNotFoundException e) {
            return null;
        }
    }

    private FetchOptions toFetchOptions(Query query, int addToMax) {
        return FetchOptions.Builder.withLimit(query.getMaxResults() + addToMax).offset(query.getOffset());
    }

    private String toTokenString(Object obj) {
        if (obj instanceof Integer) {
            return "int:" + obj;
        }
        else if (obj instanceof Long) {
            return "long:" + obj;
        }
        else if (obj instanceof Float) {
            return "float:" + obj;
        }
        else if (obj instanceof Double) {
            return "double:" + obj;
        }
        else {
            return "string:" + obj.toString();
        }
    }

    private Object fromTokenString(String str) {
        int pos = str.indexOf(":");
        
        if (pos > -1) {

            String token = str.substring(0, pos);
            String value = str.substring(pos+1);

            if (token.equals("int"))
                return Integer.parseInt(value);
            else if (token.equals("long"))
                return Long.parseLong(value);
            else if (token.equals("float"))
                return Float.parseFloat(value);
            else if (token.equals("double"))
                return Double.parseDouble(value);
            else if (token.equals("string"))
                return value;
            else
                throw new IllegalArgumentException("Unknown token: " + token);
        }
        else {
            throw new IllegalArgumentException("Invalid token: " + str);
        }
    }

}

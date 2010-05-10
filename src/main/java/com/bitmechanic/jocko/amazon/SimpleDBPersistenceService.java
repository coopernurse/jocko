package com.bitmechanic.jocko.amazon;

import com.amazonaws.sdb.AmazonSimpleDBClient;
import com.amazonaws.sdb.AmazonSimpleDBException;
import com.amazonaws.sdb.model.CreateDomainRequest;
import com.amazonaws.sdb.model.DeleteAttributesRequest;
import com.amazonaws.sdb.model.DeleteDomainRequest;
import com.amazonaws.sdb.model.Item;
import com.amazonaws.sdb.model.ListDomainsRequest;
import com.amazonaws.sdb.model.ListDomainsResponse;
import com.amazonaws.sdb.model.PutAttributesRequest;
import com.amazonaws.sdb.model.ReplaceableAttribute;
import com.amazonaws.sdb.model.SelectRequest;
import com.amazonaws.sdb.model.SelectResponse;
import com.amazonaws.sdb.model.SelectResult;
import com.bitmechanic.jocko.AbstractBlobBackedPersistenceService;
import com.bitmechanic.jocko.FilterOption;
import com.bitmechanic.jocko.NonTypedPersistenceService;
import com.bitmechanic.jocko.PaginatedResult;
import com.bitmechanic.jocko.PaginationState;
import com.bitmechanic.jocko.PersistableEntity;
import com.bitmechanic.jocko.Query;
import com.bitmechanic.util.Contract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Feb 26, 2010
 */
public class SimpleDBPersistenceService extends NonTypedPersistenceService {

    private String awsAccessId;
    private String awsSecretKey;
    private String domain;

    public SimpleDBPersistenceService(String awsAccessId, String awsSecretKey, String domain) {
        Contract.notNullOrEmpty(awsAccessId, "awsAccessId cannot be empty");
        Contract.notNullOrEmpty(awsSecretKey, "awsSecretKey cannot be empty");
        Contract.notNullOrEmpty(domain, "domain cannot be empty");
        this.awsAccessId = awsAccessId;
        this.awsSecretKey = awsSecretKey;
        this.domain = domain;

        createDomainIfNotExist();
    }

    private void createDomainIfNotExist() {
        AmazonSimpleDBClient client = getAmazonSimpleDBClient();
        try {
            boolean exists = false;
            ListDomainsResponse domains = client.listDomains(new ListDomainsRequest(100, null));
            for (String name : domains.getListDomainsResult().getDomainName()) {
                if (name.equals(this.domain)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                client.createDomain(new CreateDomainRequest(this.domain));
            }
        }
        catch (AmazonSimpleDBException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAndCreateDomain() {
        try {
            execWithBackoff(new SimpleDBCommand() {
                @Override
                public Object exec(AmazonSimpleDBClient client) throws AmazonSimpleDBException {
                    client.deleteDomain(new DeleteDomainRequest(domain));
                    return null;
                }
            });
        }
        catch (AmazonSimpleDBException e) {
            // ok -- domain may not exist
        }

        try {
            execWithBackoff(new SimpleDBCommand() {
                @Override
                public Object exec(AmazonSimpleDBClient client) throws AmazonSimpleDBException {
                    client.createDomain(new CreateDomainRequest(domain));
                    return null;
                }
            });
        }
        catch (AmazonSimpleDBException e) {
            throw new RuntimeException(e);
        }
    }

    private Object execWithBackoff(SimpleDBCommand command) throws AmazonSimpleDBException {
        AmazonSimpleDBClient client = getAmazonSimpleDBClient();

        long timeout   = System.currentTimeMillis() + 15000;
        long sleepTime = 250;
        
        while (System.currentTimeMillis() < timeout) {
            try {
                return command.exec(client);
            }
            catch (AmazonSimpleDBException e) {
                if (e.getStatusCode() == 503) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    sleepTime = sleepTime * 2;
                }
                else {
                    throw e;
                }
            }
        }

        throw new AmazonSimpleDBException("execWithBackoff: timeout exceeded");
    }

    @Override
    public void deleteMetadata(Collection<String> uuids) {
        try {
            AmazonSimpleDBClient client = getAmazonSimpleDBClient();
            for (String uuid : uuids) {
                DeleteAttributesRequest request = new DeleteAttributesRequest(domain, uuid, null, null);
                client.deleteAttributes(request);
            }
        }
        catch (AmazonSimpleDBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void putMetadata(PersistableEntity entity, boolean isNewEntity) {
        PutAttributesRequest putAttrib = new PutAttributesRequest();
        putAttrib.setDomainName(domain);
        putAttrib.setItemName(entity.getId());
        putAttrib.setAttribute(toAttributeList(entity, isNewEntity));
        try {
            getAmazonSimpleDBClient().putAttributes(putAttrib);
        }
        catch (AmazonSimpleDBException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ReplaceableAttribute> toAttributeList(PersistableEntity entity, boolean isNewEntity) {
        ArrayList<ReplaceableAttribute> list = new ArrayList<ReplaceableAttribute>();

        HashMap<String, Object> meta = new HashMap<String, Object>();
        entity.fillMetaData(meta);
        meta.put(KEY_ENTITY_CLASS, entity.getClass().getCanonicalName());

        for (String key : meta.keySet()) {
            Object value = meta.get(key);
            if (value != null) {
                if (value instanceof Collection) {
                    Collection c = (Collection)value;
                    for (Object obj : c) {
                        list.add(new ReplaceableAttribute(key, toSimpleDbString(obj), !isNewEntity));
                    }
                }
                else {
                    list.add(new ReplaceableAttribute(key, toSimpleDbString(value), !isNewEntity));
                }
            }
        }

        return list;
    }

    public int queryCount(Query<? extends PersistableEntity> query) {
        SelectRequest request = toSelectRequest(query, true);
        try {
            SelectResponse response = getAmazonSimpleDBClient().select(request);
            return Integer.parseInt(response.getSelectResult().getItem().get(0).getAttribute().get(0).getValue());
        }
        catch (AmazonSimpleDBException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<String> queryForIds(Query<T> query) {
        return queryAllInternal(query, true);
    }

    public <T> PaginatedResult<String> queryForIdsPaginated(Query<T> query) {
        return queryPaginatedInternal(query, true);
    }

    public <T> List<T> query(Query<T> query) {
        return queryAllInternal(query, false);
    }

    private List queryAllInternal(Query query, boolean idsOnly) {
        ArrayList list = new ArrayList();

        while (true) {
            PaginatedResult page = queryPaginatedInternal(query, idsOnly);
            if (page.getCurrentPage().size() > 0) {
                list.addAll(page.getCurrentPage());
            }

            if (page.getPaginationState().hasNext() && (list.size() < query.getMaxResults() || query.getMaxResults() == 0)) {
                query.getPaginationState().getNextPage();
            }
            else {
                break;
            }
        }

        return list;
    }

    public <T> PaginatedResult<T> queryPaginated(Query<T> query) {
        return queryPaginatedInternal(query, false);
    }

    private PaginatedResult queryPaginatedInternal(Query query, boolean idsOnly) {
        SelectRequest request = toSelectRequest(query, false);
        try {
            SelectResponse response = getAmazonSimpleDBClient().select(request);

            List currentPage = new ArrayList();
            PaginationState paginationState = query.getPaginationState();

            SelectResult result = response.getSelectResult();
            for (Item item : result.getItem()) {
                String uuid = item.getName();
                if (idsOnly) {
                    currentPage.add(uuid);
                }
                else {
                    Object obj = get(uuid);
                    if (obj != null)
                        currentPage.add(obj);
                }
            }

            if (result.getNextToken() != null)
                paginationState.addToken(result.getNextToken());

            return new PaginatedResult(currentPage, paginationState);
        }
        catch (AmazonSimpleDBException e) {
            throw new RuntimeException(e);
        }
    }

    private SelectRequest toSelectRequest(Query query, boolean countOnly) {
        String className = toSimpleDbString(query.getType().getCanonicalName());

        String selectAttrib = countOnly ? "count(*)" : "itemName()";

        StringBuilder sb = new StringBuilder();
        sb.append("select ").append(selectAttrib).append(" from ");
        sb.append(domain);
        sb.append(" where `").append(KEY_ENTITY_CLASS).append("`='").append(className).append("'");

        Set<String> filterProps = new HashSet<String>();

        List<FilterOption> filters = query.getFilters();
        for (FilterOption filter : filters) {

            String property = filter.getProperty();
            if (filterProps.size() == 0) {
                sb.append(" and (");
            }
            else {
                sb.append(" ").append(query.getCondition().name());
            }

            sb.append(" `").append(property).append("` ");

            if (!filterProps.contains(property))
                filterProps.add(property);

            String value = toSimpleDbString(filter.getValue());

            FilterOption.Operator operator = filter.getOperator();
            if (operator.equals(FilterOption.Operator.EQUALS)) {
                sb.append("=");
            }
            else if (operator.equals(FilterOption.Operator.NOT_EQUALS)) {
                sb.append("!=");
            }
            else if (operator.equals(FilterOption.Operator.CONTAINS)) {
                sb.append("like");
                value = "%" + escapeStr(filter.getValue()) + "%";
            }
            else if (operator.equals(FilterOption.Operator.NOT_CONTAINS)) {
                sb.append("not like");
                value = "%" + escapeStr(filter.getValue()) + "%";
            }
            else if (operator.equals(FilterOption.Operator.GREATER_THAN)) {
                sb.append(">");
            }
            else if (operator.equals(FilterOption.Operator.LESS_THAN)) {
                sb.append("<");
            }
            else if (operator.equals(FilterOption.Operator.GREATER_THAN_EQUALS)) {
                sb.append(">=");
            }
            else if (operator.equals(FilterOption.Operator.LESS_THAN_EQUALS)) {
                sb.append("<=");
            }
            else {
                throw new IllegalArgumentException("Unsupported operator: " + operator.name());
            }
            
            sb.append("'").append(value).append("'");
        }

        if (filterProps.size() > 0) {
            sb.append(" ) ");
        }

        if (!countOnly) {

            if (query.hasSortProperty()) {
                String sortProperty = query.getSortProperty();
                if (!filterProps.contains(sortProperty))
                    sb.append(" ").append(query.getCondition().name()).append(" `").append(sortProperty).append("` is not null");

                sb.append(" order by `").append(sortProperty).append("` ");
                if (query.getSortDir() == Query.Direction.ascending)
                    sb.append("asc");
                else
                    sb.append("desc");
            }

            if (query.getMaxResults() > 0) {
                sb.append(" limit ").append(query.getMaxResults());
            }

        }

        String selectExpression = sb.toString();
        return new SelectRequest(selectExpression, query.getPaginationState().getCurrentPageToken(), getConsistentRead(selectExpression));
    }

    private boolean getConsistentRead(String selectExpression) {
        // TODO: implement a callback for this
        return true;
    }

    //////////////////////////////////////

    private AmazonSimpleDBClient getAmazonSimpleDBClient() {
        return new AmazonSimpleDBClient(awsAccessId, awsSecretKey);
    }

    private interface SimpleDBCommand {
        Object exec(AmazonSimpleDBClient client) throws AmazonSimpleDBException;
    }

}

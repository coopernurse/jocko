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
public class SimpleDBPersistenceService extends AbstractBlobBackedPersistenceService {

    public static final String KEY_ENTITY_CLASS = "_entity_class";

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

    private static final int INT_LENGTH = String.valueOf(Integer.MAX_VALUE).length();
    private static final String INT_NEG_FORMAT = "!I%0" + INT_LENGTH + "d";
    private static final String INT_POS_FORMAT = "!i%0" + INT_LENGTH + "d";

    private static final int SHORT_LENGTH = String.valueOf(Short.MAX_VALUE).length();
    private static final String SHORT_NEG_FORMAT = "!T%0" + SHORT_LENGTH + "d";
    private static final String SHORT_POS_FORMAT = "!t%0" + SHORT_LENGTH + "d";

    private static final int LONG_LENGTH = String.valueOf(Long.MAX_VALUE).length();
    private static final String LONG_NEG_FORMAT = "!L%0" + LONG_LENGTH + "d";
    private static final String LONG_POS_FORMAT = "!l%0" + LONG_LENGTH + "d";

    private static final int FLOAT_LENGTH = String.valueOf(Integer.MAX_VALUE).length() + 1;
    private static final String FLOAT_FORMAT = "!f%0" + FLOAT_LENGTH + "d";

    private static final int DOUBLE_LENGTH = String.valueOf(Long.MAX_VALUE).length() + 1;
    private static final String DOUBLE_FORMAT = "!d%0" + DOUBLE_LENGTH + "d";

    protected String escapeStr(Object obj) {
        if (obj == null)
            return null;
        else
            return obj.toString().replace("'", "''");
    }

    protected String toSimpleDbString(Object obj) {
        Contract.notNull(obj, "obj cannot be null");

        if (obj instanceof String) {
            String str = (String)obj;
            return "!s" + escapeStr(str);
        } else if (obj instanceof Integer) {
            int num = (Integer) obj;
            if (num < 0)
                return String.format(INT_NEG_FORMAT, Integer.MAX_VALUE + num);
            else
                return String.format(INT_POS_FORMAT, num);
        } else if (obj instanceof Short) {
            int num = (Short) obj;
            if (num < 0)
                return String.format(SHORT_NEG_FORMAT, Short.MAX_VALUE + num);
            else
                return String.format(SHORT_POS_FORMAT, num);
        } else if (obj instanceof Long) {
            return longToStr((Long) obj);
        } else if (obj instanceof Float) {
            int num = Float.floatToRawIntBits((Float) obj);
            return String.format(FLOAT_FORMAT, num);
        } else if (obj instanceof Double) {
            long num = Double.doubleToRawLongBits((Double) obj);
            return String.format(DOUBLE_FORMAT, num);
        } else if (obj instanceof Boolean) {
            boolean b = (Boolean) obj;
            return b ? "!b" : "!B";
        } else if (obj instanceof Date) {
            return "!z" + longToStr(((Date) obj).getTime());
        } else {
            throw new IllegalArgumentException("Unable to encode object of class: " + obj.getClass().getCanonicalName());
        }
    }

    protected Object fromSimpleDbString(String str) {
        if (str == null) {
            return null;
        } else if (str.startsWith("!s")) {
            return str.substring(2);
        } else if (str.startsWith("!I") || str.startsWith("!i")) {
            return strToInt(str);
        } else if (str.startsWith("!L") || str.startsWith("!l")) {
            return strToLong(str);
        } else if (str.startsWith("!f")) {
            return Float.intBitsToFloat(strToInt(str));
        } else if (str.startsWith("!d")) {
            return Double.longBitsToDouble(strToLong(str));
        } else if (str.startsWith("!z")) {
            return new Date(strToLong(str.substring(2)));
        } else if (str.equals("!B")) {
            return false;
        } else if (str.equals("!b")) {
            return true;
        } else if (str.startsWith("!t") || str.startsWith("!T")) {
            return strToShort(str);
        } else {
            throw new IllegalArgumentException("Unknown encoding for value: " + str);
        }
    }

    private String longToStr(long num) {
        if (num < 0)
            return String.format(LONG_NEG_FORMAT, Long.MAX_VALUE + num);
        else
            return String.format(LONG_POS_FORMAT, num);
    }

    private int strToInt(String str) {
        if (str.startsWith("!I"))
            return Integer.parseInt(str.substring(2)) - Integer.MAX_VALUE;
        else
            return Integer.parseInt(str.substring(2));
    }

    private long strToLong(String str) {
        if (str.startsWith("!L"))
            return Long.parseLong(str.substring(2)) - Long.MAX_VALUE;
        else
            return Long.parseLong(str.substring(2));
    }

    private short strToShort(String str) {
        if (str.startsWith("!T"))
            return (short) (Short.parseShort(str.substring(2)) - Short.MAX_VALUE);
        else
            return Short.parseShort(str.substring(2));
    }

}

package com.bitmechanic.jocko.local;

import com.amazonaws.sdb.model.ReplaceableAttribute;
import com.bitmechanic.jocko.AbstractBlobBackedPersistenceService;
import com.bitmechanic.jocko.FilterOption;
import com.bitmechanic.jocko.NonTypedPersistenceService;
import com.bitmechanic.jocko.PaginatedResult;
import com.bitmechanic.jocko.PaginationState;
import com.bitmechanic.jocko.PersistableEntity;
import com.bitmechanic.jocko.Query;
import com.bitmechanic.util.Contract;
import com.bitmechanic.util.StringUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: May 9, 2010
 */
public class HsqlPersistenceService extends NonTypedPersistenceService {

    private static final String DRIVER_NAME = "org.hsqldb.jdbcDriver";

    private Connection conn;

    public HsqlPersistenceService(String dbFilename) throws SQLException {
        Contract.notNullOrEmpty(dbFilename, "dbFilename cannot be empty");

        try {
            Class.forName(DRIVER_NAME);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Cannot find driver: " + DRIVER_NAME);
        }
        conn = DriverManager.getConnection("jdbc:hsqldb:file:" + dbFilename, "SA", "");
        createTableIfNotExists();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        conn.close();
    }

    @Override
    public void deleteMetadata(Collection<String> uuids) {
        if (uuids != null && uuids.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String id : uuids) {
                if (sb.length() > 0)
                    sb.append(",");
                else
                    sb.append("delete from JockoEntityMeta where uuid in (");

                sb.append("'").append(escapeStr(id)).append("'");
            }

            sb.append(")");

            execUpdate(sb.toString());
        }
    }

    @Override
    public void putMetadata(PersistableEntity entity, boolean isNewEntity) {
        HashMap<String, Object> meta = new HashMap<String, Object>();
        entity.fillMetaData(meta);

        String type = formatString(entity.getClass().getCanonicalName());
        String id   = formatString(entity.getId());

        if (!isNewEntity) {
            execUpdate("delete from JockoEntityMeta where uuid = " + id);
        }

        for (String field : meta.keySet()) {
            Object valueObj = meta.get(field);
            if (valueObj != null) {
                if (valueObj instanceof Collection) {
                    Collection c = (Collection)valueObj;
                    for (Object obj : c) {
                        insertAttribute(id, type, field, obj);
                    }
                }
                else {
                    insertAttribute(id, type, field, valueObj);
                }
            }
        }
    }

    private void insertAttribute(String id, String type, String field, Object valueObj) {
        String value = formatString(toSimpleDbString(valueObj));
        field = formatString(field);
        String sql = String.format("insert into JockoEntityMeta (uuid, type, field, value) values (%s, %s, %s, %s)", id, type, field, value);
        execUpdate(sql);
    }

    @Override
    public int queryCount(Query<? extends PersistableEntity> query) {
        String sql = queryToSql(query, true);
        ResultSet rs = runQuery(sql);
        try {
            if (rs.next())
                return rs.getInt(1);
            else
                return 0;
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            try { rs.close(); } catch (SQLException e) { }
        }
    }

    @Override
    public <T> List<String> queryForIds(Query<T> query) {
        return queryForIdsPaginated(query).getCurrentPage();
    }

    @Override
    public <T> List<T> query(Query<T> query) {
        return queryPaginated(query).getCurrentPage();
    }

    @Override
    public <T> PaginatedResult<String> queryForIdsPaginated(Query<T> query) {
        
        PaginationState paginationState = query.getPaginationState();

        String sql = queryToSql(query, false);
        ResultSet rs = runQuery(sql);
        try {
            ArrayList<String> list = new ArrayList<String>();
            while (rs.next()) {
                if (list.size() < query.getMaxResults())
                    list.add(rs.getString(1));
                else if (paginationState.onLastPage())
                    paginationState.addToken(String.valueOf(getNextOffset(query)));
            }

            return new PaginatedResult(list, paginationState);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            try { rs.close(); } catch (SQLException e) { }
        }
    }

    private int getNextOffset(Query query) {
        int currentPage = query.getPaginationState().getCurrentPage();
        int maxResults  = query.getMaxResults();
        return (currentPage+1) * maxResults;
    }

    @Override
    public <T> PaginatedResult<T> queryPaginated(Query<T> query) {
        PaginationState paginationState = query.getPaginationState();
        ArrayList<T> entities = new ArrayList<T>();
        List<String> ids = queryForIds(query);
        for (String id : ids) {
            Object obj = get(id);
            if (obj != null) {
                if (entities.size() < query.getMaxResults())
                    entities.add((T)obj);
                else if (paginationState.onLastPage())
                    paginationState.addToken(String.valueOf(getNextOffset(query)));
            }
        }

        return new PaginatedResult(entities, paginationState);
    }

    ////////////////////////////////////////////////////////////////

    private String queryToSql(Query query, boolean countOnly) {
        StringBuilder sb = new StringBuilder();

        StringBuilder tableClause = new StringBuilder();
        StringBuilder whereClause = new StringBuilder();

        if (countOnly) {
            sb.append("select count(distinct uuid)");
        }
        else {
            sb.append("select distinct Jocko1.uuid");

            if (query.hasSortProperty()) {
                sb.append(", SortTable.value");
                whereClause.append(" and Jocko1.uuid = SortTable.uuid");

                tableClause.append(String.format(", (select uuid, value from JockoEntityMeta where field=%s ) as SortTable", formatString(query.getSortProperty())));
            }
        }

        List<FilterOption> filters = query.getFilters();
        int filterNum = 0;
        for (FilterOption filter : filters) {

            String property  = filter.getProperty();
            String tableName = "Jocko" + (filterNum+1);

            if (filterNum == 0) {
                whereClause.append(" and ((");
                whereClause.append("Jocko1.field=").append(formatString(property)).append(" and Jocko1.value ").append(getSqlWhereValue(filter)).append(")");
            }
            else {
                tableClause.append(", (select uuid from JockoEntityMeta where field=").append(formatString(property)).append(" and value ").append(getSqlWhereValue(filter)).append(") as ").append(tableName);
                whereClause.append(" ").append(query.getCondition().name()).append(" Jocko1.uuid=").append(tableName).append(".uuid");
            }

            filterNum++;
        }

        if (filterNum > 0) {
            whereClause.append(" ) ");
        }

        sb.append(" from JockoEntityMeta as Jocko1");
        sb.append(tableClause);

        String className = query.getType().getCanonicalName();
        sb.append(" where type='").append(className).append("'");
        sb.append(whereClause);

        if (!countOnly) {

            if (query.hasSortProperty()) {
                sb.append(" order by SortTable.value ");
                if (query.getSortDir() == Query.Direction.ascending)
                    sb.append("asc");
                else
                    sb.append("desc");
            }

            if (query.getMaxResults() > 0) {
                sb.append(" limit ").append(query.getMaxResults() + 1);

                String token = query.getPaginationState().getCurrentPageToken();
                if (StringUtil.hasText(token)) {
                    sb.append("offset ").append(token);
                }
            }
        }

        return sb.toString();
    }

    private String getSqlWhereValue(FilterOption filter) {
        StringBuilder sb = new StringBuilder();

        String value = toSimpleDbString(filter.getValue());

        FilterOption.Operator operator = filter.getOperator();
        if (operator.equals(FilterOption.Operator.EQUALS)) {
            sb.append("=");
        }
        else if (operator.equals(FilterOption.Operator.NOT_EQUALS)) {
            sb.append("<>");
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

        return sb.toString();
    }

    private String formatString(String str) {
        return "'" + escapeStr(str) + "'";
    }

    private String escapeStr(String str) {
        return str.replace("'", "''");
    }

    private ResultSet runQuery(String sql) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            return stmt.executeQuery(sql);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { } }
        }
    }

    private int execUpdate(String sql) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            return stmt.executeUpdate(sql);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { } }
        }
    }

    private void createTableIfNotExists() throws SQLException {

        if (!tableExists("JockoEntityMeta")) {
            Statement stmt = conn.createStatement();
            try {
                stmt.executeUpdate("CREATE TABLE JockoEntityMeta (uuid char(36), type varchar(255), field varchar(255), value varchar(1024)); ");
                stmt.executeUpdate("CREATE INDEX uuid_idx  ON JockoEntityMeta (uuid);");
                stmt.executeUpdate("CREATE INDEX type_idx  ON JockoEntityMeta (type);");
                stmt.executeUpdate("CREATE INDEX field_idx ON JockoEntityMeta (field);");
                stmt.executeUpdate("CREATE INDEX value_idx ON JockoEntityMeta (value);");
            }
            finally {
                stmt.close();
            }
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        Statement stmt = conn.createStatement();
        try {
            stmt.executeQuery("select count(*) from " + tableName);
            return true;
        }
        catch (SQLException e) {
            return false;
        }
        finally {
            stmt.close();
        }

    }
}

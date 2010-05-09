package com.bitmechanic.jocko;

import com.bitmechanic.util.Contract;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for all persistent entity objects in the system.
 *
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Nov 8, 2009
 */
public abstract class BaseEntity implements PersistableEntity, Comparable {

    private static final long serialVersionUID = 1L;

    private static final int MAX_RETRIES = 5;
    private static final long DEFAULT_MAX_TIME_TO_WAIT_MILLIS = 11000;
    private static final long DEFAULT_LOCK_TIMEOUT_MILLIS = 10000;

    private static final Logger log = Logger.getLogger(BaseEntity.class);

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> listToMap(List<V> list) {
        HashMap<K, V> map = new HashMap<K, V>();
        for (V e : list) {
            map.put((K)((BaseEntity)e).getId(), e);
        }
        return map;
    }

    public static void attachInfrastructure(Infrastructure infra, BaseEntity... entities) {
        for (BaseEntity entity : entities)
            entity.setInfrastructure(infra);
    }

    protected transient Infrastructure infrastructure;

    protected String uuid;

    // auto-set by save
    private long dateCreated = 0;
    private long dateUpdated = 0;

    // tracks EtlJob classes that have been run for this entity
    private Set<String> etlJobsRun;

    public String getId() {
        return uuid;
    }

    public void setId(String uuid) {
        this.uuid = uuid;
    }

    public boolean hasId() {
        return uuid != null && uuid.length() > 0;
    }

    public void setInfrastructure(Infrastructure infrastructure) {
        this.infrastructure = infrastructure;
    }

    public Infrastructure getInfrastructure() {
        return this.infrastructure;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public long getDateUpdated() {
        return dateUpdated;
    }

    public void fillMetaData(Map<String, Object> map) {
        map.put("dateCreated", dateCreated);
        map.put("dateUpdated", dateUpdated);
    }

    public static String toLower(String str) {
        return (str == null) ? null : str.toLowerCase();
    }

    public void onAfterGet() {
        // no-op by default.  subclasses can override
    }

    public void onBeforePut() {
        // no-op by default.  subclasses can override
    }

    public Collection<? extends EtlJob> getEtlJobs() {
        // no-op by default.  subclasses can override
        return null;
    }

    public PaginatedResult<? extends BaseEntity> getChildren(int maxResults) {
        // no-op.  subclasses can override
        return null;
    }

    public void lockEntityWithDefaultTimeouts() {
        lockEntity(DEFAULT_MAX_TIME_TO_WAIT_MILLIS, DEFAULT_LOCK_TIMEOUT_MILLIS);
    }

    public void lockEntity(long maxTimeToWaitMillis, long lockTimeoutMillis) {
        Contract.notNull(infrastructure, "infrastructure cannot be null");
        Contract.notNull(infrastructure.getLockService(), "lockService cannot be null");
        Contract.notNullOrEmpty(uuid, "uuid cannot be empty");

        infrastructure.getLockService().getLock(getLockId(), maxTimeToWaitMillis, lockTimeoutMillis);
    }

    public void unlockEntity() {
        Contract.notNull(infrastructure, "infrastructure cannot be null");
        Contract.notNull(infrastructure.getLockService(), "lockService cannot be null");
        Contract.notNullOrEmpty(uuid, "uuid cannot be empty");

        infrastructure.getLockService().returnLock(getLockId());
    }

    public String getLockId() {
        return uuid + "-entity-lock";
    }

    /////////////////////////////////////////////////////
    // save //
    //////////

    @SuppressWarnings("unchecked")
    public void save() {
        Contract.notNull(infrastructure, "infrastructure cannot be null");
        runTransaction(new TransactionCommand() {
           public String run(PersistenceService ps) {
               dateUpdated = System.currentTimeMillis();
               if (dateCreated == 0) {
                   dateCreated = dateUpdated;

                   // mark current ETL jobs as already run
                   etlJobsRun = new HashSet<String>();
                   Collection<? extends EtlJob> jobs = getEtlJobs();
                   if (jobs != null) {
                       for (EtlJob job : jobs) {
                           etlJobsRun.add(job.getClass().getCanonicalName());
                       }
                   }
               }
               
               return save(ps);
           }
        });
    }

    public String save(PersistenceService ps) {
        this.uuid = ps.put(this);
        return this.uuid;
    }

    /////////////////////////////////////////////////////
    // delete //
    ////////////

    public String delete() {
        return (String)runTransaction(new TransactionCommand() {
           public String run(PersistenceService ps) {
               return delete(ps);
           }
        });
    }

    public String delete(PersistenceService ps) {
        String deletedKey = null;
        if (this.uuid != null) {

            ps.delete(this.uuid);

            deletedKey = this.uuid;
            this.uuid = null;
        }

        return deletedKey;
    }

    public static String deleteByKey(final Infrastructure infra, String uuid) {
        BaseEntity entity = findByKey(infra, uuid);
        entity.delete();
        return uuid;
    }
    
    /////////////////////////////////////////////////////
    // load //
    //////////

    public static <T> T findByKey(final Infrastructure infra, final String uuid) {
        return (T)findByKey(infra, uuid, false);
    }

    public static <T> T findByKeyNoCache(final Infrastructure infra, final String uuid) {
        return (T)findByKey(infra, uuid, true);
    }

    @SuppressWarnings("unchecked")
    private static <T> T findByKey(final Infrastructure infra, final String uuid, final boolean noCache) {
        return (T)runTransaction(new TransactionCommand() {
           public Object run(PersistenceService ps) {
               Object o;
               if (noCache)
                   o = ps.getNoCache(uuid);
               else
                   o = ps.get(uuid);

               if (o != null)
                   postEntityLoad(o, infra);
               return o;
           }
        }, infra);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> findByKeys(final Infrastructure infra, final List<String> keys) {
        return (List<T>)runTransaction(new TransactionCommand() {
           public Object run(PersistenceService ps) {
               List<? extends PersistableEntity> list = ps.get(keys);
               for (Object o : list) {
                   postEntityLoad(o, infra);
               }
               return list;
           }
        }, infra);
    }

    public static <T> PaginatedResult<T> find(Infrastructure infra, Query query) {
        PaginatedResult<T> list = infra.getPersistenceService().queryPaginated(query);
        for (Object o : list.getCurrentPage()) {
            postEntityLoad(o, infra);
        }
        return list;
    }

    public static <T> PaginatedResult<T> findAllPaginated(Infrastructure infra, Class<T> type, PaginationState pstate, int rowsPerPage) {
        Contract.notNull(infra, "Infrastructure cannot be null");
        Contract.ensure(rowsPerPage > 0, "rowsPerPage must be > 0");

        Query query = Query.Builder(type)
                .sortDescending("dateCreated")
                .paginationState(pstate)
                .maxResults(rowsPerPage);

        return findPaginated(infra, query);
    }

    public static PaginatedResult<String> findAllKeysPaginated(Infrastructure infra, Class type, PaginationState pstate, int rowsPerPage) {
        Contract.notNull(infra, "Infrastructure cannot be null");
        Contract.ensure(rowsPerPage > 0, "rowsPerPage must be > 0");

        Query query = Query.Builder(type)
                .sortDescending("dateCreated")
                .paginationState(pstate)
                .maxResults(rowsPerPage);

        return infra.getPersistenceService().queryForIdsPaginated(query);
    }

    public static <T> PaginatedResult<T> findCreatedBetween(Infrastructure infra, Class<T> type, long startDate, long endDate, PaginationState pstate, int rowsPerPage) {
        Contract.notNull(infra, "Infrastructure cannot be null");
        Contract.ensure(rowsPerPage > 0, "rowsPerPage must be > 0");

        Query query = Query.Builder(type)
                .greaterThanOrEquals("dateCreated", startDate)
                .lessThan("dateCreated", endDate)
                .sortDescending("dateCreated")
                .paginationState(pstate)
                .maxResults(rowsPerPage);

        return findPaginated(infra, query);
    }

    public static long countCreatedBetween(Infrastructure infra, Class type, long startDate, long endDate) {
        Contract.notNull(infra, "Infrastructure cannot be null");

        Query query = Query.Builder(type)
                .greaterThanOrEquals("dateCreated", startDate)
                .lessThan("dateCreated", endDate);

        return infra.getPersistenceService().queryCount(query);
    }

    public static <T> PaginatedResult<T> findByDateUpdated(Infrastructure infra, Class<T> type,
                                                           String foreignKey, String foreignKeyProperty,
                                                           PaginationState paginationState,  int rowsPerPage) {
        return findByDateDescending(infra, type, foreignKey, foreignKeyProperty, paginationState, rowsPerPage, "dateUpdated");
    }

    public static <T> PaginatedResult<T> findByDateCreated(Infrastructure infra, Class<T> type,
                                                           String foreignKey, String foreignKeyProperty,
                                                           PaginationState paginationState,  int rowsPerPage) {
        return findByDateDescending(infra, type, foreignKey, foreignKeyProperty, paginationState, rowsPerPage, "dateCreated");
    }

    @SuppressWarnings("unchecked")
    private static <T> PaginatedResult<T> findByDateDescending(Infrastructure infra, Class<T> type,
                                                           String foreignKey, String foreignKeyProperty,
                                                           PaginationState paginationState,  int rowsPerPage, String dateProperty) {
        Contract.notNull(infra, "Infrastructure cannot be null");
        Contract.ensure(rowsPerPage > 0, "rowsPerPage must be > 0");

        Query query = Query.Builder(type)
                .sortDescending(dateProperty)
                .paginationState(paginationState)
                .maxResults(rowsPerPage);

        if (foreignKey != null)
            query.equals(foreignKeyProperty, foreignKey);

        return findPaginated(infra, query);
    }

    public static <T> PaginatedResult<T> findPaginated(Infrastructure infra, Query query) {
        // Get the n+1 set of rows from datastore based on our range query
        PaginatedResult<T> result = infra.getPersistenceService().queryPaginated(query);

        // if we have results, set the pivot point to the last element's dateUpdated
        if (result.getCurrentPage().size() > 0) {
            // wire up infra to all rows
            for (Object o : result.getCurrentPage()) {
                postEntityLoad(o, infra);
            }
        }

        return result;
    }

    public static long count(Infrastructure infra, Class type) {
        Contract.notNull(infra, "Infrastructure cannot be null");
        Contract.notNull(type, "type cannot be null");

        Query query = Query.Builder(type);
        return infra.getPersistenceService().queryCount(query);
    }
    private static void postEntityLoad(Object o, Infrastructure infra) {
        BaseEntity entity = (BaseEntity)o;
        entity.setInfrastructure(infra);
        entity.runEtlJobs();
    }

    private void runEtlJobs() {
        Collection<? extends EtlJob> jobs = getEtlJobs();
        if (jobs != null) {

            boolean saveEntity = false;
            if (etlJobsRun == null) {
                etlJobsRun = new HashSet<String>();
                saveEntity = true;
            }

            for (EtlJob job : jobs) {
                String className = job.getClass().getCanonicalName();
                if (!etlJobsRun.contains(className)) {
                    try {
                        log.info("Running ETL " + className + " on " + this.getClass().getName() + " with id: " + uuid);
                        job.runETL(this);
                        etlJobsRun.add(className);
                        saveEntity = true;
                        log.info("ETL job " + className + " successful for id: " + uuid);
                    }
                    catch (Exception e) {
                        log.error("Unable to run ETL Job: " + className, e);
                    }
                }
            }

            if (saveEntity) {
                log.debug("ETL done.  Saving entity with id: " + getId());
                save();
            }
        }
    }

    /////////////////////////////////////////////////////
    // private //
    /////////////

    @SuppressWarnings("unchecked")
    private <T> T runTransaction(TransactionCommand command) {
        return (T)runTransaction(command, infrastructure);
    }

    @SuppressWarnings("unchecked")
    private static <T> T runTransaction(TransactionCommand command, Infrastructure infrastructure) {
        Contract.notNull(infrastructure, "infrastructure cannot be null");
        PersistenceService ps = infrastructure.getPersistenceService();

//        Transaction trans;
//        boolean endTransaction = false;
//        try {
//            trans = ps.getCurrentTransaction();
//        }
//        catch (IllegalStateException e) {
//            trans = ps.beginTransaction();
//            endTransaction = true;
//        }

        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                Object t = command.run(ps);

//                if (endTransaction)
//                    trans.commit();

                return (T)t;
            }
//            catch (DatastoreTimeoutException e) {
//                retries++;
//                log.warn("Caught DatastoreTimeoutException. retries=" + retries + " max=" + MAX_RETRIES);
//                log.warn(e);
//            }
            catch (Exception e) {
//                if (endTransaction)
//                    trans.rollback();

                throw new RuntimeException(e);
            }
        }

        throw new RuntimeException("Max DatastoreTimeoutException retries exceeded!");
    }

    interface TransactionCommand {
        public <T> T run(PersistenceService ps);
    }

    ///////////////////////////////////////////////////////////

    public int compareTo(Object o) {
        BaseEntity other = (BaseEntity)o;
        return new Long(dateUpdated).compareTo(other.getDateUpdated());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity)) return false;

        BaseEntity that = (BaseEntity) o;

        if (dateCreated != that.dateCreated) return false;
        if (dateUpdated != that.dateUpdated) return false;
        if (etlJobsRun != null ? !etlJobsRun.equals(that.etlJobsRun) : that.etlJobsRun != null) return false;
        if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = infrastructure != null ? infrastructure.hashCode() : 0;
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (int) (dateCreated ^ (dateCreated >>> 32));
        result = 31 * result + (int) (dateUpdated ^ (dateUpdated >>> 32));
        result = 31 * result + (etlJobsRun != null ? etlJobsRun.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BaseEntity{" +
                "dateCreated=" + dateCreated +
                ", infrastructure=" + infrastructure +
                ", uuid=" + uuid +
                ", dateUpdated=" + dateUpdated +
                '}';
    }
}

package com.bitmechanic.jocko;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 27, 2010
 */
public interface PersistenceService {

    public    void                              setInfrastructure(Infrastructure infra);

    public    <T extends PersistableEntity> T   get(String uuid);
    public    <T extends PersistableEntity> T   getNoCache(String uuid);
    public    <T> List<T>                       get(Collection<String> uuids);
    public    String                            put(PersistableEntity entity);
    public    Set<String>                       put(List<? extends PersistableEntity> entities);
    public    boolean                        delete(String uuid);
    public    Set<String>                    delete(Collection<String> uuids);

    public    int                                           queryCount(Query<? extends PersistableEntity> query);
    public    <T> List<String>                             queryForIds(Query<T> query);
    public    <T> List<T>                                        query(Query<T> query);

    public    <T> PaginatedResult<String>                  queryForIdsPaginated(Query<T> query);
    public    <T> PaginatedResult<T>                             queryPaginated(Query<T> query);
    
}

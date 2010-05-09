package com.bitmechanic.jocko;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 16, 2010
 */
public interface EtlJob {

    public void runETL(PersistableEntity entity);

}

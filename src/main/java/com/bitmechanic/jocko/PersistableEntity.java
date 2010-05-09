package com.bitmechanic.jocko;

import java.io.Externalizable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 27, 2010
 */
public interface PersistableEntity extends Serializable {

    public void setInfrastructure(Infrastructure infra);

    public String getId();
    public void   setId(String uuid);

    public void fillMetaData(Map<String, Object> map);
    
    public void onBeforePut();
    public void onAfterGet();

    public Collection<? extends EtlJob> getEtlJobs();

}

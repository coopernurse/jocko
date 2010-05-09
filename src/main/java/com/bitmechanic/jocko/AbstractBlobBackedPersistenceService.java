package com.bitmechanic.jocko;

import com.bitmechanic.util.Contract;
import com.bitmechanic.util.DesCipher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Feb 26, 2010
 */
public abstract class AbstractBlobBackedPersistenceService implements PersistenceService {

    private static Log log = LogFactory.getLog(AbstractBlobBackedPersistenceService.class);

    protected Infrastructure infrastructure;
    protected String cipherKey;

    public void setInfrastructure(Infrastructure infra) {
        this.infrastructure = infra;
    }

    public void setCipherKey(String cipherKey) {
        this.cipherKey = cipherKey;
    }

    public abstract void putMetadata(PersistableEntity entity, boolean isNewEntity);
    public abstract void deleteMetadata(Collection<String> uuids);

    public <T> List<T> get(Collection<String> uuids) {
        Contract.notNull(infrastructure, "infrastructure cannot be null");

        ArrayList<T> list = new ArrayList<T>();
        for (String uuid : uuids) {
            Object o = get(uuid);
            if (o != null) {
                list.add((T)o);
            }
        }
        return list;
    }

    public boolean delete(String uuid) {
        Contract.notNull(infrastructure, "infrastructure cannot be null");

        deleteMetadata(Collections.singleton(uuid));

        try {
            return infrastructure.getBlobService().delete(uuid);
        }
        catch (IOException e) {
            throw new RuntimeException(uuid);
        }
    }

    public Set<String> delete(Collection<String> uuids) {
        Contract.notNull(infrastructure, "infrastructure cannot be null");

        deleteMetadata(uuids);

        LinkedHashSet<String> deleted = new LinkedHashSet<String>();

        IOException ex = null;

        for (String uuid : uuids) {
            try {
                if (infrastructure.getBlobService().delete(uuid))
                    deleted.add(uuid);
            }
            catch (IOException e) {
                ex = e;
            }
        }

        if (ex != null) {
            throw new RuntimeException(ex);
        }

        return deleted;
    }
    
    public <T extends PersistableEntity> T get(String uuid) {
        return (T)get(uuid, false);
    }

    public <T extends PersistableEntity> T getNoCache(String uuid) {
        return (T)get(uuid, true);
    }

    private <T extends PersistableEntity> T get(String uuid, boolean noCache) {
        Contract.notNull(infrastructure, "infrastructure cannot be null");

        try {
            byte[] entityData = null;

            if (noCache)
                entityData = infrastructure.getBlobService().getNoCache(uuid);
            else
                entityData = infrastructure.getBlobService().get(uuid);

            if (entityData == null) {
                deleteMetadata(Collections.singleton(uuid));
                return null;
            }
            else {
                log.debug("get() uuid: " + uuid);

                if (cipherKey != null) {
                    DesCipher cipher = new DesCipher(cipherKey);
                    entityData = cipher.decryptBytes(entityData);
                }

                PersistableEntity pe = PersistUtil.jsonToObject(infrastructure, entityData);
                pe.setId(uuid);
                return (T)pe;
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String put(PersistableEntity entity) {
        Contract.notNull(infrastructure, "infrastructure cannot be null");

        String uuid = entity.getId();
        boolean isNewEntity = false;
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            entity.setId(uuid);
            isNewEntity = true;
        }

        putMetadata(entity, isNewEntity);

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

        return uuid;
    }

    public Set<String> put(List<? extends PersistableEntity> entities) {
        Contract.notNull(infrastructure, "infrastructure cannot be null");

        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for (PersistableEntity pe : entities) {
            String uuid = put(pe);
            if (!set.contains(uuid))
                set.add(uuid);
        }
        return set;
    }

}

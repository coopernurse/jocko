package com.bitmechanic.jocko.appengine;

import com.bitmechanic.jocko.PersistableEntity;
import com.bitmechanic.util.Contract;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 28, 2010
 */
public class EntityMetadata {

    public static final String ENTITY_KIND = "PersistenceMetadata";
    public static final String KEY_ENTITY_CLASS = "_entity_class";

    public static Key createKeyForUUID(String uuid) {
        return KeyFactory.createKey(ENTITY_KIND, uuid);
    }

    //////////////

    private String id;
    private Map<String, Object> attributes;

    public EntityMetadata(Entity entity) {
        attributes = entity.getProperties();
        id = entity.getKey().getName();
    }

    public EntityMetadata(PersistableEntity pe) {
        this.attributes = new HashMap<String, Object>();
        pe.fillMetaData(attributes);
        attributes.put(KEY_ENTITY_CLASS, pe.getClass().getCanonicalName());
        id = pe.getId();
    }

    public Entity toDatastoreEntity() {
        Contract.notNullOrEmpty(id, "id cannot be empty");
        
        Entity entity = new Entity(ENTITY_KIND, id);
        for (String attrib : attributes.keySet())
            entity.setProperty(attrib, attributes.get(attrib));
        return entity;
    }

    public String getClassName() {
        return (String)getProperty(KEY_ENTITY_CLASS);
    }

    public Object getProperty(String property) {
        return attributes.get(property);
    }

    public String getId() {
        return id;
    }

}

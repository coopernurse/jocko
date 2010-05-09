package com.bitmechanic.jocko;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Feb 1, 2010
 */
public class PersistUtil {

    public static PersistableEntity jsonToObject(Infrastructure infra, byte data[]) throws IOException, ClassNotFoundException {
        String tmp = new String(data, "UTF-8");
        int pos = tmp.indexOf(" ");
        if (pos > -1) {
            String className = tmp.substring(0, pos);
            String      json = tmp.substring(pos+1);
            Gson gson = new Gson();
            PersistableEntity pe = (PersistableEntity)gson.fromJson(json, Class.forName(className));
            pe.setInfrastructure(infra);
            pe.onAfterGet();
            return pe;
        }
        else {
            throw new IllegalArgumentException("Unable to read class name from: " + tmp);
        }
    }

    public static byte[] objectToJson(PersistableEntity object) throws IOException {
        object.onBeforePut();
        Gson gson = new Gson();
        String json = object.getClass().getCanonicalName() + " " + gson.toJson(object);
        return json.getBytes("UTF-8");
    }

    public static void copyProperties(Object src, Object dest) throws IllegalAccessException {
        copyProperties(src, dest, src.getClass());
    }

    public static void copyProperties(Object src, Object dest, Class clazz) throws IllegalAccessException {
        if (clazz != null) {
            Field fields[] = clazz.getDeclaredFields();
            for (Field field : fields) {
                int modifiers = field.getModifiers();
                if (!Modifier.isFinal(modifiers) && !Modifier.isTransient(modifiers)) {
                    field.setAccessible(true);
                    Object val = field.get(src);
                    field.set(dest, val);
                }
            }

            copyProperties(src, dest, clazz.getSuperclass());
        }
    }

}

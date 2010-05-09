package com.bitmechanic.jocko.appengine;

import com.bitmechanic.jocko.CacheService;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import java.io.IOException;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 30, 2010
 */
public class GAECacheService implements CacheService {

    @Override
    public boolean delete(String key) throws IOException {
        return getMemcache().delete(key);
    }

    @Override
    public byte[] get(String key) throws IOException {
        return (byte[])getMemcache().get(key);
    }

    @Override
    public void put(String key, byte[] data) throws IOException {
        getMemcache().put(key, data);
    }

    private MemcacheService getMemcache() {
        return MemcacheServiceFactory.getMemcacheService();
    }
    
}

package com.bitmechanic.jocko.local;

import com.bitmechanic.jocko.CacheService;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

import java.io.IOException;
import java.util.concurrent.Future;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 1, 2010
 */
public class MemcachedCacheService implements CacheService {

    private MemcachedClient client;

    // one week.. hardcoded for now
    private static final int expiration = 60 * 60 * 24 * 7;

    public MemcachedCacheService(String servers) throws IOException {
        this.client = new MemcachedClient(AddrUtil.getAddresses(servers));
    }

    public boolean delete(String key) throws IOException {
        Future<Boolean> future = client.delete(key);
        try {
            return future.get();
        }
        catch (Exception e) {
            throw new IOException("Error waiting for async call to complete", e);
        }
    }

    public byte[] get(String key) throws IOException {
        return (byte[])client.get(key);
    }

    public void put(String key, byte[] data) throws IOException {
        Future<Boolean> future = client.set(key, expiration, data);
        try {
            future.get();
        }
        catch (Exception e) {
            throw new IOException("Error waiting for async call to complete", e);
        }
    }
}

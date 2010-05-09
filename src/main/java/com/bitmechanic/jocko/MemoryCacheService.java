package com.bitmechanic.jocko;

import org.apache.commons.collections.map.LRUMap;

import java.io.IOException;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 30, 2010
 */
public class MemoryCacheService implements CacheService {

    private LRUMap map;

    private long maxBytes;
    private long totalBytesInCache;

    public MemoryCacheService(long maxBytes) {
        this.map = new LRUMap(10000);
        this.maxBytes = maxBytes;
        this.totalBytesInCache = 0;
    }

    public synchronized boolean delete(String key) throws IOException {
        byte[] arr = (byte[])map.get(key);
        if (arr == null) {
            return false;
        }
        else {
            map.remove(key);
            totalBytesInCache -= arr.length;
            return true;
        }
    }

    public synchronized byte[] get(String key) throws IOException {
        return (byte[])map.get(key);
    }

    public synchronized void put(String key, byte[] data) throws IOException {
        // eject elements to get within max
        long newTotalBytes = totalBytesInCache + data.length;
        while (newTotalBytes > maxBytes && map.size() > 0) {
            delete((String)map.firstKey());
            newTotalBytes = totalBytesInCache + data.length;
        }

        if (newTotalBytes <= maxBytes) {
            map.put(key, data);
            totalBytesInCache += data.length;
        }
        else {
            throw new IllegalStateException("Unable to add data to cache.  data length exceeds max size: " + maxBytes);
        }
    }
}

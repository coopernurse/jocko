package com.bitmechanic.jocko;

import java.io.IOException;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 30, 2010
 */
public class NoOpCacheService implements CacheService {

    @Override
    public boolean delete(String key) throws IOException {
        return false;
    }

    @Override
    public byte[] get(String key) throws IOException {
        return null;
    }

    @Override
    public void put(String key, byte[] data) throws IOException {
        
    }
    
}

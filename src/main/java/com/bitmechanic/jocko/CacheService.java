package com.bitmechanic.jocko;

import java.io.IOException;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Nov 8, 2009
 */
public interface CacheService {

    public byte[] get(String key) throws IOException;

    public boolean delete(String key) throws IOException;
    
    public void put(String key, byte[] data) throws IOException;
    
}

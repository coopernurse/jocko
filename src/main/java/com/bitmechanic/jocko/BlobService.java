package com.bitmechanic.jocko;

import java.io.IOException;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 29, 2010
 */
public interface BlobService {

    public byte[] get(String key) throws IOException;

    public byte[] getNoCache(String key) throws IOException;

    public boolean delete(String key) throws IOException;

    public int deleteByPrefix(String prefix, int maxToDelete) throws IOException;
    
    public void put(String key, String mimeType, boolean readableByPublic, byte[] data) throws IOException;

    public String getUrlForKey(String key);

}

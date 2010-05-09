package com.bitmechanic.jocko.local;

import com.bitmechanic.util.Contract;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

import java.io.IOException;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 27, 2010
 */
public class MemcachedLockService extends AbstractLockService {

    private MemcachedClient client;

    public MemcachedLockService(String servers) throws IOException {
        this(servers, 0);
    }

    public MemcachedLockService(String servers, long retryMillis) throws IOException {
        super(retryMillis);
        this.client = new MemcachedClient(AddrUtil.getAddresses(servers));
    }

    public boolean getLockNoRetry(String id, long lockTimeoutMillis) {
        Contract.notNullOrEmpty(id, "id cannot be empty");
        Contract.ensure(lockTimeoutMillis > 0, "lockTimeoutMillis must be > 0");

        int lockTimeoutSeconds = (int)(lockTimeoutMillis / 1000);
        if (lockTimeoutSeconds == 0 && lockTimeoutMillis > 0)
            lockTimeoutSeconds = 1;
        
        long value = client.incr(id, 1, 0, lockTimeoutSeconds);
        return (value == 0);
    }

    public void returnLock(String id) {
        client.delete(id);
    }
    
}

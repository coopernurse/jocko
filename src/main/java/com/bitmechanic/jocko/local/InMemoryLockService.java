package com.bitmechanic.jocko.local;

import com.bitmechanic.jocko.LockService;
import com.bitmechanic.util.Contract;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 27, 2010
 */
public class InMemoryLockService extends AbstractLockService {

    private Map<String, Long> locks;

    public InMemoryLockService() {
        this(0);
    }

    public InMemoryLockService(long retryMillis) {
        super(retryMillis);
        this.locks = new HashMap<String,Long>();
    }

    @Override
    public synchronized boolean getLockNoRetry(String id, long lockTimeoutMillis) {
        Contract.notNullOrEmpty(id, "id cannot be empty");
        Contract.ensure(lockTimeoutMillis > 0, "lockTimeoutMillis must be > 0");

        long now = System.currentTimeMillis();
        if (!locks.containsKey(id) || now > locks.get(id)) {
            locks.put(id, now + lockTimeoutMillis);
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public synchronized void returnLock(String id) {
        locks.remove(id);
    }

}

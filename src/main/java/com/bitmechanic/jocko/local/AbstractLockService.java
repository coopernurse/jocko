package com.bitmechanic.jocko.local;

import com.bitmechanic.jocko.LockService;
import com.bitmechanic.util.Contract;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 27, 2010
 */
public abstract class AbstractLockService implements LockService {

    private static Log log = LogFactory.getLog(AbstractLockService.class);

    private static final long DEFAULT_RETRY_MILLIS = 15;

    private long retryMillis;

    public AbstractLockService() {
        this(DEFAULT_RETRY_MILLIS);
    }

    public AbstractLockService(long retryMillis) {
        if (retryMillis <= 0)
            retryMillis = DEFAULT_RETRY_MILLIS;
        
        this.retryMillis = retryMillis;
    }

    public void getLock(String id, long maxTimeToWaitMillis, long lockTimeoutMillis) {
        Contract.notNullOrEmpty(id,               "id cannot be empty");
        Contract.ensure(maxTimeToWaitMillis >= 0, "maxTimeToWaitMillis must be >= 0");
        Contract.ensure(lockTimeoutMillis > 0,    "lockTimeoutMillis must be > 0");

        boolean first = true;

        long timeout = System.currentTimeMillis() + maxTimeToWaitMillis;
        do {
            
            if (getLockNoRetry(id, lockTimeoutMillis)) {
                return;
            }

            if (first) {
                log.warn("unable to get lock for: " + id);
                first = false;
            }

            try { Thread.sleep(retryMillis); }
            catch (InterruptedException e) { }
        }
        while (System.currentTimeMillis() < timeout);

        throw new IllegalStateException("Unable to acquire lock id: " + id + " maxTimeToWait exceeded: " + maxTimeToWaitMillis);
    }


}

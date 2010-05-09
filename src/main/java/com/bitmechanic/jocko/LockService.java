package com.bitmechanic.jocko;

/**
 * General locking interface.  Allows app code to acquire locks without having to worry about
 * the deployment environment.  Implementations can be simple (single server), or distributed.
 *
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 27, 2010
 */
public interface LockService {

    /**
     * Try to acquire a lock for the given id.  If the lock cannot be immediately acquired, it returns false
     * immediately.  This method does not block or retry.
     *
     * This method is useful for cases where several threads may be asked to perform the same job
     * concurrently, but only one should win.  The others can silently no-op if the lock is not acquired.
     *
     * @param id ID of lock to acquire
     * @param lockTimeoutMillis How long lock should be held for until it times out
     * @return True if the lock was acquired.  False if lock was not acquired.
     */
    public boolean getLockNoRetry(String id, long lockTimeoutMillis);

    /**
     * Tries to acquire lock, and retries until maxTimeToWaitMillis has elapsed.
     * If lock cannot be acquired before that time, an IllegalStateException is thrown.
     *
     * If no exception is thrown, then you got the lock.
     *
     * This method is useful when you really really need to perform an operation synchronously, and
     * it's bad if you're not able to.
     *
     * @param id ID of lock to acquire
     * @param maxTimeToWaitMillis How long we should retry acquiring the lock if it is not immediately available
     * @param lockTimeoutMillis How long lock should be held for until it times out
     */
    public void getLock(String id, long maxTimeToWaitMillis, long lockTimeoutMillis);

    /**
     * Returns the lock -- effectively deleting it so other threads can acquire it with the
     * getLock() methods
     *
     * This method does not perform any checks to ensure that this thread owns the lock.  You can call it more than
     * once without error, but the 2nd call could potentially unlock the ID out from under another thread.
     *
     * @param id ID of lock to return
     */
    public void returnLock(String id);

}

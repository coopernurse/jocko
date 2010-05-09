package com.bitmechanic.jocko.local;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 27, 2010
 */
public class MemcachedLockServiceTest extends TestCase {

    MemcachedLockService service;
    String id = "my-id";

    protected void _setUp() throws Exception {
        service = new MemcachedLockService("localhost:8888");
        service.returnLock(id);
    }

    // disabling test for now..
    public void testNoOp() { }

    public void _testGetLock() {
        assertTrue(service.getLockNoRetry(id, 2000));
        assertFalse(service.getLockNoRetry(id, 1000));
        service.returnLock(id);
        assertTrue(service.getLockNoRetry(id, 100));
    }

    public void _testConcurrency() throws Exception {
        assertTrue(service.getLockNoRetry(id, 1000));

        final int failures[] = new int[1];
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 20; i++) {
            Thread t = new Thread(new Runnable() {
                
                public void run() {
                    try {
                        Thread.sleep(50);
                        if (service.getLockNoRetry(id, 1000))
                            failures[0]++;
                    }
                    catch (Throwable t) {
                        failures[0]++;
                    }
                }
            });
            t.start();
            threads.add(t);
        }

        for (Thread t : threads) {
            t.join();
        }

        assertEquals(0, failures[0]);
    }

}

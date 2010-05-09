package com.bitmechanic.jocko.local;

import com.bitmechanic.jocko.AsyncService;
import com.bitmechanic.jocko.AsyncTask;
import com.bitmechanic.jocko.Infrastructure;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Feb 6, 2010
 */
public class LocalAsyncService implements AsyncService {

    private Infrastructure infra;

    public LocalAsyncService(Infrastructure infra) {
        this.infra = infra;
    }

    @Override
    public void run(AsyncTask task) {
        AsyncRunner runner = new AsyncRunner(task);
        runner.start();
    }

    class AsyncRunner extends Thread {

        private AsyncTask task;

        AsyncRunner(AsyncTask task) {
            this.task = task;
        }

        @Override
        public void run() {
            task.run(infra);
        }
    }

}

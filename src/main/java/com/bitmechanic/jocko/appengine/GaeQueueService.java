package com.bitmechanic.jocko.appengine;

import com.bitmechanic.jocko.QueueMessage;
import com.bitmechanic.jocko.QueueService;
import com.google.appengine.api.labs.taskqueue.QueueFactory;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Dec 7, 2009
 */
public class GaeQueueService implements QueueService {

    public GAEQueue getDefaultQueue() {
        return new GAEQueue(QueueFactory.getDefaultQueue());
    }

    public GAEQueue getQueue(String name) {
        return new GAEQueue(QueueFactory.getQueue(name));
    }

    class GAEQueue implements com.bitmechanic.jocko.Queue {

        com.google.appengine.api.labs.taskqueue.Queue gaeQueue;

        GAEQueue(com.google.appengine.api.labs.taskqueue.Queue gaeQueue) {
            this.gaeQueue = gaeQueue;
        }

        @Override
        public void delete(String messageId) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public QueueMessage receive() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public String send(String message) {
            throw new RuntimeException("Not supported");
        }
        
    }
    
}

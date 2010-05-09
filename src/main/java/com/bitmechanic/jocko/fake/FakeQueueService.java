package com.bitmechanic.jocko.fake;

import com.bitmechanic.jocko.Queue;
import com.bitmechanic.jocko.QueueMessage;
import com.bitmechanic.jocko.QueueService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Dec 20, 2009
 */
public class FakeQueueService implements QueueService {

    private static final String DEFAULT_QUEUE_NAME = "__FakeQueueDefault";

    private Map<String,FakeQueue> queuesByName;

    public FakeQueueService() {
        queuesByName = new HashMap<String,FakeQueue>();
    }

    @Override
    public synchronized FakeQueue getQueue(String name) {
        FakeQueue queue = queuesByName.get(name);
        if (queue == null) {
            queue = new FakeQueue(name);
            queuesByName.put(name, queue);
        }
        return queue;
    }

    public Collection<String> getTasksForQueue(String queueName) {
        return getQueue(queueName).getAll();
    }

    public Collection<String> getTasksForDefaultQueue() {
        return getTasksForQueue(DEFAULT_QUEUE_NAME);
    }

    /////////////////////

    class FakeQueue implements Queue {

        String queueName;

        Map<String, String> inQueue;
        Map<String, String> inProcess;

        public FakeQueue(String queueName) {
            this.queueName = queueName;
            this.inQueue   = new LinkedHashMap<String, String>();
            this.inProcess = new LinkedHashMap<String, String>();
        }

        public Collection<String> getAll() {
            return inQueue.values();
        }

        @Override
        public synchronized void delete(String messageId) {
            inQueue.remove(messageId);
            inProcess.remove(messageId);
        }

        @Override
        public synchronized QueueMessage receive() {
            if (inQueue.size() > 0) {
                String key = inQueue.keySet().iterator().next();
                String msg = inQueue.get(key);
                inProcess.put(key, msg);
                inQueue.remove(key);
                return new QueueMessage(key, msg);
            }
            else {
                return null;
            }
        }

        @Override
        public synchronized String send(String message) {
            String id = UUID.randomUUID().toString();
            inQueue.put(id, message);
            return id;
        }
    }

}

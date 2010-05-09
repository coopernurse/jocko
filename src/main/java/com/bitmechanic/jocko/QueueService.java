package com.bitmechanic.jocko;

/**
 * Some other line here
 *
 * Generic interface wrapping access to queueing systems like Amazon SQS
 *
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Nov 8, 2009
 */
public interface QueueService {

    public Queue getQueue(String name);

}

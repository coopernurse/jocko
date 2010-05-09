package com.bitmechanic.jocko;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Feb 6, 2010
 */
public interface Queue {

    public QueueMessage receive();
    public String send(String message);
    public void delete(String messageId);

}

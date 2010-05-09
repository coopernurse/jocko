package com.bitmechanic.jocko;

import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Nov 8, 2009
 */
public interface MailService {

    public void send(Message msg) throws MessagingException;

}

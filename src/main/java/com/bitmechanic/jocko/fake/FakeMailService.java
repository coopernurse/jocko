package com.bitmechanic.jocko.fake;

import com.bitmechanic.jocko.MailService;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 5, 2010
 */
public class FakeMailService implements MailService {

    List<Message> messages = new ArrayList<Message>();

    public List<Message> getMessages() {
        return messages;
    }

    public void send(Message msg) throws MessagingException {
        messages.add(msg);
    }

}

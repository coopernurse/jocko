package com.bitmechanic.jocko.appengine;

import com.bitmechanic.jocko.MailService;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Nov 26, 2009
 */
public class GaeMailService implements MailService {

    public void send(Message msg) throws MessagingException {
        Transport.send(msg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return true;
    }
    
}

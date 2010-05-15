package com.bitmechanic.util;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: May 15, 2010
 */
public class MailUtil {

    public static Message toMessage(String fromEmail, String fromName, String subject, String body, String contentType, String... toEmails) throws MessagingException {
        Contract.notNullOrEmpty(fromEmail, "fromEmail cannot be empty");
        Contract.notNullOrEmpty(fromName, "fromName cannot be empty");
        Contract.notNullOrEmpty(subject, "subject cannot be empty");
        Contract.notNullOrEmpty(body, "body cannot be empty");
        Contract.notNullOrEmpty(contentType, "contentType cannot be empty");
        Contract.notNull(toEmails, "toEmails cannot be null");
        Contract.ensure(toEmails.length > 0, "toEmails.length must be > 0");

        Session session = Session.getDefaultInstance(new Properties(), null);
        Message msg = new MimeMessage(session);
        for (String toEmail : toEmails)
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

        try {
            msg.setFrom(new InternetAddress(fromEmail, fromName));
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to add addr: " + fromEmail + " with name: " + fromName, e);
        }

        msg.setSubject(subject);
        msg.setContent(body, contentType);
        msg.setHeader("Content-Type", contentType);
        return msg;
    }

}

package com.bitmechanic.jocko.local;

import com.bitmechanic.jocko.MailService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import java.util.Properties;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 1, 2010
 */
public class SmtpRelayMailService implements MailService {

    private static Log log = LogFactory.getLog(SmtpRelayMailService.class);

    private String smtpHost;
    private int smtpPort;

    public SmtpRelayMailService(String smtpHost, int smtpPort) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
    }

    public void send(Message msg) throws MessagingException {

        Transport transport = null;

        try {
            Properties props = System.getProperties();
            Session session = Session.getInstance(props);

            // get transport
            transport = session.getTransport("smtp");
            log.debug("send() opening SMTP connection to: " + smtpHost + ":" + smtpPort);

            // open connection
            transport.connect(smtpHost, smtpPort, null, null);

            // send the thing off
            transport.sendMessage(msg, msg.getAllRecipients());

        } finally {
            // close connection
            if (transport != null && transport.isConnected()) {
                transport.close();
            }
        }
    }
}

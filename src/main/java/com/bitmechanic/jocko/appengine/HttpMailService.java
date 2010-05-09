package com.bitmechanic.jocko.appengine;

import com.bitmechanic.jocko.MailService;
import com.bitmechanic.util.IOUtil;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * Hack-ish mail impl used to relay mail via HTTP URL that relays mail for us
 * Useful for testing from dev PCs that don't have a smtp relay running
 *
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Nov 27, 2009
 */
public class HttpMailService implements MailService {

    private URL url;

    public HttpMailService(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public void send(Message msg) throws MessagingException {
        try {
            HashMap<String,String> postData = new HashMap<String,String>();
            postData.put("from", combineAddresses(msg.getFrom()));
            postData.put("to", combineAddresses(msg.getRecipients(Message.RecipientType.TO)));
            postData.put("subject", msg.getSubject());
            postData.put("body", msg.getContent().toString());
            postData.put("content-type", msg.getContentType());

            IOUtil.postToConnection(url, postData);
        }
        catch (Exception e) {
            throw new MessagingException("Unable to send mail", e);
        }
    }

    private String combineAddresses(Address[] emails) {
        StringBuffer sb = new StringBuffer();
        for (Address addr : emails) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(addr.toString());
        }
        return sb.toString();
    }
}

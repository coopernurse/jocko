package com.bitmechanic.jocko.amazon;

import com.bitmechanic.jocko.Queue;
import com.bitmechanic.jocko.QueueMessage;
import com.bitmechanic.util.Contract;
import net.aw2.commons.amazon.SimpleSQS;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Feb 7, 2010
 */
public class SQSQueueService implements com.bitmechanic.jocko.QueueService {

    private static Log log = LogFactory.getLog(SQSQueueService.class);

    private SimpleSQS sqs;
    private Map<String,String> queueNameToUrl;
    private int visibilityTimeoutSeconds;

    public SQSQueueService(String awsAccessId, String awsSecretKey, int visibilityTimeoutSeconds) {
        Contract.notNullOrEmpty(awsAccessId, "awsAccessId cannot be empty");
        Contract.notNullOrEmpty(awsSecretKey, "awsSecretKey cannot be empty");
        sqs = new SimpleSQS(awsAccessId, awsSecretKey);
        queueNameToUrl = new HashMap<String,String>();
        this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
    }

    public Queue getQueue(String queueName) {
        Contract.notNullOrEmpty(queueName, "queueName cannot be empty");
        return new SQSQueue(sqs, getQueueUrlFromName(queueName));
    }

    private synchronized String getQueueUrlFromName(String queueName) {
        Contract.notNullOrEmpty(queueName, "queueName cannot be empty");

        String url = this.queueNameToUrl.get(queueName);

        if (url == null) {

            String queueEndsWith = "/" + queueName;

            try {
                List<String> queues = sqs.listQueues();
                for (String queueUrl : queues) {
                    if (queueUrl.endsWith(queueEndsWith)) {
                        url = queueUrl;
                        break;
                    }
                }

                if (url == null) {
                    // not found, create it
                    url = sqs.createQueue(queueName);
                }

                queueNameToUrl.put(queueName, url);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return url;
    }

    class SQSQueue implements Queue {

        private SimpleSQS sqs;
        private String queueUrl;

        SQSQueue(SimpleSQS sqs, String queueUrl) {
            this.sqs = sqs;
            this.queueUrl = queueUrl;
        }

        public void delete(String messageId) {
            Contract.notNullOrEmpty(messageId, "messageId cannot be empty");
            try {
                sqs.deleteMessage(queueUrl, messageId);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public QueueMessage receive() {
            try {
                List<Map> msgs = sqs.receiveMessage(queueUrl, 1, visibilityTimeoutSeconds);
                if (msgs != null && msgs.size() > 0) {
                    Map msg = msgs.get(0);
                    Contract.notNull(msg.get("ReceiptHandle"), "ReceiptHandle cannot be null");
                    Contract.notNull(msg.get("Body"), "Body cannot be null");
                    return new QueueMessage(msg.get("ReceiptHandle").toString(), msg.get("Body").toString());
                }
                else {
                    return null;
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public String send(String message) {
            Contract.notNull(message, "message cannot be null");

            try {
                return sqs.sendMessage(queueUrl, message);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}

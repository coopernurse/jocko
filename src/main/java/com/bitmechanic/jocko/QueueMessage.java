package com.bitmechanic.jocko;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Feb 6, 2010
 */
public class QueueMessage {

    private String messageId;
    private String messageBody;

    public QueueMessage(String messageId, String messageBody) {
        this.messageId = messageId;
        this.messageBody = messageBody;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getMessageBody() {
        return messageBody;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueueMessage)) return false;

        QueueMessage that = (QueueMessage) o;

        if (messageBody != null ? !messageBody.equals(that.messageBody) : that.messageBody != null) return false;
        if (messageId != null ? !messageId.equals(that.messageId) : that.messageId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = messageId != null ? messageId.hashCode() : 0;
        result = 31 * result + (messageBody != null ? messageBody.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "QueueMessage{" +
                "messageBody='" + messageBody + '\'' +
                ", messageId='" + messageId + '\'' +
                '}';
    }
}

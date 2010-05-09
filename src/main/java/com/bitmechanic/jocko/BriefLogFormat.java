package com.bitmechanic.jocko;

import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Nov 19, 2009
 */
public class BriefLogFormat extends Formatter {

    public BriefLogFormat() {
        super();
    }

    @Override
    public String formatMessage(LogRecord record) {
        return format(record);
    }

    @Override
    public String format(LogRecord record) {
        MessageFormat messageFormat = new MessageFormat("{0,date,YYYY-MM-dd hh:mm:ss}|{1}[{2}|{3}|{4}\n");
        Object[] arguments = new Object[5];
        arguments[0] = new Date(record.getMillis());
        arguments[1] = record.getLevel();
        arguments[2] = record.getLoggerName();
        arguments[3] = Thread.currentThread().getName();
        arguments[4] = record.getMessage();
        return messageFormat.format(arguments);
    }
}

package com.bitmechanic.util;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 13, 2010
 */
public class TimeBasedTriggeringEventEvaluator implements TriggeringEventEvaluator {

    private long intervalTimeMillis = 3600000;

    private long lastTriggerTimeMillis = 0;

    public long getIntervalTimeMillis() {
        return intervalTimeMillis;
    }

    public void setIntervalTimeMillis(long intervalTimeMillis) {
        this.intervalTimeMillis = intervalTimeMillis;
    }

    public boolean isTriggeringEvent(LoggingEvent event) {
        long now = System.currentTimeMillis();
        boolean result = (lastTriggerTimeMillis + intervalTimeMillis) < now;
        if (result) {
            lastTriggerTimeMillis = now;
        }
        return result;
    }

}

package com.bitmechanic.jocko;

import com.bitmechanic.util.Contract;
import com.bitmechanic.util.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 11, 2010
 */
public class PaginationStateManager implements Serializable {

    private static Log log = LogFactory.getLog(PaginationStateManager.class);

    private static final long serialVersionUID = 1L;
    private static final long ONE_DAY_MILLIS = 60000L * 60 * 24;

    private HashMap<String, String> paginationStates;
    private HashMap<String, Long> timeToLive;

    public PaginationStateManager() {
        paginationStates = new HashMap<String,String>();
        timeToLive = new HashMap<String, Long>();
    }

    public synchronized PaginationState getState(String token) {
        String paginationStateJson = null;
        int pageNum = 0;

        if (StringUtil.hasText(token)) {
            String parts[] = token.split("\\|");
            Contract.ensure(parts.length == 2, "Invalid token: " + token);

            String uuid = parts[0];
            pageNum = Integer.parseInt(parts[1]);

            paginationStateJson = paginationStates.get(uuid);
        }

        PaginationState state;
        if (paginationStateJson == null || pageNum == 0)
            state = new PaginationState();
        else
            state = PaginationState.fromJson(paginationStateJson, pageNum);

        updateTTL(state.getUuid());
        return state;
    }

    public synchronized void storeState(PaginationState state) {
        String uuid = state.getUuid();
        updateTTL(uuid);
        paginationStates.put(uuid, state.toJson());
        flushExpired();
    }

    public String getNextPageToken(PaginationState state) {
        return getPageToken(state, state.getCurrentPage() + 1);
    }

    public String getPreviousPageToken(PaginationState state) {
        return getPageToken(state, state.getCurrentPage() - 1);
    }

    private String getPageToken(PaginationState state, int pageNumber) {
        Contract.notNull(state, "state cannot be null");
        return state.getUuid() + "|" + pageNumber;
    }

    private void updateTTL(String uuid) {
        long ttl = System.currentTimeMillis() + ONE_DAY_MILLIS;
        timeToLive.put(uuid, ttl);
    }

    private synchronized void flushExpired() {
        long now = System.currentTimeMillis();
        Set<String> expired = new HashSet<String>();
        for (String uuid : timeToLive.keySet()) {
            if (timeToLive.get(uuid) < now)
                expired.add(uuid);
        }

        if (expired.size() > 0) {
            log.debug("flushExpired() removing items: " + expired.size());

            for (String uuid : expired) {
                timeToLive.remove(uuid);
                paginationStates.remove(uuid);
            }
        }
    }

    @Override
    public String toString() {
        return "PaginationStateManager{" +
                "paginationStates=" + paginationStates +
                ", timeToLive=" + timeToLive +
                '}';
    }
}

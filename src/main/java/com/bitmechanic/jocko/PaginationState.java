package com.bitmechanic.jocko;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 29, 2010
 */
public class PaginationState {

    public static PaginationState fromJson(String json, int currentPage) {
        Gson gson = new Gson();
        PaginationState state = gson.fromJson(json, PaginationState.class);
        state.setCurrentPage(currentPage);
        return state;
    }

    ///////////////////

    private String uuid;
    private int currentPage;
    private List<String> paginationTokens;

    public PaginationState() {
        paginationTokens = new ArrayList<String>();
        currentPage = 0;
        uuid = UUID.randomUUID().toString();
    }

    public boolean onLastPage() {
        return !hasNext();
    }

    public void setCurrentPage(int page) {
        this.currentPage = page;
        checkPageBounds();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public String getUuid() {
        return uuid;
    }

    public int getPages() {
        return paginationTokens.size() + 1;
    }

    public boolean hasPrevious() {
        return currentPage > 0;
    }

    public boolean hasNext() {
        return paginationTokens.size() > currentPage;
    }

    public void addToken(String token) {
        if (onLastPage())
            paginationTokens.add(token);
    }

    public String getCurrentPageToken() {
        if (currentPage > 0 && paginationTokens.size() >= currentPage)
            return paginationTokens.get(currentPage-1);
        else
            return null;
    }

    public boolean hasCurrentPageToken() {
        String token = getCurrentPageToken();
        return token != null && token.trim().length() > 0;
    }

    ///////////

    public String getStateForPage(int pageNum) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pageNum && i < paginationTokens.size(); i++) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(paginationTokens.get(i));
        }

        return sb.toString();
    }

    public PaginationState getNextPage() {
        currentPage++;
        checkPageBounds();
        return this;
    }

    public PaginationState getPreviousPage() {
        currentPage--;
        checkPageBounds();
        return this;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    private void checkPageBounds() {
        if (currentPage < 0 || currentPage > paginationTokens.size())
            throw new IllegalStateException("currentPage=" + currentPage + " but paginationTokens.size()=" + paginationTokens.size());
    }

}

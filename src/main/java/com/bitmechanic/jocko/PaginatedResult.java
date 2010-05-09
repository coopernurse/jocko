package com.bitmechanic.jocko;

import java.util.List;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Dec 21, 2009
 */
public class PaginatedResult<T> {

    public enum Direction {
        Previous, Next
    };

    private List<T> currentPage;
    private PaginationState paginationState;

    public PaginatedResult(List<T> currentPage, PaginationState paginationState) {
        this.currentPage     = currentPage;
        this.paginationState = paginationState;
    }

    public List<T> getCurrentPage() {
        return currentPage;
    }

    public PaginationState getPaginationState() {
        return paginationState;
    }
}

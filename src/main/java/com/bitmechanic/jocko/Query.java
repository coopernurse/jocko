package com.bitmechanic.jocko;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 27, 2010
 */
public class Query<T> {

    public enum Direction { ascending, descending };

    public enum Condition { and, or };

    public static <T> Query<T> Builder(Class<T> type) {
        return new Query<T>(type);
    }

    public static <T> Query<T> Next(Query<T> query, String nextToken) {
        return null;
    }

    //////////////////////////////

    private Class<T> type;
    private int maxResults;
    private int offset;
    private Direction sortDir;
    private String sortProperty;
    private List<FilterOption> filters;
    private PaginationState paginationState;
    private Condition condition = Condition.and;

    private Query(Class<T> type) {
        this.maxResults = 1000;
        this.filters = new ArrayList<FilterOption>();
        this.type = type;
        this.paginationState = new PaginationState();
    }

    public Class<T> getType() {
        return this.type;
    }

    public List<FilterOption> getFilters() {
        return filters;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public int getOffset() {
        return offset;
    }

    public Direction getSortDir() {
        return sortDir;
    }

    public String getSortProperty() {
        return sortProperty;
    }

    public Condition getCondition() {
        return condition;
    }

    public PaginationState getPaginationState() {
        return paginationState;
    }

    public boolean hasSortProperty() {
        return sortProperty != null && sortProperty.trim().length() > 0 && sortDir != null;
    }

    public Query<T> equals(String property, Object value) {
        filters.add(new FilterOption(property, FilterOption.Operator.EQUALS, value));
        return this;
    }

    public Query<T> notEquals(String property, Object value) {
        filters.add(new FilterOption(property, FilterOption.Operator.NOT_EQUALS, value));
        return this;
    }

    public Query<T> contains(String property, Object value) {
        filters.add(new FilterOption(property, FilterOption.Operator.CONTAINS, value));
        return this;
    }

    public Query<T> notContains(String property, Object value) {
        filters.add(new FilterOption(property, FilterOption.Operator.NOT_CONTAINS, value));
        return this;
    }

    public Query<T> greaterThan(String property, Object value) {
        filters.add(new FilterOption(property, FilterOption.Operator.GREATER_THAN, value));
        return this;
    }

    public Query<T> lessThan(String property, Object value) {
        filters.add(new FilterOption(property, FilterOption.Operator.LESS_THAN, value));
        return this;
    }

    public Query<T> greaterThanOrEquals(String property, Object value) {
        filters.add(new FilterOption(property, FilterOption.Operator.GREATER_THAN_EQUALS, value));
        return this;
    }

    public Query<T> lessThanOrEquals(String property, Object value) {
        filters.add(new FilterOption(property, FilterOption.Operator.LESS_THAN_EQUALS, value));
        return this;
    }

    public Query<T> maxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    public Query<T> offset(int offset) {
        this.offset = offset;
        return this;
    }

    public Query<T> sortAscending(String property) {
        this.sortDir = Direction.ascending;
        this.sortProperty = property;
        return this;
    }

    public Query<T> sortDescending(String property) {
        this.sortDir = Direction.descending;
        this.sortProperty = property;
        return this;
    }

    public Query<T> paginationState(PaginationState state) {
        if (state != null)
            this.paginationState = state;
        
        return this;
    }

    public Query<T> and() {
        condition = Condition.and;        
        return this;
    }

    public Query<T> or() {
        condition = Condition.or;        
        return this;
    }

}

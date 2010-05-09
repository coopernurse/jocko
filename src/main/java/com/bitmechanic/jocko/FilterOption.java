package com.bitmechanic.jocko;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 27, 2010
 */
public class FilterOption {

    public enum Operator { EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS, GREATER_THAN, LESS_THAN, GREATER_THAN_EQUALS, LESS_THAN_EQUALS };

    private String property;
    private Operator operator;
    private Object value;

    public FilterOption(String property, Operator operator, Object value) {
        this.property = property;
        this.operator = operator;
        this.value = value;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getProperty() {
        return property;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return operator.name() + " " + property + " " + value;
    }
}

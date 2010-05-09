package com.bitmechanic.jocko;

import java.util.Map;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 3, 2010
 */
public class BillingServiceException extends Exception {

    Map<String, String> errors = null;

    public BillingServiceException(String s) {
        super(s);
    }

    public BillingServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public BillingServiceException(Throwable cause) {
        super(cause);
    }

    public BillingServiceException(String s, Map<String, String> errors) {
        super(s);
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    @Override
    public String getMessage() {
        if (null != errors && errors.size() > 0) {
            StringBuilder details = new StringBuilder(" Details:");
            for (Map.Entry<String, String> error : errors.entrySet()) {
                details.append(" - code:");
                details.append(error.getKey());
                details.append(":");
                details.append(error.getValue());
            }
            return super.getMessage() + details.toString();
        } else {
            return super.getMessage();
        }
    }

}

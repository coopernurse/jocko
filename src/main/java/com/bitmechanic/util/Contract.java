package com.bitmechanic.util;

/**
 * Very simple design by contract util class.
 * Used to specify pre-conditions for methods.
 *
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Dec 16, 2009
 */
public class Contract {

    public static void ensure(boolean test, String msg) {
        if (!test)
            throw new IllegalStateException(msg);
    }

    public static void notNull(Object obj, String msg) {
        if (obj == null)
            throw new IllegalStateException(msg);
    }

    public static void notNullOrEmpty(String str, String msg) {
        if (str == null || str.trim().length() == 0)
            throw new IllegalStateException(msg);
    }

}

package com.bitmechanic.jocko;

import com.bitmechanic.util.Contract;

import java.util.Date;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: May 9, 2010
 */
public abstract class NonTypedPersistenceService extends AbstractBlobBackedPersistenceService {

    public static final String KEY_ENTITY_CLASS = "_entity_class";

    private static final int INT_LENGTH = String.valueOf(Integer.MAX_VALUE).length();
    private static final String INT_NEG_FORMAT = "!I%0" + INT_LENGTH + "d";
    private static final String INT_POS_FORMAT = "!i%0" + INT_LENGTH + "d";

    private static final int SHORT_LENGTH = String.valueOf(Short.MAX_VALUE).length();
    private static final String SHORT_NEG_FORMAT = "!T%0" + SHORT_LENGTH + "d";
    private static final String SHORT_POS_FORMAT = "!t%0" + SHORT_LENGTH + "d";

    private static final int LONG_LENGTH = String.valueOf(Long.MAX_VALUE).length();
    private static final String LONG_NEG_FORMAT = "!L%0" + LONG_LENGTH + "d";
    private static final String LONG_POS_FORMAT = "!l%0" + LONG_LENGTH + "d";

    private static final int FLOAT_LENGTH = String.valueOf(Integer.MAX_VALUE).length() + 1;
    private static final String FLOAT_FORMAT = "!f%0" + FLOAT_LENGTH + "d";

    private static final int DOUBLE_LENGTH = String.valueOf(Long.MAX_VALUE).length() + 1;
    private static final String DOUBLE_FORMAT = "!d%0" + DOUBLE_LENGTH + "d";

    protected String escapeStr(Object obj) {
        if (obj == null)
            return null;
        else
            return obj.toString().replace("'", "''");
    }

    public String toSimpleDbString(Object obj) {
        Contract.notNull(obj, "obj cannot be null");

        if (obj instanceof String) {
            String str = (String)obj;
            return "!s" + escapeStr(str);
        } else if (obj instanceof Integer) {
            int num = (Integer) obj;
            if (num < 0)
                return String.format(INT_NEG_FORMAT, Integer.MAX_VALUE + num);
            else
                return String.format(INT_POS_FORMAT, num);
        } else if (obj instanceof Short) {
            int num = (Short) obj;
            if (num < 0)
                return String.format(SHORT_NEG_FORMAT, Short.MAX_VALUE + num);
            else
                return String.format(SHORT_POS_FORMAT, num);
        } else if (obj instanceof Long) {
            return longToStr((Long) obj);
        } else if (obj instanceof Float) {
            int num = Float.floatToRawIntBits((Float) obj);
            return String.format(FLOAT_FORMAT, num);
        } else if (obj instanceof Double) {
            long num = Double.doubleToRawLongBits((Double) obj);
            return String.format(DOUBLE_FORMAT, num);
        } else if (obj instanceof Boolean) {
            boolean b = (Boolean) obj;
            return b ? "!b" : "!B";
        } else if (obj instanceof Date) {
            return "!z" + longToStr(((Date) obj).getTime());
        } else {
            throw new IllegalArgumentException("Unable to encode object of class: " + obj.getClass().getCanonicalName());
        }
    }

    public Object fromSimpleDbString(String str) {
        if (str == null) {
            return null;
        } else if (str.startsWith("!s")) {
            return str.substring(2);
        } else if (str.startsWith("!I") || str.startsWith("!i")) {
            return strToInt(str);
        } else if (str.startsWith("!L") || str.startsWith("!l")) {
            return strToLong(str);
        } else if (str.startsWith("!f")) {
            return Float.intBitsToFloat(strToInt(str));
        } else if (str.startsWith("!d")) {
            return Double.longBitsToDouble(strToLong(str));
        } else if (str.startsWith("!z")) {
            return new Date(strToLong(str.substring(2)));
        } else if (str.equals("!B")) {
            return false;
        } else if (str.equals("!b")) {
            return true;
        } else if (str.startsWith("!t") || str.startsWith("!T")) {
            return strToShort(str);
        } else {
            throw new IllegalArgumentException("Unknown encoding for value: " + str);
        }
    }

    private String longToStr(long num) {
        if (num < 0)
            return String.format(LONG_NEG_FORMAT, Long.MAX_VALUE + num);
        else
            return String.format(LONG_POS_FORMAT, num);
    }

    private int strToInt(String str) {
        if (str.startsWith("!I"))
            return Integer.parseInt(str.substring(2)) - Integer.MAX_VALUE;
        else
            return Integer.parseInt(str.substring(2));
    }

    private long strToLong(String str) {
        if (str.startsWith("!L"))
            return Long.parseLong(str.substring(2)) - Long.MAX_VALUE;
        else
            return Long.parseLong(str.substring(2));
    }

    private short strToShort(String str) {
        if (str.startsWith("!T"))
            return (short) (Short.parseShort(str.substring(2)) - Short.MAX_VALUE);
        else
            return Short.parseShort(str.substring(2));
    }

}

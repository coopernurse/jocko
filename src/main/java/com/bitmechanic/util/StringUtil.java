package com.bitmechanic.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.UnsupportedEncodingException;
import java.util.Random;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 3, 2010
 */
public class StringUtil {

    private static final String alphaNumeric = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static Random rand = new Random();

    public static boolean hasText(String str) {
        return (str != null) && (str.trim().length() > 0);
    }

    public static String toMD5Hex(String str) {
        try {
            return DigestUtils.md5Hex(str.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String randomAlphaNumericString(int length) {
        int len = alphaNumeric.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(alphaNumeric.charAt(rand.nextInt(len)));
        }
        return sb.toString();
    }
    
}

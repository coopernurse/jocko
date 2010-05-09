package com.bitmechanic.jocko.amazon;

import com.bitmechanic.jocko.BasePersistenceServiceTest;
import com.bitmechanic.jocko.PersistenceService;
import com.bitmechanic.util.PropertyUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Feb 26, 2010
 */
public class SimpleDBPersistenceServiceTest extends BasePersistenceServiceTest {

    SimpleDBPersistenceService service;

    @Override
    protected PersistenceService getPersistenceService() {
        return service;
    }

    @Override
    public void setUp() throws Exception {
        Properties props = PropertyUtil.loadProperties("jocko-test.properties");
        service = new SimpleDBPersistenceService(props.getProperty("awsAccessId"), props.getProperty("awsSecretKey"), "unittest");
        service.deleteAndCreateDomain();

        super.setUp();
    }

    public void testStoringIntegerZeroPads() throws Exception {
        String negativeNum = service.toSimpleDbString(-100);
        String positiveNum = service.toSimpleDbString(20);
        int negativeInt    = (Integer)service.fromSimpleDbString(negativeNum);
        int positiveInt    = (Integer)service.fromSimpleDbString(positiveNum);
        assertEquals(-100, negativeInt);
        assertEquals(20, positiveInt);
        assertTrue(negativeNum.compareTo(positiveNum) < 0);
        assertTrue(positiveNum.compareTo(negativeNum) > 0);
    }

    public void testStoringLongs() throws Exception {
        long aLong = System.currentTimeMillis() * -1;
        long bLong = System.currentTimeMillis();
        String aLongStr = service.toSimpleDbString(aLong);
        String bLongStr = service.toSimpleDbString(bLong);
        assertTrue(aLongStr.compareTo(bLongStr) < 0);
        assertTrue(bLongStr.compareTo(aLongStr) > 0);

        assertEquals(aLong, service.fromSimpleDbString(aLongStr));
        assertEquals(bLong, service.fromSimpleDbString(bLongStr));
    }

    public void testStoringFloats() throws Exception {
        float arr[] = {
            (float)-10000000.0,
            (float)-109.3239,
            (float)-109.32389,
            (float)0.00,
            (float)0.01,
            (float)0.02,
            (float)0.0200001,
            (float)1.01,
            (float)1.02,
            (float)2.0001,
            (float)101.0,
            (float)10000000.1};

        for (int i = 0; i < arr.length; i++) {
            float a = arr[i];
            for (int x = i; x < arr.length; x++) {
                float b = arr[x];
                String aStr = service.toSimpleDbString(a);
                String bStr = service.toSimpleDbString(b);

                float aF = (Float)service.fromSimpleDbString(aStr);
                float bF = (Float)service.fromSimpleDbString(bStr);
                assertEquals(arr[i], aF);
                assertEquals(arr[x], bF);
                if (i != x) {
                    assertTrue(aF + " not < " + bF, aF < bF);
                    assertTrue(aStr + " not < " + bStr, aStr.compareTo(bStr) < 0);
                }
            }
        }
    }

    public void testStoringDoubles() throws Exception {
        double arr[] = {
            -10000000.0,
            -109.3239,
            -109.32389,
            0.00,
            0.01,
            0.02,
            0.0200001,
            1.01,
            1.02,
            2.0001,
            101.0,
            10000000.1};

        for (int i = 0; i < arr.length; i++) {
            double a = arr[i];
            for (int x = i; x < arr.length; x++) {
                double b = arr[x];
                String aStr = service.toSimpleDbString(a);
                String bStr = service.toSimpleDbString(b);

                double aF = (Double)service.fromSimpleDbString(aStr);
                double bF = (Double)service.fromSimpleDbString(bStr);
                assertEquals(arr[i], aF);
                assertEquals(arr[x], bF);
                if (i != x) {
                    assertTrue(aF + " not < " + bF, aF < bF);
                    assertTrue(aStr + " not < " + bStr, aStr.compareTo(bStr) < 0);
                }
            }
        }
    }

    public void testStoringString() throws Exception {
        String arr[] = {
            "A",
            "Ab",
            "a",
            "aA",
            "aa"
        };

        for (int i = 0; i < arr.length; i++) {
            String a = arr[i];
            for (int x = i; x < arr.length; x++) {
                String b = arr[x];

                String aStr = service.toSimpleDbString(a);
                String bStr = service.toSimpleDbString(b);

                String aStrDecode = (String)service.fromSimpleDbString(aStr);
                String bStrDecode = (String)service.fromSimpleDbString(bStr);
                assertEquals(a, aStrDecode);
                assertEquals(b, bStrDecode);
                if (i != x) {
                    assertTrue(aStr + " not < " + bStr, aStr.compareTo(bStr) < 0);
                }
            }
        }
    }

    public void testStoreBoolean() throws Exception {
        String trueStr  = service.toSimpleDbString(true);
        String falseStr = service.toSimpleDbString(false);
        assertEquals(true, service.fromSimpleDbString(trueStr));
        assertEquals(false, service.fromSimpleDbString(falseStr));
    }

    public void testStoreDates() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date arr[] = {
                format.parse("1801-01-02 12:30:01"),
                format.parse("1801-01-02 12:30:02"),
                format.parse("1899-12-31 23:23:58"),
                format.parse("1899-12-31 23:23:59"),
                format.parse("1900-01-01 00:00:00"),
                format.parse("2010-01-02 12:02:00")
        };

        for (int i = 0; i < arr.length; i++) {
            Date a = arr[i];
            for (int x = i; x < arr.length; x++) {
                Date b = arr[x];

                String aStr = service.toSimpleDbString(a);
                String bStr = service.toSimpleDbString(b);

                Date aStrDecode = (Date)service.fromSimpleDbString(aStr);
                Date bStrDecode = (Date)service.fromSimpleDbString(bStr);
                assertEquals(a, aStrDecode);
                assertEquals(b, bStrDecode);
                if (i != x) {
                    assertTrue(aStr + " not < " + bStr, aStr.compareTo(bStr) < 0);
                }
            }
        }
    }

    public void testStoreShorts() throws Exception {
        short arr[] = {
                Short.MIN_VALUE,
                -300,
                0,
                1,
                5000,
                Short.MAX_VALUE
        };

        for (int i = 0; i < arr.length; i++) {
            short a = arr[i];
            for (int x = i; x < arr.length; x++) {
                short b = arr[x];

                String aStr = service.toSimpleDbString(a);
                String bStr = service.toSimpleDbString(b);

                short aStrDecode = (Short)service.fromSimpleDbString(aStr);
                short bStrDecode = (Short)service.fromSimpleDbString(bStr);
                assertEquals(a, aStrDecode);
                assertEquals(b, bStrDecode);
                if (i != x) {
                    assertTrue(aStr + " not < " + bStr, aStr.compareTo(bStr) < 0);
                }
            }
        }

    }

}

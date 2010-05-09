package com.bitmechanic.util;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Feb 10, 2010
 */
public class ExecUtilTest extends TestCase {

    private String convertBinary;
    private String identifyBinary;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        Properties props = PropertyUtil.loadProperties("jocko-test.properties");
        convertBinary = props.getProperty("convertBinary");
        identifyBinary = props.getProperty("identifyBinary");
    }

    public void testThrowsExceptionIfCommandInvalid() {
        try {
            ExecUtil.execFailFast("");
            fail("Should have thrown an IOException - command invalid");
        } catch (IOException e) {
            // expected behavior
        }
    }

    public void testCanGetStdoutFromCommand() throws Exception {
        String cmd = identifyBinary + " test" + File.separatorChar + "images" +  File.separatorChar + "image1.jpg";
        ExecResult result = ExecUtil.execFailFast(cmd);
        assertNotNull(result);
        assertTrue(result.getStdOutAsString().indexOf("JPEG") > -1);
    }

    public void testCanGetStderrFromCommandIOException() throws Exception {
        // invalid 'convert' invocation.  should give us output on STDERR
        String cmd = convertBinary + " -size -23";
        try {
            ExecResult result = ExecUtil.execFailFast(cmd);
            fail("Should have thrown IOException");
        }
        catch (IOException e) {
            assertTrue(e.getMessage().indexOf("option requires an argument") > -1);
        }
    }
    
}

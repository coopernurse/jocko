package com.bitmechanic.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Nov 29, 2009
 */
public class PropertyUtil {

    private static Log log = LogFactory.getLog(PropertyUtil.class);

    public static Properties loadPropertiesFromFileOrResource(String resourceFilename, String resourceName) {
        if (resourceFilename != null) {
            log.info("loading properties from file: " + resourceFilename);
            FileInputStream fis = null;
            try {
                File file = new File(resourceFilename);
                fis = new FileInputStream(file);
                Properties props = new Properties();
                props.load(fis);
                return props;
            }
            catch (Throwable t) {
                log.warn("Unable to load properties from file: " + resourceFilename, t);

            }
            finally {
                if (fis != null)
                    try { fis.close(); } catch(IOException e) { }
            }
        }

        // fallthrough - try properties resourceName
        return loadProperties(resourceName);
    }

    public static Properties loadProperties(String resourceName) {
        ClassLoader loader = PropertyUtil.class.getClassLoader();

        if(loader != null) {
            log.info("loading properties from resource: " + resourceName);
            URL url = loader.getResource(resourceName);
            if(url == null) {
                url = loader.getResource("/"+resourceName);
            }

            if(url != null) {
                try {
                    InputStream in = url.openStream();
                    Properties props = new Properties();
                    props.load(in);
                    return props;
                }
                catch(IOException ioe) {
                    throw new RuntimeException("Unable to load properties for: " + resourceName, ioe);
                }
            }
            else {
                throw new IllegalStateException("Unable to find resource URL for: " + resourceName);
            }
        }
        else {
            throw new IllegalStateException("Unable to get classloader");
        }
    }

}

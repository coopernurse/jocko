package com.bitmechanic.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 7, 2010
 */
public class IOUtil {

    public static String getMD5ForFile(File file) throws IOException {
        Contract.notNull(file, "file cannot be null");
        if (!file.exists() || !file.canRead()) {
            throw new IllegalArgumentException("Cannot read file: " + file.getAbsolutePath());
        }

        FileInputStream fis = new FileInputStream(file);
        String md5Hex = DigestUtils.md5Hex(fis);
        fis.close();
        return md5Hex;
    }

    public static void renameFile(File origFile, File fileToRenameTo) throws IOException {
        copyFile(origFile, fileToRenameTo);
        if (fileToRenameTo.exists()) {
            if (fileToRenameTo.length() != origFile.length()) {
                throw new IOException("Original file length != target file.  orig.length(): " + origFile.length() +
                    " target.length(): " + fileToRenameTo.length());
            }
            else {
                if (!origFile.delete()) {
                    throw new IOException("Unable to delete original file: " + origFile.getAbsolutePath());
                }
            }
        }
        else {
            throw new IOException("Target file does not exist: " + fileToRenameTo.getAbsolutePath());
        }
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public static byte[] getBytesFromInput(ObjectInput input) throws IOException {
        return getBytesFromInput(input);
    }

    public static byte[] getBytesFromInput(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte arr[] = new byte[4096];
        int length;
        while ((length = is.read(arr)) != -1) {
            bos.write(arr, 0, length);
        }
        byte[] data = bos.toByteArray();
        bos.close();
        return data;
    }

    public static File fetchUrlToTempFile(String url, String filePrefix, String fileExtension) throws IOException {
        byte data[] = fetchUrlToByteArray(url);

        File tmpFile = File.createTempFile(filePrefix, fileExtension);
        putBytesToFile(tmpFile, data);
        return tmpFile;
    }

    public static byte[] fetchUrlToByteArray(String url) throws IOException {
        URL urlObj = new URL(url);

        HttpURLConnection httpConn = (HttpURLConnection) urlObj.openConnection();
        httpConn.setRequestMethod("GET");
        httpConn.setReadTimeout(30000);
        httpConn.setInstanceFollowRedirects(false);
        httpConn.connect();
        if (httpConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Got non-200 HTTP response: " + httpConn.getResponseCode() + " - " + httpConn.getResponseMessage() + " - " + url);
        }

        InputStream responseData = httpConn.getInputStream();
        byte data[] = getBytesFromInput(responseData);
        responseData.close();
        httpConn.disconnect();
        return data;
    }

    public static Map<String, String> postToConnection(URL endPoint, Map uriParams) throws Exception {
        HttpURLConnection urlc = (HttpURLConnection) endPoint.openConnection();
        urlc.setRequestMethod("POST");

        urlc.setDoOutput(true);
        urlc.setDoInput(true);
        urlc.setUseCaches(false);
        //urlc.setAllowUserInteraction(false);
        urlc.setRequestProperty("Host", endPoint.getHost());
        urlc.setRequestProperty("Content-type", "application/x-www-form-urlencoded");


        /* Send out the data */
        OutputStream out = urlc.getOutputStream();
        Writer writer = new OutputStreamWriter(out, "UTF-8");

        try {
            Iterator it = uriParams.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                String val = (String) uriParams.get(key);

                writer.write(key);
                writer.write("=");
                writer.write(URLEncoder.encode(val, "utf-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~"));

                if (it.hasNext())
                    writer.write("&");
            }
        } finally {
            writer.flush();
            writer.close();
        }

        /* Read in the data */
        Reader reader = new InputStreamReader(urlc.getInputStream());
        StringBuilder in = new StringBuilder(32000);

        char arr[] = new char[4096];
        int length;
        while ((length = reader.read(arr)) != -1) {
            in.append(arr, 0, length);
        }
        reader.close();
        urlc.disconnect();

        Map<String, String> results = new HashMap<String, String>();
        results.put("body", in.toString());
        results.put("code", String.valueOf(urlc.getResponseCode()));

        return results;
    }

    public static void putBytesToFile(File file, byte data[]) throws IOException {
        OutputStream os = new FileOutputStream(file);
        try {
            os.write(data, 0, data.length);
            os.flush();
        }
        finally {
            os.close();
        }
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        try {
            // Get the size of the file
            long length = file.length();

            // You cannot create an array using a long type.
            // It needs to be an int type.
            // Before converting to an int type, check
            // to ensure that file is not larger than Integer.MAX_VALUE.
            if (length > Integer.MAX_VALUE) {
                // File is too large
            }

            // Create the byte array to hold the data
            byte[] bytes = new byte[(int) length];

            // Read in the bytes
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            // Ensure all the bytes have been read in
            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }


            return bytes;
        }
        finally {
            is.close();
        }
    }

}

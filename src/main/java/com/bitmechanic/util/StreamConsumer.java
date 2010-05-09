package com.bitmechanic.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 14, 2010
 */
public class StreamConsumer extends Thread {

    private InputStream stream;
    private String output;

    public StreamConsumer(InputStream stream) {
        this.stream = stream;
    }

    public String getOutput() {
        return output;
    }

    public void run() {
        try {
            output = convertStreamToString(stream);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String convertStreamToString(InputStream inputStream) throws IOException {
        StringWriter sw = new StringWriter();
        InputStreamReader isr = new InputStreamReader(inputStream);

        char data[] = new char[1024];
        int len;
        while ((len = isr.read(data, 0, 1024)) > 0) {
            sw.write(data, 0, len);
        }

        return sw.toString();
    }

}

package com.bitmechanic.util;

import java.io.IOException;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Feb 9, 2010
 */
public class ExecResult {

    private Process process;

    private String stdError;
    private String stdOut;

    public ExecResult(Process process) {
        this.process = process;
        this.stdOut = null;
    }

    public Process getProcess() {
        return process;
    }

    public String getStdOutAsString() throws IOException {
        return stdOut;
    }

    public String getStdErrAsString() throws IOException {
        return stdError;
    }

    public void consumeStdOutStdErrAndWait() {
        StreamConsumer stdOutConsumer = new StreamConsumer(process.getInputStream());
        StreamConsumer stdErrConsumer = new StreamConsumer(process.getErrorStream());

        stdOutConsumer.start();
        stdErrConsumer.start();

        try {
            process.waitFor();
        }
        catch (InterruptedException e) {
            // try to read the output anyway.
        }

        try {
            stdOutConsumer.join();
        }
        catch (InterruptedException ignored) { }
        try {
            stdErrConsumer.join();
        }
        catch (InterruptedException ignored) { }

        stdOut   = stdOutConsumer.getOutput();
        stdError = stdErrConsumer.getOutput();
    }

}

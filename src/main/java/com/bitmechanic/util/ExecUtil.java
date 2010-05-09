package com.bitmechanic.util;

import java.io.IOException;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Feb 9, 2010
 */
public class ExecUtil {

    public static ExecResult fork(String cmd) throws IOException {
        return fork(cmd, " ");
    }

    public static ExecResult fork(String cmd, String delimiter) throws IOException {
        String cmdArr[] = cmd.split(delimiter);
        ProcessBuilder pb = new ProcessBuilder(cmdArr);
        Process process = pb.start();
        return new ExecResult(process);
    }

    public static ExecResult exec(String cmd) throws IOException {
        return exec(cmd, " ");
    }

    public static ExecResult exec(String cmd, String delimiter) throws IOException {
        String cmdArr[] = cmd.split(delimiter);
        ProcessBuilder pb = new ProcessBuilder(cmdArr);
        Process process = pb.start();

        ExecResult result = new ExecResult(process);
        result.consumeStdOutStdErrAndWait();
        return result;

    }

    public static ExecResult execFailFast(String cmd) throws IOException {
        return execFailFast(cmd, " ");
    }

    public static ExecResult execFailFast(String cmd, String delimiter) throws IOException {
        ExecResult result = exec(cmd, delimiter);

        if (result.getProcess().exitValue() == 0) {
            return result;
        }
        else {
            throw new IOException("STDOUT=" + result.getStdOutAsString() + "\nSTDERR=" + result.getStdErrAsString());
        }
    }

}

package org.kframework.ktest2;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;

/**
 * A unified process class for kompile, kompile --pdf and krun processes.
 */
public class Proc<T> implements Runnable {

    private final String[] args;
    private final String expectedOut;
    private final String expectedErr;
    private final String procInput;
    private final Comparator<String> strComparator;
    private Process proc = null;

    private final T obj;

    private final boolean verbose;

    /**
     * Return code of process. -1 if process is not run/finished yet.
     */
    private int returnCode = -1;

    /**
     *
     * @param obj this is basically an arbitrary object to keep in a process,
     *            this used to know which TestCase a process is running
     * @param args arguments to pass to process
     * @param expectedOut null to ignore the program output, output messages are ignored when
     *                    program fails with an error
     * @param expectedErr null if not testing for error output, error messages are ignored when
     *                    program returns 0
     * @param procInput null or empty string to not pass anything to program input
     * @param strComparator comparator object to compare program outputs with expected outputs
     */
    public Proc(T obj, String[] args, String procInput, String expectedOut, String expectedErr,
                Comparator<String> strComparator, boolean verbose) {
        this.obj = obj;
        this.args = args;
        this.expectedOut = expectedOut;
        this.expectedErr = expectedErr;
        this.procInput = procInput;
        this.strComparator = strComparator;
        this.verbose = verbose;
    }

    public Proc(T obj, String[] args, boolean verbose) {
        this(obj, args, "", "", "", new DefaultStringComparator(), verbose);
    }

    @Override
    public void run() {
        String procCmd = StringUtils.join(args, ' ');
        ProcessBuilder pb = new ProcessBuilder(args);

        try {
            long startTime = System.currentTimeMillis();
            proc = pb.start();

            // I'm using a different naming convention because this is more intuitive for me
            InputStream errorStream = proc.getErrorStream(); // program's error stream
            InputStream outStream = proc.getInputStream(); // program's output stream
            OutputStream inStream = proc.getOutputStream(); // program's input stream

            // pass input to process
            IOUtils.write(procInput, inStream);
            inStream.close();

            returnCode = proc.waitFor();
            long endTime = System.currentTimeMillis() - startTime;

            String pgmOut = IOUtils.toString(outStream);
            String pgmErr = IOUtils.toString(errorStream);

            if (returnCode == 0) {

                // program ended successfully ..
                if (expectedOut == null) {
                    // we're not comparing outputs
                    if (verbose)
                        System.out.format("DONE: %s (time %d ms)%n", procCmd, endTime);
                } else if (strComparator.compare(pgmOut, expectedOut) != 0)
                    // outputs don't match
                    System.out.format(
                            "ERROR: %s output doesn't match with expected output (time: %d ms)%n",
                            procCmd, endTime);
                else
                    // outputs match
                    if (verbose)
                        System.out.format("DONE: %s (time %d ms)%n", procCmd, endTime);

            } else {

                // program ended with error ..
                if (expectedErr == null)
                    // we're not comparing error outputs
                    System.out.format(
                            "ERROR: %s failed with error (time: %d ms)%n", procCmd, endTime);
                else if (strComparator.compare(pgmErr, expectedErr) == 0)
                    // error outputs match
                    if (verbose)
                        System.out.format("DONE: %s (time %d ms)%n", procCmd, endTime);
                else
                    // error outputs don't match
                    System.out.format(
                            "ERROR: %s throwed error, but expected error message doesn't match " +
                            "(time: %d ms)%n", procCmd, endTime);

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // TODO: error handlers (osa1)
        proc = null;
    }

    /**
     * Kill spawned process. If no process is spawned or spawned process is ended, nothing happens.
     */
    public void kill() {
        proc.destroy();
        String procCmd = StringUtils.join(args, ' ');
        System.out.format("ERROR: %s killed (timeout)%n", procCmd);
    }

    public int getReturnCode() {
        return returnCode;
    }

    public T getObj() {
        return obj;
    }
}

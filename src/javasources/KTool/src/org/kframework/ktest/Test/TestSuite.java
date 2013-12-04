package org.kframework.ktest.Test;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.kframework.krun.ColorSetting;
import org.kframework.ktest.*;
import org.kframework.ktest.CmdArgs.CmdArg;
import org.kframework.utils.ColorUtil;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestSuite {

    private final List<TestCase> tests;

    private ThreadPoolExecutor tpe;

    private final boolean verbose;

    private final ColorSetting colorSetting;

    /**
     * Set of ktest steps to skip.
     */
    private final Set<KTestStep> skips;

    /**
     * Timeout for a process.
     */
    private final int timeout;

    private final ReportGen reportGen;

    private final Comparator<String> strComparator;

    private int kompileTime; // total time spent on compiling
    private int pdfTime; // total time spent on pdf generation
    private int krunTime; // total time spent on running programs
    private int kompileSteps; // total number of kompile tasks, this number is not known until
                              // ktest finishes job, because while running krun tests, adinitional
                              // compilations may be neccessary
    private int pdfSteps; // total number of pdf generation tasks
    private int krunSteps; // total number of krun tasks

    private TestSuite(List<TestCase> tests, Set<KTestStep> skips, boolean verbose,
                     Comparator<String> strComparator, ColorSetting colorSetting,
                     int timeout, boolean report) {
        this.tests = tests;
        this.skips = skips;
        this.verbose = verbose;
        this.strComparator = strComparator;
        this.colorSetting = colorSetting;
        this.timeout = timeout;
        reportGen = report ? new ReportGen() : null;
    }

    private TestSuite(TestCase singleTest, Set<KTestStep> skips, boolean verbose,
                     Comparator<String> strComparator, ColorSetting colorSetting,
                     int timeout, boolean report) {
        tests = new LinkedList<>();
        tests.add(singleTest);
        this.skips = skips;
        this.verbose = verbose;
        this.strComparator = strComparator;
        this.colorSetting = colorSetting;
        this.timeout = timeout;
        reportGen = report ? new ReportGen() : null;
    }

    public TestSuite(List<TestCase> tests, CmdArg cmdArg) {
        this(tests, cmdArg.getSkips(), cmdArg.isVerbose(),
                cmdArg.getStringComparator(), cmdArg.getColorSetting(), cmdArg.getTimeout(),
                cmdArg.getGenerateReport());
    }

    public TestSuite(TestCase singleTest, CmdArg cmdArg) {
        this(singleTest, cmdArg.getSkips(), cmdArg.isVerbose(),
                cmdArg.getStringComparator(), cmdArg.getColorSetting(), cmdArg.getTimeout(),
                cmdArg.getGenerateReport());
    }

    /**
     * Run TestSuite and return true if all tests passed.
     * @return whether all tests passed or not
     * @throws InterruptedException when some process is interrupted for some reason
     */
    public boolean run() throws InterruptedException, TransformerException,
            ParserConfigurationException, IOException {
        boolean ret = true;
        List<TestCase> successfulTests = tests;

        if (!skips.contains(KTestStep.KOMPILE)) {
            successfulTests = runKompileSteps(filterSkips(tests, KTestStep.KOMPILE));
            ret &= successfulTests.size() == tests.size();
        }
        if (!skips.contains(KTestStep.PDF))
            ret &= runPDFSteps(filterSkips(successfulTests, KTestStep.PDF));
        if (!skips.contains(KTestStep.KRUN))
            ret &= runKRunSteps(filterSkips(successfulTests, KTestStep.KRUN));

        String colorCode = ColorUtil.RgbToAnsi(ret ? Color.green : Color.red, colorSetting);
        String msg = ret ? "SUCCESS" : "FAIL (see details above)";
        System.out.format("%n%s%s%s%n", colorCode, msg, ColorUtil.ANSI_NORMAL);

        System.out.format("----------------------------%n" +
                "Definitions kompiled: %s (%s'%s'')%n" +
                "PDF posters kompiled: %s (%s'%s'')%n" +
                "Programs krun: %s (%s'%s'')%n" +
                "Total time: %s'%s''%n" +
                "----------------------------%n",
                kompileSteps, kompileTime / 60, kompileTime % 60,
                pdfSteps, pdfTime / 60, pdfTime % 60,
                krunSteps, krunTime / 60, krunTime % 60,
                (kompileTime + pdfTime + krunTime) / 60, (kompileTime + pdfTime + krunTime) % 60);

        // save reports
        if (reportGen != null)
            reportGen.save();

        return ret;
    }

    /**
     * Print the commands that would be executed, but do not execute them.
     * (inspired by GNU Make)
     */
    public void dryRun() {
        if (!skips.contains(KTestStep.KOMPILE)) {
            List<TestCase> kompileSteps = filterSkips(tests, KTestStep.KOMPILE);
            for (TestCase tc : kompileSteps)
                System.out.println(StringUtils.join(tc.getKompileCmd(), " "));
        }
        if (!skips.contains(KTestStep.PDF)) {
            List<TestCase> pdfSteps = filterSkips(tests, KTestStep.PDF);
            for (TestCase tc : pdfSteps)
                System.out.println(StringUtils.join(tc.getPdfCmd(), " "));
        }
        if (!skips.contains(KTestStep.KRUN)) {
            List<TestCase> krunSteps = filterSkips(tests, KTestStep.KRUN);
            for (TestCase tc : krunSteps) {
                List<KRunProgram> programs = tc.getPrograms();
                for (KRunProgram program : programs) {
                    String[] krunCmd = program.getKrunCmd();
                    LinkedList<String> krunCmd1 = new LinkedList<>();
                    Collections.addAll(krunCmd1, krunCmd);
                    if (program.outputFile != null)
                        krunCmd1.add("> " + program.outputFile);
                    if (program.inputFile != null)
                        krunCmd1.add("< " + program.inputFile);
                    System.out.println(StringUtils.join(krunCmd1, " "));
                }
            }
        }
    }

    private List<TestCase> filterSkips(List<TestCase> tests, KTestStep step) {
        List<TestCase> ret = new LinkedList<>();
        for (TestCase test : tests)
            if (!test.skip(step))
                ret.add(test);
        return ret;
    }

    /**
     * Run kompile steps in list of test cases.
     *
     * This method returns something different from others, this is because in kompile tests we
     * need to know exactly what tests passed, because otherwise we can't know what krun and
     * pdf tests to run (running krun/pdf on a broken definition doesn't make sense)
     * @return list of test cases that run successfully
     * @throws InterruptedException
     */
    private List<TestCase> runKompileSteps(List<TestCase> tests) throws InterruptedException {
        int len = tests.size();
        List<TestCase> successfulTests = new ArrayList<>(len);
        List<Proc<TestCase>> ps = new ArrayList<>(len);

        System.out.format("Kompile the language definitions...(%d in total)%n", len);
        startTpe();
        for (TestCase tc : tests) {
            Proc<TestCase> p = new Proc<>(tc, tc.getKompileCmd(),
                    strComparator, timeout, verbose, colorSetting);
            ps.add(p);
            tpe.execute(p);
            kompileSteps++;
        }
        stopTpe();

        // collect successful test cases, report failures
        for (Proc<TestCase> p : ps) {
            TestCase tc = p.getObj();
            kompileTime += p.getTimeDeltaSec();
            if (p.isSuccess())
                successfulTests.add(tc);
            makeReport(p, makeRelative(tc.getDefinition()),
                    FilenameUtils.getName(tc.getDefinition()));
        }

        printResult(successfulTests.size() == len);

        return successfulTests;
    }

    /**
     * Run pdf tests.
     * @param tests list of tests to run pdf step
     * @return whether all run successfully or not
     * @throws InterruptedException
     */
    private boolean runPDFSteps(List<TestCase> tests) throws InterruptedException {
        List<Proc<TestCase>> ps = new ArrayList<>();
        int len = tests.size();
        System.out.format("Generate PDF files...(%d in total)%n", len);
        startTpe();
        for (TestCase tc : tests) {
            Proc<TestCase> p = new Proc<>(tc, tc.getPdfCmd(),
                    strComparator, timeout, verbose, colorSetting);
            ps.add(p);
            tpe.execute(p);
            pdfSteps++;
        }
        stopTpe();

        boolean ret = true;
        for (Proc<TestCase> p : ps) {
            TestCase tc = p.getObj();
            pdfTime += p.getTimeDeltaSec();
            if (!p.isSuccess())
                ret = false;
            makeReport(p, makeRelative(tc.getDefinition()),
                    FilenameUtils.getBaseName(tc.getDefinition()) + ".pdf");
        }

        printResult(ret);

        return ret;
    }

    /**
     * Run krun tests.
     * @param tests list of test cases to run krun steps
     * @return whether all run successfully or not
     * @throws InterruptedException
     */
    private boolean runKRunSteps(List<TestCase> tests) throws InterruptedException {
        List<TestCase> kompileSuccesses = new LinkedList<>();

        // collect definitions that are not yet kompiled and kompile them first
        ArrayList<TestCase> notKompiled = new ArrayList<>();
        for (TestCase tc : tests) {
            if (!tc.isDefinitionKompiled())
                notKompiled.add(tc);
            else
                kompileSuccesses.add(tc);
        }
        System.out.println("Kompiling definitions that are not yet kompiled.");
        kompileSuccesses.addAll(runKompileSteps(notKompiled));

        // at this point we have a subset of tests that are successfully kompiled,
        // so run programs of those tests
        int successes = 0;
        int totalTests = 0;
        for (TestCase tc : kompileSuccesses) {

            List<KRunProgram> programs = tc.getPrograms();
            int inputs = 0, outputs = 0, errors = 0;
            for (KRunProgram p : programs) {
                if (p.inputFile != null) inputs++;
                if (p.outputFile != null) outputs++;
                if (p.errorFile != null) errors++;
            }

            System.out.format("Running %s programs... (%d in total, %d with input, " +
                    "%d with output, %d with error)%n", tc.getDefinition(), programs.size(),
                    inputs, outputs, errors);

            // we can have more parallelism here, but just to keep things same as old ktest,
            // I'm testing tast cases sequentially
            List<Proc<KRunProgram>> testCaseProcs = new ArrayList<>(programs.size());
            startTpe();
            for (KRunProgram program : programs) {
                testCaseProcs.add(runKRun(program));
                totalTests++;
            }
            stopTpe();

            for (Proc<KRunProgram> p : testCaseProcs)
                if (p != null) // p may be null when krun test is skipped because of missing
                               // input file
                {
                    KRunProgram pgm = p.getObj();
                    krunTime += p.getTimeDeltaSec();
                    makeReport(p, makeRelative(tc.getDefinition()),
                            FilenameUtils.getName(pgm.pgmName));
                    if (p.isSuccess())
                        successes++;
                }
        }
        printResult(successes == totalTests);
        return successes == totalTests;
    }

    private void startTpe() {
        tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());
    }

    private void stopTpe() throws InterruptedException {
        tpe.shutdown();
        while (!tpe.awaitTermination(1, TimeUnit.SECONDS));
    }

    /**
     * Execute a krun step.
     * @param program KRunProgram object that holds required information to run a krun process
     * @return Proc object for krun process
     */
    private Proc<KRunProgram> runKRun(KRunProgram program) {
        String[] args = program.getKrunCmd();

        // passing null to Proc is OK, it means `ignore'
        String inputContents = null, outputContents = null, errorContents = null;
        if (program.inputFile != null)
            try {
                inputContents = IOUtils.toString(new FileInputStream(new File(program.inputFile)));
            } catch (IOException e) {
                System.out.format("WARNING: cannot read input file %s -- skipping program %s%n",
                        program.inputFile, program.args.get(1));
                // this case happens when an input file is found by TestCase,
                // but somehow file is not readable. in that case there's no point in running the
                // program because it'll wait for input forever.
                return null;
            }
        if (program.outputFile != null)
            try {
                outputContents = IOUtils.toString(new FileInputStream(
                        new File(program.outputFile)));
            } catch (IOException e) {
                System.out.format("WARNING: cannot read output file %s -- program output " +
                        "won't be matched against output file%n", program.outputFile);
            }
        if (program.errorFile != null)
            try {
                errorContents = IOUtils.toString(new FileInputStream(
                        new File(program.errorFile)));
            } catch (IOException e) {
                System.out.format("WARNING: cannot read error file %s -- program error output "
                        + "won't be matched against error file%n", program.errorFile);
            }

        // Annotate expected output and error messages with paths of files that these strings
        // are defined in (to be used in error messages)
        Annotated<String, String> outputContentsAnn = null;
        if (outputContents != null)
            outputContentsAnn = new Annotated<>(outputContents, program.outputFile);

        Annotated<String, String> errorContentsAnn = null;
        if (errorContents != null)
            errorContentsAnn = new Annotated<>(errorContents, program.errorFile);

        if (verbose)
            printVerboseRunningMsg(program);
        Proc<KRunProgram> p = new Proc<>(program, args, inputContents, outputContentsAnn,
                errorContentsAnn, strComparator, timeout, verbose, colorSetting);
        tpe.execute(p);
        krunSteps++;
        return p;
    }

    private void printVerboseRunningMsg(KRunProgram program) {
        StringBuilder b = new StringBuilder();
        b.append("Running [");
        b.append(StringUtils.join(program.args, " "));
        b.append("]");
        if (program.inputFile != null) {
            b.append(" [input: ");
            b.append(program.inputFile);
            b.append("]");
        }
        if (program.outputFile != null) {
            b.append(" [output: ");
            b.append(program.outputFile);
            b.append("]");
        }
        System.out.println(b);
    }

    private void printResult(boolean condition) {
        if (condition)
            System.out.println("SUCCESS");
        else
            System.out.println(ColorUtil.RgbToAnsi(Color.red, colorSetting) + "FAIL" + ColorUtil
                    .ANSI_NORMAL);
    }

    private String makeRelative(String absolutePath) {
        // I'm not sure if this works as expected, but I'm simply removing prefix of absolutePath
        String pathRegex = System.getProperty("user.dir")
                // on Windows, `\` characters in file paths are causing problem, so we need to
                // escape one more level:
                .replaceAll("\\\\", "\\\\\\\\");
        return absolutePath.replaceFirst(pathRegex, "");
    }

    private void makeReport(Proc p, String definition, String testName) {
        if (reportGen == null)
            return;
        if (p.isSuccess())
            reportGen.addSuccess(definition, testName,
                    p.getTimeDelta(), p.getPgmOut(), p.getPgmErr());
        else
            reportGen.addFailure(definition, testName,
                    p.getTimeDelta(), p.getPgmOut(), p.getPgmErr(), p.getReason());
    }
}

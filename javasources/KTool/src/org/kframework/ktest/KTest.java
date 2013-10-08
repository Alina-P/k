package org.kframework.ktest;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.kframework.ktest.execution.Execution;
import org.kframework.ktest.execution.Task;
import org.kframework.ktest.tests.Program;
import org.kframework.ktest.tests.Test;
import org.kframework.utils.ColorUtil;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.errorsystem.KException.ExceptionType;
import org.kframework.utils.errorsystem.KException.KExceptionGroup;
import org.kframework.utils.file.FileUtil;
import org.kframework.utils.file.KPaths;
import org.kframework.utils.general.GlobalSettings;
import org.kframework.utils.OptionComparator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class KTest {

    private static final String USAGE = "ktest <file> [options]";
    private static final String HEADER_STANDARD = "<file> is either a K definition (single job mode) or an XML configuration (batch mode).";
    private static final String FOOTER_STANDARD = "";
    private static final String HEADER_EXPERIMENTAL = "Experimental options:";
    private static final String FOOTER_EXPERIMENTAL = System.getProperty("line.separator") + "These options are non-standard and subject to change without notice.";
    public static void printUsageS(KTestOptionsParser op) {
        org.kframework.utils.Error.helpMsg(USAGE, HEADER_STANDARD, FOOTER_STANDARD, op.getOptionsStandard(), new OptionComparator(op.getOptionList()));
    }
    public static void printUsageE(KTestOptionsParser op) {
        org.kframework.utils.Error.helpMsg(USAGE, HEADER_EXPERIMENTAL, FOOTER_EXPERIMENTAL, op.getOptionsExperimental(), new OptionComparator(op.getOptionList()));
    }

    public static void test(String[] args) {

        KTestOptionsParser op = new KTestOptionsParser();
        CommandLine cmd = op.parse(args);
        if (cmd == null) {
            printUsageS(op);
            System.exit(1);
        }

        // Help
        if (cmd.hasOption(Configuration.HELP_OPTION)) {
            printUsageS(op);
            System.exit(0);
        }
        if (cmd.hasOption(Configuration.HELP_EXPERIMENTAL_OPTION)) {
            printUsageE(op);
            System.exit(0);
        }

        // Version
        if (cmd.hasOption(Configuration.VERSION_OPTION)) {
            String msg = FileUtil.getFileContent(KPaths.getKBase(false)
                    + KPaths.VERSION_FILE);
            System.out.println(msg);
            System.exit(0);
        }

        // Input argument
        String input = null;
        String[] remainingArgs = cmd.getArgs();
        if (remainingArgs.length < 1) {
            String msg = "You have to provide an input file, which is either a K definition (*.k) or a test configuration (*.xml).";
            GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, "command line", "System file."));
        } else {
            input = remainingArgs[0];
        }
        // Single job mode
        if (input.endsWith(".k")) {
            Configuration.KDEF = input;
            Configuration.SINGLE_DEF_MODE = true;
            // Invalid: --directory
            if (cmd.hasOption(Configuration.DIRECTORY_OPTION)) {
                String msg = "You cannot use --" + Configuration.DIRECTORY_OPTION + " option when a single K definition is given: " + Configuration.KDEF;
                GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, "command line", "System file."));
            }
            // Required: --extensions
            if (cmd.hasOption(Configuration.EXTENSIONS_OPTION)) {
                Configuration.EXTENSIONS = Arrays.asList(cmd.getOptionValue(Configuration.EXTENSIONS_OPTION).split("\\s+"));
            } else {
                if (cmd.hasOption(Configuration.PROGRAMS_OPTION)) {
                    String msg = "You have to provide a list of extensions by using --" + Configuration.EXTENSIONS_OPTION + " option, when --" + Configuration.PROGRAMS_OPTION + " is given.";
                    GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, "command line", "System file."));
                }
            }
        // Batch job mode
        } else if (input.endsWith(".xml")) {
            Configuration.CONFIG = input;
            Configuration.SINGLE_DEF_MODE = false;
            // Optional: --directory
            if (cmd.hasOption(Configuration.DIRECTORY_OPTION)) {
                Configuration.KDEF = cmd.getOptionValue(Configuration.DIRECTORY_OPTION);
                if (!new File(Configuration.KDEF).isDirectory()) {
                    String msg = "Invalid options: " + Configuration.KDEF + " is not a directory. You should provide a (root) directory where K definitions reside.";
                    GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, "command line", "System file."));
                }
            }
            // Invalid: --extensions
            if (cmd.hasOption(Configuration.EXTENSIONS_OPTION)) {
                String msg = "You cannot use --" + Configuration.EXTENSIONS_OPTION + " option when a test configuration is given: " + Configuration.CONFIG;
                GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, "command line", "System file."));
            }
            // Invalid: --exclude
            if (cmd.hasOption(Configuration.EXCLUDE_OPTION)) {
                String msg = "You cannot use --" + Configuration.EXCLUDE_OPTION + " option when a test configuration is given: " + Configuration.CONFIG;
                GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, "command line", "System file."));
            }
        } else {
            String msg = "You have to provide a valid input file, which is either a K definition (*.k) or a test configuration (*.xml).";
            GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, "command line", "System file."));
        }

        // Programs folder
        if (cmd.hasOption(Configuration.PROGRAMS_OPTION)) {
            Configuration.PGM_DIR = cmd
                    .getOptionValue(Configuration.PROGRAMS_OPTION);
            // also set the results to be programs folder by default
            Configuration.RESULTS_FOLDER = Configuration.PGM_DIR;
        }

        // List of excluded programs
        if (cmd.hasOption(Configuration.EXCLUDE_OPTION)) {
            Configuration.EXCLUDE_PROGRAMS = Arrays.asList(cmd.getOptionValue(
                    Configuration.EXCLUDE_OPTION).split("\\s+"));
        }

        // Results directory
        if (cmd.hasOption(Configuration.RESULTS_OPTION)) {
            Configuration.RESULTS_FOLDER = cmd
                    .getOptionValue(Configuration.RESULTS_OPTION);
        }

        if (cmd.hasOption(Configuration.REPORT_OPTION)) {
            Configuration.REPORT = true;
        }

        // Resolve skip options
        if (cmd.hasOption(Configuration.SKIP_OPTION)) {
            String[] stepsToSkip = cmd
                    .getOptionValue(Configuration.SKIP_OPTION).split("\\s+");
            for (int i = 0; i < stepsToSkip.length; i++) {
                if (stepsToSkip[i].equals(Configuration.KOMPILE_STEP)) {
                    Configuration.KOMPILE = false;
                }
                if (stepsToSkip[i].equals(Configuration.PDF_STEP)) {
                    Configuration.PDF = false;
                }
                if (stepsToSkip[i].equals(Configuration.PROGRAMS_STEP)) {
                    Configuration.PROGRAMS = false;
                }
            }
        }

        // Verbose
        if (cmd.hasOption(Configuration.VERBOSE_OPTION)) {
            Configuration.VERBOSE = true;
        }

        // Maximum number of threads
        if (cmd.hasOption(Configuration.PROCESSES_OPTION)) {
            Execution.POOL_SIZE = Integer.parseInt(cmd
                    .getOptionValue(Configuration.PROCESSES_OPTION));
        }

        // execution
        if (Configuration.SINGLE_DEF_MODE) {
            List<Test> alltests = new LinkedList<Test>();

            List<String> pgmsFolder = Configuration.PGM_DIR == null ? new LinkedList<String>()
                    : Arrays.asList(Configuration.PGM_DIR.split("\\s+"));
            List<String> resultsFolder = Configuration.RESULTS_FOLDER == null ? new LinkedList<String>()
                    : Arrays.asList(Configuration.RESULTS_FOLDER.split("\\s+"));
            Test test = new Test(Configuration.KDEF, pgmsFolder, resultsFolder,
                    Configuration.EXTENSIONS, Configuration.EXCLUDE_PROGRAMS,
                    System.getProperty("user.dir"));
            alltests.add(test);

            testing(0, new File(System.getProperty("user.dir")), alltests);
        } else {
            testConfig(Configuration.CONFIG, Configuration.KDEF,
                    Configuration.PGM_DIR, Configuration.RESULTS_FOLDER);
        }
    }

    private static List<Test> parseXMLConfig(String configFile,
            String rootDefDir, String rootProgramsDir, String rootResultsDir)
            throws ParserConfigurationException, SAXException, IOException {
        List<Test> alltests = new LinkedList<Test>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        if (!new File(configFile).exists()) {
            GlobalSettings.kem.register(new KException(ExceptionType.ERROR,
                    KExceptionGroup.CRITICAL, "File " + configFile
                            + " does not exists", "command line",
                    "System file."));
        }

        System.out.println("Buildfile: " + configFile);
        Document doc = dBuilder.parse(new File(configFile));
        Element root = doc.getDocumentElement();

        NodeList test = root.getElementsByTagName(Configuration.TEST);
        for (int i = 0; i < test.getLength(); i++)
            alltests.add(new Test((Element) test.item(i), rootDefDir,
                    rootProgramsDir, rootResultsDir, System
                            .getProperty("user.dir")));

        return alltests;
    }

    public static void testConfig(String configFile, String rootDir,
            String rootProgramsDir, String rootResultsDir) {

        int exitCode = 0;
        List<Test> alltests = new LinkedList<Test>();

        // load config
        try {
            alltests = parseXMLConfig(configFile, rootDir, rootProgramsDir,
                    rootResultsDir);
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            exitCode = 1;
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            exitCode = 1;
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            exitCode = 1;
            e.printStackTrace();
        }

        testing(exitCode, new File(System.getProperty("user.dir")), alltests);
    }

    private static void testing(int exitCode, File homeDir, List<Test> alltests) {
        // compile definitions first
        int i = 0;
        System.out.print("Kompiling the language definitions...");
        Map<Test, Task> definitions = new TreeMap<Test, Task>();
        for (Test test : alltests) {
            Task def = test.getDefinitionTask(homeDir);
            definitions.put(test, def);
            if (Configuration.KOMPILE) {
                Execution.execute(def);
            }
            if (test.runOnOS()) {
                Task unixOnlyScript = test.getUnixOnlyScriptTask(homeDir);
                if (unixOnlyScript != null) {
                    Execution.execute(unixOnlyScript);
                }
            }
            i++;
        }
        if (Configuration.KOMPILE) {
            System.out.println("(" + i + ")");
        } else {
            System.out.println("\nSkipped " + i + " definitions");
        }
        Execution.finish();

        if (Configuration.KOMPILE) {

            if (!GlobalSettings.isWindowsOS()) {
                // report
                for (Entry<Test, Task> entry : definitions.entrySet()) {
                    entry.getKey().reportCompilation(entry.getValue());
                }
            }

            // console display
            String kompileStatus = "\n";
            for (Entry<Test, Task> entry : definitions.entrySet()) {
                if (!entry.getKey().compiled(entry.getValue())) {
                    kompileStatus += ColorUtil.RgbToAnsi(Color.red) + "FAIL: "
                            + entry.getKey().getLanguage()
                            + ColorUtil.ANSI_NORMAL + "\n";
                    exitCode = 1;
                }
            }
            if (kompileStatus.equals("\n"))
                kompileStatus = "SUCCESS";
            System.out.println(kompileStatus);
        }

        // compile pdf definitions
        i = 0;
        System.out.print("Generating PDF documentation...");
        Map<Test, Task> pdfDefinitions = new TreeMap<Test, Task>();
        for (Test test : alltests) {
            // also compile pdf if set
            if (test.getPdf()) {
                Task pdfDef = test.getPdfDefinitionTask(homeDir);
                if (Configuration.PDF) {
                    pdfDefinitions.put(test, pdfDef);
                    Execution.execute(pdfDef);
                }
                i++;
            }
        }
        if (Configuration.PDF) {
            System.out.println("(" + i + ")");
        } else {
            System.out.println("\nSkipped " + i + " definitions");
        }
        Execution.finish();

        if (Configuration.PDF) {

            if (!GlobalSettings.isWindowsOS()) {
                // create XML report
                for (Entry<Test, Task> entry : pdfDefinitions.entrySet()) {
                    entry.getKey().reportPdfCompilation(entry.getValue());
                }
            }
            // console messages
            String pdfKompileStatus = "\n";
            for (Entry<Test, Task> entry : pdfDefinitions.entrySet()) {
                if (!entry.getKey().compiledPdf(entry.getValue())) {
                    pdfKompileStatus += ColorUtil.RgbToAnsi(Color.red)
                            + "FAIL: " + entry.getKey().getLanguage()
                            + ColorUtil.ANSI_NORMAL + "\n";
                    exitCode = 1;
                }
            }
            if (pdfKompileStatus.equals("\n"))
                pdfKompileStatus = "SUCCESS";
            System.out.println(pdfKompileStatus);
        }

        // execute all programs (for which corresponding definitions are
        // compiled)
        for (Entry<Test, Task> dentry : definitions.entrySet()) {
            Test test = dentry.getKey();
            if (test.compiled(dentry.getValue()) && test.runOnOS()) {

                System.out.print("Running " + test.getLanguage()
                        + " programs... " + test.getTag());

                // execute
                List<Program> pgms = test.getPrograms();
                Map<Program, Task> all = new TreeMap<Program, Task>();
                i = 0;
                for (Program p : pgms) {
                    Task task = p.getTask(homeDir);
                    all.put(p, task);
                    if (Configuration.PROGRAMS) {
                        Execution.tpe.execute(task);
                    }
                    i++;
                }
                if (Configuration.PROGRAMS) {
                    System.out.println("(" + i + ")");
                } else {
                    System.out.println("\nSkipped " + i + " programs");
                }
                Execution.finish();

                if (Configuration.PROGRAMS) {

                    if (!GlobalSettings.isWindowsOS()) {
                        // report
                        for (Entry<Program, Task> entry : all.entrySet()) {
                            entry.getKey().reportTest(entry.getValue());
                        }
                    }

                    // console
                    String pgmOut = "";
                    for (Entry<Program, Task> entry : all.entrySet()) {
                        if (!entry.getKey().success(entry.getValue())) {
                            pgmOut += ColorUtil.RgbToAnsi(Color.red) + "FAIL: "
                                    + entry.getKey().getProgramPath()
                                    + ColorUtil.ANSI_NORMAL + "\n";
                            exitCode = 1;
                        }
                    }
                    if (pgmOut.equals(""))
                        pgmOut = "SUCCESS";
                    System.out.println(pgmOut);
                }
            }
        }

        System.exit(exitCode);
    }

    public static void copyFolder(File src, File dest) throws IOException {
        FileUtils.copyDirectory(src, dest);
    }
}

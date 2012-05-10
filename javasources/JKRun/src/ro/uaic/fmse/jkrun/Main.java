package ro.uaic.fmse.jkrun;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ro.uaic.fmse.runner.KRunner;

public class Main {

	static class OptionComparator implements Comparator {
		public int compare(Object obj1, Object obj2) {
			Option opt1 = (Option) obj1;
			Option opt2 = (Option) obj2;
			int index1 = CommandlineOptions.optionList.indexOf(opt1);
			int index2 = CommandlineOptions.optionList.indexOf(opt2);

			if (index1 > index2)
				return 1;
			else if (index1 < index2)
				return -1;
			else
				return 0;
		}
	}

	private static final String USAGE = "jkrun [options] <file>" + K.lineSeparator;
	private static final String HEADER = "";
	private static final String FOOTER = K.lineSeparator + "jkrun also has several predefined option groups such as --search," + K.lineSeparator
			+ "--config, and --no-config. These predefined groups can be found in " + K.lineSeparator + "$K_BASE/tools/global-defaults.desk";

	public static void printUsage(Options options) {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.setOptionComparator(new Main.OptionComparator());
		helpFormatter.setWidth(100);
		helpFormatter.printHelp(USAGE, HEADER, options, FOOTER);
		System.out.println();
	}

	public static void printVersion() {
		System.out.println("JKrun 0.1.0\n" + "Copyright (C) 2012 Necula Emilian & Raluca");
	}

	public static void printStatistics() {
		PrettyPrintOutput p = new PrettyPrintOutput();
		String input = K.userdir + K.fileSeparator + K.maude_output;
		File file = new File(input);
		if (K.maude_cmd.equals("search")) {
			String totalStates = p.getSearchTagAttr(file, "total-states");
			String totalRewrites = p.getSearchTagAttr(file, "total-rewrites");
			String realTime = p.getSearchTagAttr(file, "real-time-ms");
			String cpuTime = p.getSearchTagAttr(file, "cpu-time-ms");
			String rewritesPerSecond = p.getSearchTagAttr(file, "rewrites-per-second");
			System.out.println(PrettyPrintOutput.ANSI_BLUE + "states: " + totalStates + " rewrites: " + totalRewrites + " in " + cpuTime + "ms cpu (" + realTime + "ms real) (" + rewritesPerSecond
					+ " rewrites/second)" + PrettyPrintOutput.ANSI_RESET);
		} else {
			String totalRewrites = p.getResultTagAttr(file, "total-rewrites");
			String realTime = p.getResultTagAttr(file, "real-time-ms");
			String cpuTime = p.getResultTagAttr(file, "cpu-time-ms");
			String rewritesPerSecond = p.getResultTagAttr(file, "rewrites-per-second");
			System.out.println(PrettyPrintOutput.ANSI_BLUE + "rewrites: " + totalRewrites + " in " + cpuTime + "ms cpu (" + realTime + "ms real) (" + rewritesPerSecond + " rewrites/second)"
					+ PrettyPrintOutput.ANSI_RESET);
		}
	}

	public static void printSearchResults() {
		PrettyPrintOutput p = new PrettyPrintOutput();
		String input = K.userdir + K.fileSeparator + K.maude_output;
		File file = new File(input);
		String solutionNumber = p.getSearchTagAttr(file, "solution-number");
		String stateNumber = p.getSearchTagAttr(file, "state-number");
		System.out.println("Search results:\n");
		System.out.println(PrettyPrintOutput.ANSI_BLUE + "Solution: " + solutionNumber + ", state " + stateNumber + PrettyPrintOutput.ANSI_RESET);
	}

	/**
	 * 
	 * @param args
	 * @return get the command with the options passed to jkrun
	 */
	public String getCommand(String args[]) {
		StringBuilder str = new StringBuilder();

		for (String arg : args) {
			str.append(arg);
		}
		return str.toString();
	}

	public static String initOptions(String path, String lang) {
		String result = null;
		String path_ = null;
		String compiledFile = null;
		String fileName = null;
		StringBuilder str = new StringBuilder();
		int count = 0;

		try {
			ArrayList<File> maudeFiles = FileUtil.searchFiles(path, "maude", false);
			for (File maudeFile : maudeFiles) {
				String fullPath = maudeFile.getCanonicalPath();
				path_ = FileUtil.dropExtension(fullPath, ".", K.fileSeparator);
				int sep = path_.lastIndexOf(K.fileSeparator);
				fileName = path_.substring(sep + 1);
				if (fileName.startsWith(lang)) {
					// result = compiledFile;
					result = fullPath;
					str.append("\"./" + fileName + "\" ");
					count++;
				}
			}
			if (count > 1) {
				Error.report("\nMultiple compiled definitions found.\nPlease use only one of: " + str.toString());
			} else if (maudeFiles.size() == 1) {
				result = K.k_definition + "-compiled.maude";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static void resolveOption(String optionName, String lang, CommandLine cmd) {
		String s = FileUtil.dropKExtension(K.k_definition, ".", K.fileSeparator);
		int sep = s.lastIndexOf(K.fileSeparator);
		String str = s.substring(sep + 1).toUpperCase();

		if (optionName == "compiled-def") {
			if (cmd.hasOption("k-definition")) {
				K.compiled_def = s + "-compiled.maude";
			} else {
				K.compiled_def = initOptions(K.userdir, lang);
			}
		} else if (optionName == "main-module") {
			K.main_module = str;
		} else if (optionName == "syntax-module") {
			K.syntax_module = str + "-SYNTAX";
		}
	}

	/**
	 * @param cmds
	 *            represents the arguments/options given to jkrun command..
	 */
	public static void execute_Krun(String cmds[]) {
		CommandlineOptions cmd_options = new CommandlineOptions();
		CommandLine cmd = cmd_options.parse(cmds);
		try {
			// Parse the program arguments
            
			if (cmd.hasOption('h') || cmd.hasOption('?')) {
				K.help = true;
			}
			if (cmd.hasOption('v')) {
				K.version = true;
			}
			if (cmd.hasOption("desk-file")) {
				K.desk_file = new File(cmd.getOptionValue("desk-file")).getCanonicalPath();
				// System.out.println("desk-file=" + K.desk_file);
			}
			if (cmd.hasOption("k-definition")) {
				K.k_definition = new File(cmd.getOptionValue("k-definition")).getCanonicalPath();
				K.k_definition = FileUtil.dropKExtension(K.k_definition, ".", K.fileSeparator);
				//System.out.println("k-definition=" + K.k_definition);
			}
			if (cmd.hasOption("main-module")) {
				K.main_module = cmd.getOptionValue("main-module");
				// System.out.println("main-module=" + K.main_module);
			}
			if (cmd.hasOption("syntax-module")) {
				K.syntax_module = cmd.getOptionValue("syntax-module");
				// System.out.println("syntax-module=" + K.syntax_module);
			}
			if (cmd.hasOption("parser")) {
				K.parser = cmd.getOptionValue("parser");
			}
			if (cmd.hasOption("io")) {
				K.io = true;
			}
			if (cmd.hasOption("no-io")) {
				K.io = false;
			}
			if (cmd.hasOption("statistics")) {
				K.statistics = true;
			}
			if (cmd.hasOption("no-statistics")) {
				K.statistics = false;
			}
			if (cmd.hasOption("color")) {
				K.color = true;
			}
			if (cmd.hasOption("no-color")) {
				K.color = false;
			}
			if (cmd.hasOption("parens")) {
				K.parens = true;
			}
			if (cmd.hasOption("no-parens")) {
				K.parens = false;
			}
			if (cmd.hasOption("compiled-def")) {
				K.compiled_def = new File(cmd.getOptionValue("compiled-def")).getCanonicalPath();
				// System.out.println("compiled-def=" + K.compiled_def);
			}
			if (cmd.hasOption("do-search")) {
				K.do_search = true;
			}
			if (cmd.hasOption("no-do-search")) {
				K.do_search = false;
			}
			if (cmd.hasOption("maude-cmd")) {
				K.maude_cmd = cmd.getOptionValue("maude-cmd");
				// System.out.println("maude-cmd=" + K.maude_cmd);
			}
			if (cmd.hasOption("xsearch-pattern")) {
				K.xsearch_pattern = cmd.getOptionValue("xsearch-pattern");
				// System.out.println("xsearch-pattern=" + K.xsearch_pattern);
			}
			if (cmd.hasOption("output-mode")) {
				K.output_mode = cmd.getOptionValue("output-mode");
				// System.out.println("output-mode=" + K.output_mode);
			}
			if (cmd.hasOption("log-io")) {
				K.log_io = true;
			}
			if (cmd.hasOption("no-log-io")) {
				K.log_io = false;
			}
			if (cmd.hasOption("debug") && cmd.hasOption("rule-labels")) {
				K.debug = true;
				K.rule_labels = cmd.getOptionValue("rule-labels");
				// System.out.println("rule-labels=" + K.rule_labels);
			}
			if (cmd.hasOption("debug") && !cmd.hasOption("rule-labels")) {
				K.debug = true;
				Error.report("You have to provide some labels in order to start debugging.");
			}
			if (cmd.hasOption("trace"))
			{
				K.trace = true;
			}

			if (cmd.hasOption("pgm")) {
				K.pgm = new File(cmd.getOptionValue("pgm")).getCanonicalPath();
			}

			// printing the output according to the given options
			if (K.help) {
				printUsage(cmd_options.options);
				System.exit(0);
			}
			if (K.version) {
				printVersion();
				System.exit(0);
			}

			String[] remainingArguments = null;
			if (cmd_options.getCommandLine().getOptions().length > 0) {
				remainingArguments = cmd.getArgs();
			} else {
				remainingArguments = cmds;
			}
			String programArg = new String();
			if (remainingArguments.length > 0) {
				programArg = new File(remainingArguments[0]).getCanonicalPath();
				K.pgm = programArg;
			}
			if (K.pgm == null) {
				Error.usageError("missing required <file> argument");
			}
			File pgmFile = new File(K.pgm);
			if (!pgmFile.exists()) {
				Error.report("\nProgram file does not exist: " + K.pgm);
			}
			String pgm = FileUtil.getFilename(K.pgm, ".", K.fileSeparator);
			String lang = FileUtil.getExtension(K.pgm, ".");

			// by default
			if (!cmd.hasOption("k-definition")) {
				K.k_definition = new File(K.userdir).getCanonicalPath() + K.fileSeparator + lang;
			}

			initOptions(K.userdir, lang);

			if (!cmd.hasOption("compiled-def")) {
				resolveOption("compiled-def", lang, cmd);
			}
			if (!cmd.hasOption("main-module")) {
				resolveOption("main-module", lang, cmd);
			}
			if (!cmd.hasOption("syntax-module")) {
				resolveOption("syntax-module", lang, cmd);
			}

			if (K.compiled_def == null) {
				Error.report("\nCould not find a compiled K definition.");
			}
			File compiledFile = new File(K.compiled_def);
			if (!compiledFile.exists()) {
				Error.report("\nCould not find compiled definition: " + K.compiled_def + "\nPlease compile the definition by using `make' or `kompile'.");
			}

			/*System.out.println("K.k_definition=" + K.k_definition);
			System.out.println("K.syntax_module=" + K.syntax_module);
			System.out.println("K.main_module=" + K.main_module);*/

			/* String kastCmd = new String(); if (cmd.hasOption("k-definition")) { kastCmd += "--k-definition=" + K.k_definition; } if (cmd.hasOption("main-module")) { kastCmd += " " +
			 * "--main-module=" + K.main_module; } if (cmd.hasOption("--syntax-module")) { kastCmd += " " + "--syntax-module=" + K.syntax_module; } */

			// in KAST variable we obtain the output from running kast process on a program defined in K
			String KAST = new String();
			RunProcess rp = new RunProcess();
			// rp.execute("kast", "--k-definition=" + K.k_base + "/examples/languages/classic/"+ lang + "/" + lang, K.k_base + "/examples/languages/classic/" + lang + "/programs/" + pgm + "." +
			// lang);

			if (K.parser.equals("kast")) {
				// rp.execute("perl", K.kast, kastCmd, "-pgm=" + K.pgm);
				//rp.execute(new String[] { K.kast, "--definition=" + K.k_definition, "--main-module=" + K.main_module, "--syntax-module=" + K.syntax_module, "-pgm=" + K.pgm });
				rp.execute(new String[] { K.kast, K.pgm });
				/* rp.execute("kast", "--k-definition=" + K.k_base + "/examples/languages/research/" + lang + "/" + lang, K.k_base + "/examples/languages/research/" + lang + "/programs/" + pgm + "." +
				 * lang); */
			} else {
				System.out.println("The external parser to be used is:" + K.parser);
				// code to execute the external parser
				rp.execute(new String[] { K.parser, "--k-definition=" + K.k_definition, "--main-module=" + K.main_module, "--syntax-module=" + K.syntax_module, "-pgm=" + K.pgm });
				// System.exit(0);
			}
			if (rp.getStdout() != null) {
				KAST = rp.getStdout();
			}

			/* //create a file called temp.maude in which we copy the content from the imp-compiled.maude file and then append at the end of the file the following line
			 * "rew #eval (_`(_`) (#_(\"$PGM\"),.List{K}) |-> " + KAST + ") ." String s1 = K.userdir + "/temp.maude"; String s2 = K.k_base + "/examples/languages/classic/" + lang + "/" + lang +
			 * "-compiled.maude"; //String s2 = K.k_base + "/examples/languages/research/" + lang + "/" + lang + "-compiled.maude"; //String s2 = K.k_base +
			 * "/examples/languages/classic/imp/imp-compiled.maude"; String s3 = "rew #eval (_`(_`) (#_(\"$PGM\"),.List{K}) |-> " + KAST + ") ."; FileUtil.copyFile(s1, s2, s3);
			 * 
			 * //call maude process on the temp.maude file we previously created and obtain the output of execution in an XML file format Maude.run_maude(new String[] {"load "+ s1}); */

			// run IOServer
			String s = new String();
			if (K.do_search) {
				if (K.maude_cmd.equals("search")) {
					s = "set show command off ." + K.lineSeparator + "search #eval(__((_|->_((# \"$PGM\"(.List{K})) ,(" + KAST + "))),(.).Map)) =>! B:Bag .";
				} else {
					Error.report("For the do-search option you need to specify that --maude-cmd=search");
				}
			} else if (cmd.hasOption("maude-cmd")) {
				s = "set show command off ." + K.lineSeparator + K.maude_cmd + " #eval(__((_|->_((# \"$PGM\"(.List{K})) ,(" + KAST + "))),(.).Map)) .";
			} else if (cmd.hasOption("xsearch-pattern")) {
				s = "set show command off ." + K.lineSeparator + "search #eval(__((_|->_((# \"$PGM\"(.List{K})) ,(" + KAST + "))),(.).Map)) " + K.xsearch_pattern + " .";
			} else {
				s = "set show command off ." + K.lineSeparator + "erew #eval(__((_|->_((# \"$PGM\"(.List{K})) ,(" + KAST + "))),(.).Map)) .";
			}
			
			if (K.trace)
				s = "set trace on ." + K.lineSeparator + s;
			
			FileUtil.createFile(K.maude_io_cmd, s);

			/* String compiled_file = new String(); if (cmd.hasOption("compiled-def")) { compiled_file = K.compiled_def; } else { compiled_file = K.k_base + K.fileSeparator + "examples" +
			 * K.fileSeparator + "languages" + K.fileSeparator + "research" + K.fileSeparator + lang + K.fileSeparator + lang + "-compiled.maude";
			 * 
			 * delims = "[.]"; tokens = K.k_definition.split(delims); String kDef = tokens[0]; compiled_file = kDef + "-compiled.maude"; compiled_file = FileUtil.getFilename(K.k_definition, ".",
			 * K.fileSeparator); } */

			// String jar_file = K.k_base + K.fileSeparator + "core" + K.fileSeparator + "java" + K.fileSeparator + "wrapperAndServer.jar";
			// String content = "java -jar " + jar_file + " --maudeFile " + compiled_file + " --moduleName " + lang.toUpperCase() + " --commandFile io-cmd.maude";

			File outFile = FileUtil.createMaudeFile(K.maude_out);
			if (K.log_io) {
				KRunner.main(new String[] { "--maudeFile", K.compiled_def, "--moduleName", K.main_module, "--commandFile", K.maude_io_cmd, "--outputFile", outFile.getCanonicalPath(), "--createLogs" });
			}
			if (!K.io) {
				KRunner.main(new String[] { "--maudeFile", K.compiled_def, "--moduleName", K.main_module, "--commandFile", K.maude_io_cmd, "--outputFile", outFile.getCanonicalPath(), "--noServer" });
			} else {
				KRunner.main(new String[] { "--maudeFile", K.compiled_def, "--moduleName", K.main_module, "--commandFile", K.maude_io_cmd, "--outputFile", outFile.getCanonicalPath() });
			}
			PrettyPrintOutput p = new PrettyPrintOutput();
			String input = K.userdir + K.fileSeparator + K.maude_output;
			File file = new File(input);
			String red = p.processDoc(file);
			String prettyOutput = XmlUtil.formatXml(red, K.color);
			if (K.maude_cmd.equals("search") && K.do_search) {
				printSearchResults();
			}
			if (K.output_mode.equals("pretty")) {
				System.out.print(K.lineSeparator + prettyOutput);
			} else if (K.output_mode.equals("raw")) {
				String output = FileUtil.parseOutputMaude(K.maude_out);
				System.out.println(output);
			} else if (K.output_mode.equals("none")) {
				System.out.print("");
			} else {
				Error.report(K.output_mode + " is not a valid value for output-mode option");
			}

			if (K.statistics) {
				printStatistics();
			}

			// save the pretty-printed output of jkrun in a file
			FileUtil.createFile(K.krun_output, prettyOutput);

			// delete temporary files
			// FileUtil.deleteFile(K.maude_io_cmd);
			// FileUtil.deleteFile(K.maude_output);

			System.exit(1);

		} catch (ParseException exp) {
			System.out.println("Unexpected exception:" + exp.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("You provided bad program arguments!");
			e.printStackTrace();
			printUsage(cmd_options.options);
			System.exit(1);
		}

	}

	public static void main(String[] args) {
		
		execute_Krun(args);
	}

}

package org.kframework.krun;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Runtime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import jline.ArgumentCompletor;
import jline.Completor;
import jline.ConsoleReader;
import jline.FileNameCompletor;
import jline.MultiCompletor;
import jline.SimpleCompletor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.fusesource.jansi.AnsiConsole;
import org.kframework.krun.runner.KRunner;
import org.kframework.kil.ASTNode;
import org.kframework.kil.Term;
import org.kframework.backend.maude.MaudeFilter;
import org.kframework.compile.transformers.FlattenSyntax;
import org.kframework.compile.utils.MetaK;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Main {

	private static final String USAGE_KRUN = "krun [options] <file>" + K.lineSeparator;
	private static final String USAGE_DEBUG = "Enter one of the following commands without \"--\" in front. " + K.lineSeparator + "For autocompletion press TAB key and for accessing the command" + K.lineSeparator + "history use up and down arrows." + K.lineSeparator;
	private static final String HEADER = "";
	private static final String FOOTER = "";

	//needed for displaying the krun help
	public static void printKRunUsage(Options options) {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.setOptionComparator(new CommandlineOptions.OptionComparator());
		helpFormatter.setWidth(79);
		helpFormatter.printHelp(USAGE_KRUN, HEADER, options, FOOTER);
		System.out.println();
	}

	//needed for displaying the krun debugger help
	public static void printDebugUsage(Options options) {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.setOptionComparator(new CommandlineOptions.OptionComparator());
		helpFormatter.setWidth(79);
		helpFormatter.printHelp(USAGE_DEBUG, HEADER, options, FOOTER);
		System.out.println();
	}

	public static void printVersion() {
		System.out.println("JKrun 0.2.0\n" + "Copyright (C) 2012 Necula Emilian & Raluca");
	}

	//find the maude compiled definitions on the disk
	public static String initOptions(String path) {
		String result = null;
		String path_ = null;
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
				if (fileName.endsWith("-compiled")) {
					result = fullPath;
					str.append("\"./" + fileName + "\" ");
					count++;
				}
			}
			if (count > 1) {
				Error.report("\nMultiple compiled definitions found.\nPlease use only one of: " + str.toString());
			} else if (count == 1) {
				return result;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	//set the main-module, syntax-module and k-definition according to their correlation with compiled-def
	public static void resolveOption(String optionName, CommandLine cmd) {
		String s = FileUtil.dropKExtension(K.k_definition, ".", K.fileSeparator);
		int sep = s.lastIndexOf(K.fileSeparator);
		String str = s.substring(sep + 1).toUpperCase();
		int index;

		if (optionName == "compiled-def") {
			if (cmd.hasOption("k-definition")) {
				K.compiled_def = s + "-compiled.maude";
			} else {
				K.compiled_def = initOptions(K.userdir);
				if (K.compiled_def != null) {
					index = K.compiled_def.indexOf("-compiled.maude");
					K.k_definition = K.compiled_def.substring(0, index);
				}
			}
		} else if (optionName == "main-module") {
			if (cmd.hasOption("syntax-module")) {
				int pos = K.syntax_module.indexOf("-SYNTAX");
				K.main_module = K.syntax_module.substring(0, pos);
			} else {
				K.main_module = str;
			}
		} else if (optionName == "syntax-module") {
			if (cmd.hasOption("main-module")) {
				K.syntax_module = K.main_module + "-SYNTAX";
			} else {
				K.syntax_module = str + "-SYNTAX";
			}
		}
	}

	public static String makeConfiguration(String kast, Properties configuration_variables) {
		org.kframework.parser.concrete.KParser.ImportTbl(K.kdir + "/def/K3Disamb.tbl");
		String output = "";
		boolean hasPGM = false;
		int items = 0;
		Enumeration<Object> en = configuration_variables.keys();
		while(en.hasMoreElements()) {
			String name = (String) en.nextElement();
			String value = configuration_variables.getProperty(name);
			//TODO: get sort from configuration term in definition and pass it here
			String parsed = "";
			try {
				ASTNode term = org.kframework.utils.DefinitionLoader.parseCmdString(value, "");
				term = term.accept(new FlattenSyntax());
				term = MetaK.kWrapper((Term) term);
				MaudeFilter maudeFilter = new MaudeFilter();
				term.accept(maudeFilter);
				parsed = maudeFilter.getResult();
			} catch (Exception e1) {
				Error.report(e1.getMessage());
			}
			output += "__(_|->_((# \"$" + name + "\"(.List{K})), (" + parsed + ")), ";
			hasPGM = hasPGM || name.equals("PGM");
			items++;
		}
		if (!hasPGM) {
			output += "__(_|->_((# \"$PGM\"(.List{K})), (" + kast + ")), ";
			items++;
		}
		output += "(.).Map";
		for (int i = 0; i < items; i++) {
			output += ")";
		}
		return output;
	}


	// execute krun in normal mode (i.e. not in debug mode)
	public static void normalExecution(String KAST, String lang, RunProcess rp, CommandlineOptions cmd_options) {
		try {
			String s = new String();
			List<String> red = new ArrayList<String>();
			StringBuilder aux1 = new StringBuilder();
			CommandLine cmd = cmd_options.getCommandLine();
			
			if (K.do_search) {
				if ("search".equals(K.maude_cmd)) {
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					String buffer = null;
					// detect if the input comes from console or redirected from a pipeline
					Console c = System.console();
					if (c == null) {
						try {
							buffer = br.readLine();
						} catch (IOException ioe) {
							ioe.printStackTrace();
						} finally {
							if (br != null) {
								try {
									br.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					}
					if (cmd.hasOption("bound") && cmd.hasOption("depth")) {
						s = "set show command off ." + K.lineSeparator + "search [" + K.bound + "," + K.depth + "] " + "#eval(__(" + makeConfiguration(KAST, K.configuration_variables) + ",(_|->_((# \"$noIO\"(.List{K})) , (List2KLabel_(#noIO)(.List{K}))))"
								+ ",(_|->_((# \"$stdin\"(.List{K})) , ((# \"" + buffer + "\\n\"(.List{K})))))" + ",(.).Map)) ";
					} else if (cmd.hasOption("bound")) {
						s = "set show command off ." + K.lineSeparator + "search [" + K.bound + "] " + "#eval(__(" + makeConfiguration(KAST, K.configuration_variables) + ",(_|->_((# \"$noIO\"(.List{K})) , (List2KLabel_(#noIO)(.List{K}))))"
								+ ",(_|->_((# \"$stdin\"(.List{K})) , ((# \"" + buffer + "\\n\"(.List{K})))))" + ",(.).Map)) ";
					} else if (cmd.hasOption("depth")) {
						s = "set show command off ." + K.lineSeparator + "search [," + K.depth + "] " + "#eval(__(" + makeConfiguration(KAST, K.configuration_variables) + ",(_|->_((# \"$noIO\"(.List{K})) , (List2KLabel_(#noIO)(.List{K}))))"
								+ ",(_|->_((# \"$stdin\"(.List{K})) , ((# \"" + buffer + "\\n\"(.List{K})))))" + ",(.).Map)) ";
					} else {
						s = "set show command off ." + K.lineSeparator + "search #eval(__(" + makeConfiguration(KAST, K.configuration_variables) + ",(_|->_((# \"$noIO\"(.List{K})) , (List2KLabel_(#noIO)(.List{K}))))"
								+ ",(_|->_((# \"$stdin\"(.List{K})) , ((# \"" + buffer + "\\n\"(.List{K})))))" + ",(.).Map)) ";
					}
					s += K.pattern + " .";
					if (K.showSearchGraph) {
						s += K.lineSeparator + "show search graph" + " .";
					}
					/*
					 * if (cmd.hasOption("xsearch-pattern")) { s += K.xsearch_pattern + " ."; //s = "set show command off ." + K.lineSeparator + "search #eval(__(" + makeConfiguration(KAST, K.configuration_variables) + ",(.).Map)) " + "\"" + K.xsearch_pattern +
					 * "\"" + " ."; } else s += " =>! B:Bag .";
					 */

				} else {
					Error.report("For the do-search option you need to specify that --maude-cmd=search");
				}
			} else if (cmd.hasOption("maude-cmd")) {
				s = "set show command off ." + K.lineSeparator + K.maude_cmd + " #eval(__(" + makeConfiguration(KAST, K.configuration_variables) + ",(.).Map)) .";
			} else {
				s = "set show command off ." + K.lineSeparator + "erew #eval(__(" + makeConfiguration(KAST, K.configuration_variables) + ",(.).Map)) .";
			}

			if (K.trace) {
				s = "set trace on ." + K.lineSeparator + s;
			}

			StringBuilder sb = new StringBuilder();
			if (K.model_checking.length() > 0) {
				// run kast for the formula to be verified
				File formulaFile = new File(K.model_checking);
				String KAST1 = new String();
				if (!formulaFile.exists()) {
					// Error.silentReport("\nThe specified argument does not exist as a file on the disc; it may represent a direct formula: " + K.model_checking);
					// assume that the specified argument is not a file and maybe represents a formula
					KAST1 = rp.runParser(K.parser, K.model_checking, false);
				} else {
					// the specified argument represents a file
					KAST1 = rp.runParser(K.parser, K.model_checking, true);
				}

				sb.append("mod MCK is" + K.lineSeparator);
				sb.append(" including " + K.main_module + " ." + K.lineSeparator + K.lineSeparator);
				sb.append(" op #initConfig : -> Bag ." + K.lineSeparator + K.lineSeparator);
				sb.append(" eq #initConfig =" + K.lineSeparator);
				sb.append("  #eval(__(" + makeConfiguration(KAST, K.configuration_variables) + ",(.).Map)) ." + K.lineSeparator);
				sb.append("endm" + K.lineSeparator + K.lineSeparator);
				sb.append("red" + K.lineSeparator);
				sb.append("_`(_`)(('modelCheck`(_`,_`)).KLabel,_`,`,_(_`(_`)(Bag2KLabel(#initConfig),.List`{K`}),");
				sb.append(K.lineSeparator);
				sb.append(KAST1 + ")" + K.lineSeparator + ") .");
				s = sb.toString();
			}

			FileUtil.createFile(K.maude_in, s);

			// run IOServer
			File outFile = FileUtil.createFile(K.maude_out);
			File errFile = FileUtil.createFile(K.maude_err);

			if (K.log_io) {
				KRunner.main(new String[] { "--maudeFile", K.compiled_def, "--moduleName", K.main_module, "--commandFile", K.maude_in, "--outputFile", outFile.getCanonicalPath(), "--errorFile", errFile.getCanonicalPath(), "--createLogs" });
			}
			if (!K.io) {
				KRunner.main(new String[] { "--maudeFile", K.compiled_def, "--moduleName", K.main_module, "--commandFile", K.maude_in, "--outputFile", outFile.getCanonicalPath(), "--errorFile", errFile.getCanonicalPath(), "--noServer" });
			} else {
				KRunner.main(new String[] { "--maudeFile", K.compiled_def, "--moduleName", K.main_module, "--commandFile", K.maude_in, "--outputFile", outFile.getCanonicalPath(), "--errorFile", errFile.getCanonicalPath() });
			}

			// check whether Maude produced errors
			rp.checkMaudeForErrors(errFile, lang);

			if ("search".equals(K.maude_cmd) && K.do_search && !cmd.hasOption("output")) {
				System.out.println("Search results:");
			}
			if ("pretty".equals(K.output_mode)) {
				PrettyPrintOutput p = new PrettyPrintOutput();
				p.preprocessDoc(K.maude_output, K.processed_maude_output);
				red = p.processDoc(K.processed_maude_output);
				for (String result : red) {
					aux1.append(result);
					if (!cmd.hasOption("output")) {
						AnsiConsole.out.println(result);
					}
				}
				//print search graph
				if ("search".equals(K.maude_cmd) && K.do_search && K.showSearchGraph) {
					System.out.println(K.lineSeparator + "The search graph is:" + K.lineSeparator);
					String result = p.printSearchGraph(K.processed_maude_output);
					AnsiConsole.out.println(result);
					//offer the user the possibility to request info about some nodes in the search graph  
					while (true) {
						System.out.print(K.lineSeparator + "Do you want to show information about a node from the search graph? (y/n):");
						BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
						String input = stdin.readLine();
						if (input.equals("y")) {
							System.out.print("Specify the id of the node:");
							stdin = new BufferedReader(new InputStreamReader (System.in));
							input = stdin.readLine();
							String result1 = p.printNodeSearchGraph(K.processed_maude_output, input);
							if (result1 != null) {
								System.out.println(result1);
							}
							else {
								System.out.println("A node with the specified id couldn't be found in the search graph");
							}
						}
						else if (input.equals("n")) {
							break;
						}
						else {
							System.out.println("You should specify one of the possible answers:y or n");
						}
					}
				}
			} else if ("raw".equals(K.output_mode)) {
				String output = new String();
				if (K.model_checking.length() > 0) {
					output = FileUtil.parseModelCheckingOutputMaude(K.maude_out);
				} else {
					if ("search".equals(K.maude_cmd)) {
						List<String> l = FileUtil.parseSearchOutputMaude(K.maude_out);
						if (l.size() > 0) {
							output = l.get(0);
						} else {
							output = "";
						}
					} else if ("erewrite".equals(K.maude_cmd)) {
						output = FileUtil.parseResultOutputMaude(K.maude_out);
					}
				}
				if (!cmd.hasOption("output")) {
					System.out.println(output);
				}

			} else if ("none".equals(K.output_mode)) {
				System.out.print("");
			} else {
				Error.report(K.output_mode + " is not a valid value for output-mode option");
			}

			// save the pretty-printed output of jkrun in a file
			if (cmd.hasOption("output")) {
				FileUtil.createFile(K.output, aux1.toString());
			}
		
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			System.out.println("You provided bad program arguments!");
			e.printStackTrace();
			printKRunUsage(cmd_options.getOptions());
			System.exit(1);
		}

	}

	// execute krun in debug mode (i.e. step by step execution)
	public static void debugExecution(String kast, String lang) {
		try {
			// adding autocompletion and history feature to the stepper internal commandline by using the JLine library
			ConsoleReader reader = new ConsoleReader();
			reader.setBellEnabled(false);

			List<Completor> argCompletor = new LinkedList<Completor>();
			//argCompletor.add(new SimpleCompletor(new String[] { "help", "abort", "resume", "step", "step-all", "show path labels" }));
			argCompletor.add(new SimpleCompletor(new String[] { "help", "abort", "resume", "step", "step-all", "select", "show-search-graph", "show-node" }));
			argCompletor.add(new FileNameCompletor());
			List<Completor> completors = new LinkedList<Completor>();
			completors.add(new ArgumentCompletor(argCompletor));
			reader.addCompletor(new MultiCompletor(completors));

			// first execute one step then prompt from the user an input
			System.out.println("After running one step of execution the result is:");
			String compiledFile = new String();
			compiledFile = new File(K.compiled_def).getCanonicalPath();
			String maudeCmd = "set show command off ." + K.lineSeparator + "load " + KPaths.windowfyPath(compiledFile) + K.lineSeparator + "rew [1] #eval(__(" + makeConfiguration(kast, K.configuration_variables) + ",(.).Map)) .";
			File outFile = FileUtil.createFile(K.maude_out);
			File errFile = FileUtil.createFile(K.maude_err);
			RunProcess rp = new RunProcess();
			rp.runMaude(maudeCmd, outFile.getCanonicalPath(), errFile.getCanonicalPath());
			// check whether Maude produced errors
			rp.checkMaudeForErrors(errFile, lang);

			// pretty-print the obtained configuration
			PrettyPrintOutput p = new PrettyPrintOutput();
			p.preprocessDoc(K.maude_output, K.processed_maude_output);
			List<String> red = p.processDoc(K.processed_maude_output);
			for (String result : red) {
				AnsiConsole.out.println(result);
			}

			while (true) {
				System.out.println();
				String input = reader.readLine("Command > ");

				// construct the right command line input when we specify the "step" option with an argument (i.e. step=3)
				input = input.trim();
				String[] tokens = input.split(" ");
				// store the tokens that are not a blank space
				List<String> items = new ArrayList<String>();
				for (int i = 0; i < tokens.length; i++) {
					if ((!(tokens[i].equals(" ")) && (!tokens[i].equals("")))) {
						items.add(tokens[i]);
					}
				}
				StringBuilder aux = new StringBuilder();
				// excepting the case of a command like: help
				if (items.size() > 1) {
					for (int i = 0; i < items.size(); i++) {
						if (i == items.size() - 1) {
							aux.append("=");
							aux.append(items.get(i));
						} else if (i == items.size() - 2) {
							aux.append(items.get(i));
						} else {
							aux.append(items.get(i) + " ");
						}
					}
					input = aux.toString();
				}
				// apply trim to remove possible blank spaces from the inserted command
				String[] cmds = new String[] { "--" + input };
				CommandlineOptions cmd_options = new CommandlineOptions();
				CommandLine cmd = cmd_options.parse(cmds);
				// when an error occurred during parsing the commandline continue the execution of the stepper
				if (cmd == null) {
					continue;
				} else {
					if (cmd.hasOption("help")) {
						printDebugUsage(cmd_options.getOptions());
					}
					if (cmd.hasOption("abort")) {
						System.exit(0);
					}
					if (cmd.hasOption("resume")) {
						// get the maudified version of the current configuration based on the xml obtained from -xml-log option
						String maudeConfig = XmlUtil.xmlToMaude(K.maude_output);
						//check first to see if we have a current configuration obtained at previous steps
						if (maudeConfig != null && maudeConfig.length() > 0) {
							maudeCmd = "set show command off ." + K.lineSeparator + "load " + KPaths.windowfyPath(compiledFile) + K.lineSeparator + "rew " + maudeConfig + " .";
							rp.runMaude(maudeCmd, outFile.getCanonicalPath(), errFile.getCanonicalPath());
							// check whether Maude produced errors
							rp.checkMaudeForErrors(errFile, lang);
	
							// pretty-print the obtained configuration
							K.maude_cmd = "erewrite";
							p = new PrettyPrintOutput();
							p.preprocessDoc(K.maude_output, K.processed_maude_output);
							red = p.processDoc(K.processed_maude_output);
							AnsiConsole.out.println(red.get(0));
	
							System.exit(0);
						}
						else {
							Error.silentReport("Wrong command: If you previously used the step-all command you must select" + K.lineSeparator + "first a solution with step command before executing steps of rewrites!");
						}
					}
					// one step execution (by default) or more if you specify an argument
					if (cmd.hasOption("step")) {
						// by default execute only one step at a time
						String arg = new String("1");
						String[] remainingArguments = null;
						remainingArguments = cmd.getArgs();
						if (remainingArguments.length > 0) {
							arg = remainingArguments[0];
						}
						// get the maudified version of the current configuration based on the xml obtained from -xml-log option
						String maudeConfig = XmlUtil.xmlToMaude(K.maude_output);
						//System.out.println("config=" + maudeConfig);
						//check first to see if we have a current configuration obtained at previous steps
						if (maudeConfig != null && maudeConfig.length() > 0) {
							maudeCmd = "set show command off ." + K.lineSeparator + "load " + KPaths.windowfyPath(compiledFile) + K.lineSeparator + "rew[" + arg + "] " + maudeConfig + " .";
							//System.out.println("Maude cmd:" + maudeCmd);
							rp.runMaude(maudeCmd, outFile.getCanonicalPath(), errFile.getCanonicalPath());
							// check whether Maude produced errors
							rp.checkMaudeForErrors(errFile, lang);
	
							// pretty-print the obtained configuration
							K.maude_cmd = "erewrite";
							p = new PrettyPrintOutput();
							p.preprocessDoc(K.maude_output, K.processed_maude_output);
							red = p.processDoc(K.processed_maude_output);
							AnsiConsole.out.println(red.get(0));
						}
						else {
							Error.silentReport("Wrong command: If you previously used the step-all command you must select" + K.lineSeparator + "first a solution with step command before executing steps of rewrites!");
						}
					}
					if (cmd.hasOption("step-all")) {
						// by default compute all successors in one transition
						String arg = new String("1");
						String[] remainingArguments = null;
						remainingArguments = cmd.getArgs();
						if (remainingArguments.length > 0) {
							arg = remainingArguments[0];
						}
						// get the maudified version of the current configuration based on the xml obtained from -xml-log option
						String maudeConfig = XmlUtil.xmlToMaude(K.maude_output);
						// System.out.println("config=" + maudeConfig);
						//check first to see if we have a current configuration obtained at previous steps
						if (maudeConfig != null && maudeConfig.length() > 0) {
							maudeCmd = "set show command off ." + K.lineSeparator + "load " + KPaths.windowfyPath(compiledFile) + K.lineSeparator + "search[," + arg + "] " + maudeConfig + "=>+ B:Bag .";
							maudeCmd += K.lineSeparator + "show search graph" + " .";
							// System.out.println("maude cmd=" + maudeCmd);
							rp.runMaude(maudeCmd, outFile.getCanonicalPath(), errFile.getCanonicalPath());
							// check whether Maude produced errors
							rp.checkMaudeForErrors(errFile, lang);
	
							// pretty-print the obtained search results
							K.maude_cmd = "search";
							p = new PrettyPrintOutput();
							p.preprocessDoc(K.maude_output, K.processed_maude_output);
							red = p.processDoc(K.processed_maude_output);
							for (String result : red) {
								AnsiConsole.out.println(result);
							}
						}
						else {
							Error.silentReport("Wrong command: If you previously used the step-all command you must select" + K.lineSeparator + "first a solution with step command before executing steps of rewrites!");
						}
					}
					if (cmd.hasOption("select")) {
						int arg = 0;
						try {
							arg = Integer.parseInt(cmd.getOptionValue("select").trim());
						}
						catch (NumberFormatException e) {
							System.out.println("You must provide an integer argument for the select command");
							continue;
						}
						Element elem = XmlUtil.getSearchSolution(K.maude_output, arg);
						if (elem != null) {
							String s = XmlUtil.printSearchSolution(K.processed_maude_output, arg);
							System.out.println("Selected solution is:" + s);
							
							Document doc = XmlUtil.createXmlRewriteForm(elem);
							
					        //delete the content of the xml file
							FileOutputStream writer = new FileOutputStream(K.maude_output);
							writer.write((new String()).getBytes());
							writer.close();
							
							//place the corresponding content in the xml file according to the selected solution
							XmlUtil.serializeXML(doc, K.maude_output);
							
							K.maude_cmd = "erewrite";
							
						}
						else {
							System.out.println("A solution with the specified solution-number could not be found in the" + K.lineSeparator + "previous search result");
						}
					}
					if (cmd.hasOption("show-search-graph")) {
						System.out.println(K.lineSeparator + "The search graph is:" + K.lineSeparator);
						String result = p.printSearchGraph(K.processed_maude_output);
						System.out.println(result);
					}
					if (cmd.hasOption("show-node")) {
						String nodeId = cmd.getOptionValue("show-node").trim();
						String result = p.printNodeSearchGraph(K.processed_maude_output, nodeId);
						if (result != null) {
							System.out.println(result);
						}
						else {
							System.out.println("A node with the specified id couldn't be found in the search graph");
						}
						
					}
					/*if (cmd.hasOption("show path labels")) {
						String arg = cmd.getOptionValue("show path labels");
						maudeCmd = "set show command off ." + K.lineSeparator + "load " + KPaths.windowfyPath(compiledFile) + K.lineSeparator + "show path labels " + arg + " .";
						// System.out.println("maude cmd=" + maudeCmd);
						rp.runMaude(maudeCmd, outFile.getCanonicalPath(), errFile.getCanonicalPath());
						// check whether Maude produced errors
						rp.checkMaudeForErrors(errFile, lang);
					}*/
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * @param cmds
	 *            represents the arguments/options given to jkrun command..
	 */
	public static void execute_Krun(String cmds[]) {
		// delete temporary krun directory
		FileUtil.deleteDirectory(new File(K.krunDir));
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					FileUtil.renameFolder(K.krunTempDir, K.krunDir);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});


		CommandlineOptions cmd_options = new CommandlineOptions();
		CommandLine cmd = cmd_options.parse(cmds);
		try {

			// Parse the program arguments

			if (cmd.hasOption("search")) {
				K.maude_cmd = "search";
				K.io = false;
				K.do_search = true;
				K.output_mode = "pretty";
			}
			if (cmd.hasOption("config")) {
				K.output_mode = "pretty";
			}
			if (cmd.hasOption("no-config")) {
				K.output_mode = "none";
			}
			if (cmd.hasOption('h') || cmd.hasOption('?')) {
				K.help = true;
			}
			if (cmd.hasOption('v')) {
				K.version = true;
			}
			if (cmd.hasOption("k-definition")) {
				K.k_definition = new File(cmd.getOptionValue("k-definition")).getCanonicalPath();
				K.kdir = new File(K.k_definition).getParent() + K.fileSeparator + ".k";
			}
			if (cmd.hasOption("main-module")) {
				K.main_module = cmd.getOptionValue("main-module");
			}
			if (cmd.hasOption("syntax-module")) {
				K.syntax_module = cmd.getOptionValue("syntax-module");
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
			// k-definition beats compiled-def in a fight
			if (cmd.hasOption("compiled-def") && !cmd.hasOption("k-definition")) {
				K.compiled_def = new File(cmd.getOptionValue("compiled-def")).getCanonicalPath();
				K.kdir = new File(K.compiled_def).getParent() + K.fileSeparator + ".k";
			}
			if (cmd.hasOption("do-search")) {
				K.do_search = true;
			}
			if (cmd.hasOption("no-do-search")) {
				K.do_search = false;
			}
			if (cmd.hasOption("maude-cmd")) {
				K.maude_cmd = cmd.getOptionValue("maude-cmd");
			}
			/*
			 * if (cmd.hasOption("xsearch-pattern")) { K.maude_cmd = "search"; K.do_search = true; K.xsearch_pattern = cmd.getOptionValue("xsearch-pattern"); // System.out.println("xsearch-pattern:" + K.xsearch_pattern); }
			 */
			if (cmd.hasOption("pattern")) {
				K.pattern = cmd.getOptionValue("pattern");
			}
			if (cmd.hasOption("bound")) {
				K.bound = cmd.getOptionValue("bound");
			}
			if (cmd.hasOption("depth")) {
				K.depth = cmd.getOptionValue("depth");
			}
			if (cmd.hasOption("graph")) {
				K.showSearchGraph = true;
			}
			if (cmd.hasOption("output-mode")) {
				K.output_mode = cmd.getOptionValue("output-mode");
			}
			if (cmd.hasOption("log-io")) {
				K.log_io = true;
			}
			if (cmd.hasOption("no-log-io")) {
				K.log_io = false;
			}
			if (cmd.hasOption("debug")) {
				K.debug = true;
			}
			if (cmd.hasOption("trace")) {
				K.trace = true;
			}
			if (cmd.hasOption("pgm")) {
				K.pgm = new File(cmd.getOptionValue("pgm")).getCanonicalPath();
			}
			if (cmd.hasOption("ltlmc")) {
				K.model_checking = cmd.getOptionValue("ltlmc");
			}
			if (cmd.hasOption("deleteTempDir")) {
				K.deleteTempDir = true;
			}
			if (cmd.hasOption("no-deleteTempDir")) {
				K.deleteTempDir = false;
			}
			if (cmd.hasOption("output")) {
				K.output = new File(cmd.getOptionValue("output")).getCanonicalPath();
			}
			if (cmd.hasOption("c")) {
				K.configuration_variables = cmd.getOptionProperties("c");
			}

			// printing the output according to the given options
			if (K.help) {
				printKRunUsage(cmd_options.getOptions());
				System.exit(0);
			}
			if (K.version) {
				printVersion();
				System.exit(0);
			}
			if (K.deleteTempDir) {
				File[] folders = FileUtil.searchSubFolders(K.kdir, "krun\\d+");
				if (folders != null && folders.length > 0) {
					for (int i = 0; i < folders.length; i++) {
						FileUtil.deleteDirectory(folders[i]);
					}
				}
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
			String lang = FileUtil.getExtension(K.pgm, ".", K.fileSeparator);

			// by default
			if (!cmd.hasOption("k-definition")) {
				K.k_definition = new File(K.userdir).getCanonicalPath() + K.fileSeparator + lang;
			}

			initOptions(K.userdir);

			if (K.compiled_def == null) {
				resolveOption("compiled-def", cmd);
			}
			if (!cmd.hasOption("main-module")) {
				resolveOption("main-module", cmd);
			}
			if (!cmd.hasOption("syntax-module")) {
				resolveOption("syntax-module", cmd);
			}

			if (!K.k_definition.endsWith(".k")) {
				K.k_definition = K.k_definition + ".k";
			}

			if (K.compiled_def == null) {
				Error.report("\nCould not find a compiled K definition.");
			}
			File compiledFile = new File(K.compiled_def);
			if (!compiledFile.exists()) {
				Error.report("\nCould not find compiled definition: " + K.compiled_def + "\nPlease compile the definition by using `kompile'.");
			}

			/*
			 * System.out.println("K.k_definition=" + K.k_definition); System.out.println("K.syntax_module=" + K.syntax_module); System.out.println("K.main_module=" + K.main_module); System.out.println("K.compiled_def=" + K.compiled_def);
			 */

			// in KAST variable we obtain the output from running kast process on a program defined in K
			String KAST = new String();
			RunProcess rp = new RunProcess();

			KAST = rp.runParser(K.parser, K.pgm, true);

			if (!K.debug) {
				normalExecution(KAST, lang, rp, cmd_options);
			} else {
				debugExecution(KAST, lang);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			System.out.println("You provided bad program arguments!");
			e.printStackTrace();
			printKRunUsage(cmd_options.getOptions());
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		
		execute_Krun(args);
		
	}

}

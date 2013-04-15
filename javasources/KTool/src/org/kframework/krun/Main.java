package org.kframework.krun;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jline.ArgumentCompletor;
import jline.Completor;
import jline.ConsoleReader;
import jline.FileNameCompletor;
import jline.MultiCompletor;
import jline.SimpleCompletor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.fusesource.jansi.AnsiConsole;
import org.kframework.backend.java.symbolic.JavaSymbolicKRun;
import org.kframework.compile.ConfigurationCleaner;
import org.kframework.compile.FlattenModules;
import org.kframework.compile.transformers.AddTopCellConfig;
import org.kframework.compile.transformers.FlattenSyntax;
import org.kframework.compile.utils.MetaK;
import org.kframework.compile.utils.CompilerStepDone;
import org.kframework.compile.utils.RuleCompilerSteps;
import org.kframework.kil.ASTNode;
import org.kframework.kil.BackendTerm;
import org.kframework.kil.Configuration;
import org.kframework.kil.Empty;
import org.kframework.kil.Rule;
import org.kframework.kil.Term;
import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.krun.api.KRun;
import org.kframework.krun.api.KRunApiDebugger;
import org.kframework.krun.api.KRunDebugger;
import org.kframework.krun.api.KRunResult;
import org.kframework.krun.api.KRunState;
import org.kframework.krun.api.MaudeKRun;
import org.kframework.krun.api.SearchResults;
import org.kframework.krun.api.SearchType;
import org.kframework.krun.api.Transition;
import org.kframework.krun.gui.Controller.RunKRunCommand;
import org.kframework.krun.gui.UIDesign.MainWindow;
import org.kframework.parser.concrete.disambiguate.CollectVariablesVisitor;
import org.kframework.utils.BinaryLoader;
import org.kframework.utils.DefinitionLoader;
import org.kframework.utils.Stopwatch;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.errorsystem.KException.ExceptionType;
import org.kframework.utils.errorsystem.KException.KExceptionGroup;
import org.kframework.utils.general.GlobalSettings;
import edu.uci.ics.jung.graph.*;

public class Main {

	private static final String USAGE_KRUN = "krun [options] <file>"
			+ K.lineSeparator;
	private static final String USAGE_DEBUG = "Enter one of the following commands without \"--\" in front. "
			+ K.lineSeparator
			+ "For autocompletion press TAB key and for accessing the command"
			+ K.lineSeparator
			+ "history use up and down arrows."
			+ K.lineSeparator;
	private static final String HEADER = "";
	private static final String FOOTER = "";
	private static Stopwatch sw = new Stopwatch();

	// needed for displaying the krun help
	public static void printKRunUsage(Options options) {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter
				.setOptionComparator(new CommandlineOptions.OptionComparator());
		helpFormatter.setWidth(79);
		helpFormatter.printHelp(USAGE_KRUN, HEADER, options, FOOTER);
		System.out.println();
	}

	// needed for displaying the krun debugger help
	public static void printDebugUsage(Options options) {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter
				.setOptionComparator(new CommandlineOptions.OptionComparator());
		helpFormatter.setWidth(79);
		helpFormatter.printHelp(USAGE_DEBUG, HEADER, options, FOOTER);
		System.out.println();
	}

	public static void printVersion() {
		System.out.println("JKrun 0.2.0\n"
				+ "Copyright (C) 2012 Necula Emilian & Raluca");
	}

	// find the maude compiled definitions on the disk
	public static String initOptions(String path) {
		String result = null;
		// String path_ = null;
		StringBuilder str = new StringBuilder();
		int count = 0;

		try {
			File[] maudeFiles = FileUtil.searchSubFolders(path, ".*-kompiled");
			for (File maudeFile : maudeFiles) {
				String fullPath = maudeFile.getCanonicalPath();
				if (fullPath.endsWith("-kompiled")) {
					result = fullPath;
					str.append("\"./" + maudeFile.getName() + "\" ");
					count++;
				}
			}
			if (count > 1) {
				Error.report("Multiple compiled definitions found. Please specify one of: "
						+ str.toString() + "with --compiled-def");
			} else if (count == 1) {
				return result;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	// set the main-module, syntax-module and k-definition according to their
	// correlation with compiled-def
	public static void resolveOption(String optionName, CommandLine cmd) {
		String s;
		if (K.k_definition != null) {
			s = FileUtil.dropKExtension(K.k_definition, ".", K.fileSeparator);
		} else {
			// using --compiled-def
			if (K.compiled_def != null && K.compiled_def.endsWith("-kompiled")) {
				s = K.compiled_def.substring(0,
						K.compiled_def.lastIndexOf("-kompiled"));
			} else {
				s = null;
			}
		}

		int index;

		if (optionName == "compiled-def") {
			if (cmd.hasOption("k-definition")) {
				K.compiled_def = s + "-kompiled";
			} else {
				K.compiled_def = initOptions(K.userdir);
				if (K.compiled_def != null) {
					index = K.compiled_def.indexOf("-kompiled");
					K.k_definition = K.compiled_def.substring(0, index);
				}
			}
		} else if (optionName == "main-module") {
			K.main_module = K.definition.getMainModule();
		} else if (optionName == "syntax-module") {
			K.syntax_module = K.definition.getMainSyntaxModule();
		}
	}

	private static Term parseTerm(String value) throws Exception {
		org.kframework.parser.concrete.KParser.ImportTblGround(K.compiled_def
				+ "/ground/Concrete.tbl");
		ASTNode term = org.kframework.utils.DefinitionLoader.parseCmdString(
				value, "", "Command line argument");
		return (Term) term.accept(new FlattenSyntax());
	}

	public static Term plug(Map<String, Term> args) throws TransformerException {
		Configuration cfg = K.kompiled_cfg;
		ASTNode cfgCleanedNode = null;
		try {
			cfgCleanedNode = new ConfigurationCleaner().transform(cfg);
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Term cfgCleaned;
		if (cfgCleanedNode == null) {
			cfgCleaned = new Empty(MetaK.Constants.Bag);
		} else {
			if (!(cfgCleanedNode instanceof Configuration)) {
				GlobalSettings.kem.register(new KException(ExceptionType.ERROR,
						KExceptionGroup.INTERNAL,
						"Configuration Cleaner failed.", cfg.getFilename(), cfg
								.getLocation()));
			}
			cfgCleaned = ((Configuration) cfgCleanedNode).getBody();
		}

		if (GlobalSettings.verbose)
			sw.printIntermediate("Plug configuration variables");

		return (Term) cfgCleaned.accept(new SubstitutionFilter(args));
	}

	public static Term makeConfiguration(Term kast, String stdin,
			RunProcess rp, boolean hasTerm) throws TransformerException {

		if (hasTerm) {
			if (kast == null) {
				return rp.runParserOrDie(K.parser, K.term, false, null);
			} else {
				Error.report("You cannot specify both the term and the configuration variables.");
			}
		}

		HashMap<String, Term> output = new HashMap<String, Term>();
		boolean hasPGM = false;
		Enumeration<Object> en = K.configuration_variables.keys();
		while (en.hasMoreElements()) {
			String name = (String) en.nextElement();
			String value = K.configuration_variables.getProperty(name);
			String parser = K.cfg_parsers.getProperty(name);
			// TODO: get sort from configuration term in definition and pass it
			// here
			Term parsed = null;
			if (parser == null) {
				try {
					parsed = parseTerm(value);
				} catch (Exception e1) {
					e1.printStackTrace();
					Error.report(e1.getMessage());
				}
			} else {
				parsed = rp.runParserOrDie(parser, value, true, null);
			}
			output.put("$" + name, parsed);
			hasPGM = hasPGM || name.equals("$PGM");
		}
		if (!hasPGM && kast != null) {
			output.put("$PGM", kast);
		}
		if (!K.io && stdin == null) {
			stdin = "";
		}
		if (stdin != null) {
			output.put("$noIO", new BackendTerm("List", "#noIO"));
			output.put("$stdin", new BackendTerm("K", "# \"" + stdin
					+ "\\n\"(.KList)"));
		} else {
			output.put("$noIO", new Empty("List"));
			output.put("$stdin", new Empty("K"));
		}

		if (GlobalSettings.verbose)
			sw.printIntermediate("Make configuration");

		return plug(output);
	}

	// execute krun in normal mode (i.e. not in debug mode)
	public static void normalExecution(Term KAST, String lang, RunProcess rp,
			CommandlineOptions cmd_options) {
		try {
			CommandLine cmd = cmd_options.getCommandLine();

			KRun krun = null;
			if (K.backend.equals("maude")) {
				krun = new MaudeKRun();
			} else if (K.backend.equals("java-symbolic")) {
				krun = new JavaSymbolicKRun();
			} else {
				Error.report("Currently supported backends are 'maude' and 'java-symbolic'");
			}
			KRunResult<?> result = null;
			Set<String> varNames = null;
			Rule patternRule = null;
			try {
				if (cmd.hasOption("pattern") || "search".equals(K.maude_cmd)) {
					org.kframework.parser.concrete.KParser
							.ImportTbl(K.compiled_def + "/def/Concrete.tbl");
					ASTNode pattern = DefinitionLoader.parsePattern(K.pattern,
							"Command line pattern");
					CollectVariablesVisitor vars = new CollectVariablesVisitor();
					pattern.accept(vars);
					varNames = vars.getVars().keySet();
					try {
						pattern = new RuleCompilerSteps(K.definition).compile(
								(Rule) pattern, null);
					} catch (CompilerStepDone e) {
						pattern = (ASTNode) e.getResult();
					}
					patternRule = (Rule) pattern;
					if (GlobalSettings.verbose)
						sw.printIntermediate("Parsing search pattern");
				}

				if (K.do_search) {
					if ("search".equals(K.maude_cmd)) {
						BufferedReader br = new BufferedReader(
								new InputStreamReader(System.in));
						String buffer = "";
						// detect if the input comes from console or redirected
						// from a pipeline
						Console c = System.console();
						if (c == null && br.ready()) {
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
						Integer depth = null;
						Integer bound = null;
						if (cmd.hasOption("bound") && cmd.hasOption("depth")) {
							bound = Integer.parseInt(K.bound);
							depth = Integer.parseInt(K.depth);
						} else if (cmd.hasOption("bound")) {
							bound = Integer.parseInt(K.bound);
						} else if (cmd.hasOption("depth")) {
							depth = Integer.parseInt(K.depth);
						}
						result = krun.search(
								bound,
								depth,
								K.searchType,
								patternRule,
								makeConfiguration(KAST, buffer, rp,
										(K.term != null)), varNames);

						if (GlobalSettings.verbose)
							sw.printTotal("Search total");
					} else {
						Error.report("For the search option you need to specify that --maude-cmd=search");
					}
				} else if (K.model_checking.length() > 0) {
					// run kast for the formula to be verified
					File formulaFile = new File(K.model_checking);
					Term KAST1 = null;
					if (!formulaFile.exists()) {
						// Error.silentReport("\nThe specified argument does not exist as a file on the disc; it may represent a direct formula: "
						// + K.model_checking);
						// assume that the specified argument is not a file and
						// maybe represents a formula
						KAST1 = rp.runParserOrDie(K.parser, K.model_checking, true,
								"LTLFormula");
					} else {
						// the specified argument represents a file
						KAST1 = rp.runParserOrDie(K.parser, K.model_checking, false,
								"LTLFormula");
					}

					result = krun
							.modelCheck(
									KAST1,
									makeConfiguration(KAST, null, rp,
											(K.term != null)));

					if (GlobalSettings.verbose)
						sw.printTotal("Model checking total");
				} else {
					result = krun.run(makeConfiguration(KAST, null, rp,
							(K.term != null)));

					if (GlobalSettings.verbose)
						sw.printTotal("Normal execution total");
				}

				if (cmd.hasOption("pattern") && !K.do_search) {
					Object krs = result.getResult();
					if (krs instanceof KRunState) {
						Term res = ((KRunState) krs).getRawResult();
						result = krun.search(null, null, K.searchType,
								patternRule, res, varNames);
					}else {
						Error.report("Pattern matching after execution is not supported by search and model checking");
					}
				}

			} catch (KRunExecutionException e) {
				rp.printError(e.getMessage(), lang);
				System.exit(1);
			} catch (TransformerException e) {
				e.report();
			}

			if ("pretty".equals(K.output_mode)) {
				String output = result.toString();
				if (!cmd.hasOption("output")) {
					AnsiConsole.out.println(output);
				} else {
					FileUtil.createFile(K.output, output);
				}
				// print search graph
				if ("search".equals(K.maude_cmd) && K.do_search
						&& K.showSearchGraph) {
					System.out.println(K.lineSeparator + "The search graph is:"
							+ K.lineSeparator);
					@SuppressWarnings("unchecked")
					KRunResult<SearchResults> searchResult = (KRunResult<SearchResults>) result;
					AnsiConsole.out
							.println(searchResult.getResult().getGraph());
					// offer the user the possibility to turn execution into
					// debug mode
					while (true) {
						System.out.print(K.lineSeparator
								+ "Do you want to enter in debug mode? (y/n):");
						BufferedReader stdin = new BufferedReader(
								new InputStreamReader(System.in));
						String input = stdin.readLine();
						if (input == null) {
							K.debug = false;
							K.guidebug = false;
							System.out.println();
							break;
						}
						if (input.equals("y")) {
							K.debug = true;
							debugExecution(KAST, lang, searchResult);
						} else if (input.equals("n")) {
							K.debug = false;
							K.guidebug = false;
							break;
						} else {
							System.out
									.println("You should specify one of the possible answers:y or n");
						}
					}
				}
			} else if ("raw".equals(K.output_mode)) {
				String output = result.getRawOutput();
				;
				if (!cmd.hasOption("output")) {
					System.out.println(output);
				} else {
					FileUtil.createFile(K.output, output);
				}
			} else if ("none".equals(K.output_mode)) {
				System.out.print("");
			} else if ("binary".equals(K.output_mode)) {
				Object krs = result.getResult();
				if (krs instanceof KRunState) {
					Term res = ((KRunState) krs).getRawResult();

					if (!cmd.hasOption("output")) {
						Error.silentReport("Did not specify an output file. Cannot print output-mode binary to standard out. Saving to .k/krun/krun_output");
						BinaryLoader.toBinary(res, new FileOutputStream(
								K.krun_output));
					} else {
						BinaryLoader.toBinary(res, new FileOutputStream(
								K.output));
					}
				} else {
					Error.report("binary output mode is not supported by search and model checking");
				}

			} else {
				Error.report(K.output_mode
						+ " is not a valid value for output-mode option");
			}

			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// execute krun in debug mode (i.e. step by step execution)
	// isSwitch variable is true if we enter in debug execution from normal
	// execution (we use the search command with --graph option)
	@SuppressWarnings("unchecked")
	public static void debugExecution(Term kast, String lang,
			KRunResult<SearchResults> state) {
		try {
			// adding autocompletion and history feature to the stepper internal
			// commandline by using the JLine library
			ConsoleReader reader = new ConsoleReader();
			reader.setBellEnabled(false);

			List<Completor> argCompletor = new LinkedList<Completor>();
			argCompletor.add(new SimpleCompletor(new String[] { "help", 
					"abort", "resume", "step", "step-all", "select", 
					"show-search-graph", "show-node", "show-edge", "save", "load" }));
			argCompletor.add(new FileNameCompletor());
			List<Completor> completors = new LinkedList<Completor>();
			completors.add(new ArgumentCompletor(argCompletor));
			reader.addCompletor(new MultiCompletor(completors));

			new File(K.compiled_def + K.fileSeparator + "main.maude")
					.getCanonicalPath();
			RunProcess rp = new RunProcess();
			KRun krun = new MaudeKRun();
			KRunDebugger debugger;
			if (state == null) {
				Term t = makeConfiguration(kast, null, rp, (K.term != null));
				debugger = krun.debug(t);
				System.out
						.println("After running one step of execution the result is:");
				AnsiConsole.out.println(debugger.printState(debugger
						.getCurrentState()));
			} else {
				debugger = krun.debug(state.getResult());
			}

			while (true) {
				System.out.println();
				String input = reader.readLine("Command > ");
				if (input == null) {
					// probably pressed ^D
					System.out.println();
					System.exit(0);
				}

				// construct the right command line input when we specify the
				// "step" option with an argument (i.e. step=3)
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
						if (i > 0) {
							aux.append("=");
							aux.append(items.get(i));
						} else if (i == 0) {
							aux.append(items.get(i));
						}
					}
					input = aux.toString();
				}
				// apply trim to remove possible blank spaces from the inserted
				// command
				String[] cmds = new String[] { "--" + input };
				CommandlineOptions cmd_options = new CommandlineOptions();
				CommandLine cmd = cmd_options.parse(cmds);
				// when an error occurred during parsing the commandline
				// continue the execution of the stepper
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
						try {
							debugger.resume();
							AnsiConsole.out.println(debugger
									.printState(debugger.getCurrentState()));
						} catch (IllegalStateException e) {
							Error.silentReport("Wrong command: If you previously used the step-all command you must select"
									+ K.lineSeparator
									+ "first a solution with step command before executing steps of rewrites!");
						}
					}
					// one step execution (by default) or more if you specify an
					// argument
					if (cmd.hasOption("step")) {
						// by default execute only one step at a time
						String arg = new String("1");
						String[] remainingArguments = null;
						remainingArguments = cmd.getArgs();
						if (remainingArguments.length > 0) {
							arg = remainingArguments[0];
						}
						try {
							int steps = Integer.parseInt(arg);
							debugger.step(steps);
							AnsiConsole.out.println(debugger
									.printState(debugger.getCurrentState()));
						} catch (NumberFormatException e) {
							Error.silentReport("Argument to step must be an integer.");
						} catch (IllegalStateException e) {
							Error.silentReport("Wrong command: If you previously used the step-all command you must select"
									+ K.lineSeparator
									+ "first a solution with step command before executing steps of rewrites!");
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
						try {
							int steps = Integer.parseInt(arg);
							SearchResults states = debugger.stepAll(steps);
							AnsiConsole.out.println(states);

						} catch (NumberFormatException e) {
							Error.silentReport("Argument to step-all must be an integer.");
						} catch (IllegalStateException e) {
							Error.silentReport("Wrong command: If you previously used the step-all command you must select"
									+ K.lineSeparator
									+ "first a solution with step command before executing steps of rewrites!");
						}
					}
					// "select n7" or "select 3" are both valid commands
					if (cmd.hasOption("select")) {
						String arg = new String();
						arg = cmd.getOptionValue("select").trim();
						try {
							int stateNum = Integer.parseInt(arg);
							debugger.setCurrentState(stateNum);
							AnsiConsole.out.println(debugger
									.printState(debugger.getCurrentState()));
						} catch (NumberFormatException e) {
							Error.silentReport("Argument to select must bean integer.");
						} catch (IllegalArgumentException e) {
							System.out
									.println("A node with the specified state number could not be found in the"
											+ K.lineSeparator + "search graph");
						}
					}
					if (cmd.hasOption("show-search-graph")) {
						System.out.println(K.lineSeparator
								+ "The search graph is:" + K.lineSeparator);
						System.out.println(debugger.getGraph());
					}
					if (cmd.hasOption("show-node")) {
						String nodeId = cmd.getOptionValue("show-node").trim();
						try {
							int stateNum = Integer.parseInt(nodeId);
							AnsiConsole.out.println(debugger
									.printState(stateNum));
						} catch (NumberFormatException e) {
							Error.silentReport("Argument to select node to show must be an integer.");
						} catch (IllegalArgumentException e) {
							System.out
									.println("A node with the specified state number could not be found in the"
											+ K.lineSeparator + "search graph");
						}
					}
					if (cmd.hasOption("show-edge")) {
						String[] vals = cmd.getOptionValues("show-edge");
						vals = vals[0].split("=", 2);
						try {
							int state1 = Integer.parseInt(vals[0].trim());
							int state2 = Integer.parseInt(vals[1].trim());
							AnsiConsole.out.println(debugger.printEdge(state1,
									state2));
						} catch (ArrayIndexOutOfBoundsException e) {
							Error.silentReport("Must specify two nodes with an edge between them.");
						} catch (NumberFormatException e) {
							Error.silentReport("Arguments to select edge to show must be integers.");
						} catch (IllegalArgumentException e) {
							System.out
									.println("An edge with the specified endpoints could not be found in the"
											+ K.lineSeparator + "search graph");
						}
					}

					DirectedGraph<KRunState, Transition> savedGraph = null;
					if(cmd.hasOption("save")) {
						BinaryLoader.toBinary(debugger.getGraph(), new FileOutputStream(new File(cmd.getOptionValue("save")).getCanonicalPath()));
						System.out.println("File successfully saved.");
					} 
					if (cmd.hasOption("load")) {
						try {
							savedGraph = (DirectedGraph<KRunState, Transition>) BinaryLoader.fromBinary(new FileInputStream(cmd.getOptionValue("load")));
							krun = new MaudeKRun();
							debugger = new KRunApiDebugger(krun, savedGraph);
							debugger.setCurrentState(1);
							System.out.println("File successfully loaded.");
						} catch (FileNotFoundException e) {
							System.out.println("There is no such file, please try again.");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void guiDebugExecution(Term kast, String lang,
			KRunResult<SearchResults> state) {

		try {
			new MainWindow(new RunKRunCommand(kast, lang, false));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

		// set verbose
		if (cmd.hasOption("verbose")) {
			GlobalSettings.verbose = true;
		}

		if (GlobalSettings.verbose)
			sw.printIntermediate("Deleting temporary krun directory");

		try {

			// Parse the program arguments

			if (cmd.hasOption("search") || cmd.hasOption("do-search")
					|| cmd.hasOption("search-final")
					|| cmd.hasOption("search-all")
					|| cmd.hasOption("search-one-step")
					|| cmd.hasOption("search-one-or-more-steps")) {
				K.maude_cmd = "search";
				K.io = false;
				K.do_search = true;
				if (cmd.hasOption("search") && cmd.hasOption("depth")) {
					K.searchType = SearchType.STAR;
				}
			}
			if (cmd.hasOption("search-final")) {
				K.searchType = SearchType.FINAL;
			}
			if (cmd.hasOption("search-all")) {
				K.searchType = SearchType.STAR;
			}
			if (cmd.hasOption("search-one-step")) {
				K.searchType = SearchType.ONE;
			}
			if (cmd.hasOption("search-one-or-more-steps")) {
				K.searchType = SearchType.PLUS;
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
				K.k_definition = new File(cmd.getOptionValue("k-definition"))
						.getCanonicalPath();
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
			if (cmd.hasOption("term")) {
				K.term = cmd.getOptionValue("term");
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
				K.compiled_def = new File(cmd.getOptionValue("compiled-def"))
						.getCanonicalPath();
			}
			if (cmd.hasOption("maude-cmd")) {
				K.maude_cmd = cmd.getOptionValue("maude-cmd");
			}
			/*
			 * if (cmd.hasOption("xsearch-pattern")) { K.maude_cmd = "search";
			 * K.do_search = true; K.xsearch_pattern =
			 * cmd.getOptionValue("xsearch-pattern"); //
			 * System.out.println("xsearch-pattern:" + K.xsearch_pattern); }
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
			if (cmd.hasOption("debug-gui")) {
				K.guidebug = true;
			}
			if (cmd.hasOption("trace")) {
				K.trace = true;
			}
			if (cmd.hasOption("profile")) {
				K.profile = true;
			}
			if (cmd.hasOption("pgm")) {
				K.pgm = cmd.getOptionValue("pgm");
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
				if (!cmd.hasOption("color")) {
					K.color = false;
				}
				K.output = new File(cmd.getOptionValue("output"))
						.getCanonicalPath();
			}
			if (cmd.hasOption("c")) {

				if (K.term != null) {
					Error.report("You cannot specify both the term and the configuration variables.");
				}

				K.configuration_variables = cmd.getOptionProperties("c");
				String parser = null;
				for (Option opt : cmd.getOptions()) {
					if (opt.equals(cmd_options.getOptions().getOption("c"))
							&& parser != null) {
						K.cfg_parsers.setProperty(opt.getValue(0), parser);
					}
					if (opt.equals(cmd_options.getOptions().getOption(
							"cfg-parser"))) {
						parser = opt.getValue();
					}
				}
			}
			if (cmd.hasOption("backend")) {
				K.backend = cmd.getOptionValue("backend");
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
				programArg = remainingArguments[0];
				K.pgm = programArg;
			}
			String lang = null;
			if (K.pgm != null) {
				File pgmFile = new File(K.pgm);
				if (pgmFile.exists()) {
					lang = FileUtil.getExtension(K.pgm, ".", K.fileSeparator);
				}
			}

			if (GlobalSettings.verbose)
				sw.printIntermediate("Parsing command line options");

			// by default
			if (!cmd.hasOption("k-definition")
					&& !cmd.hasOption("compiled-def") && lang != null) {
				K.k_definition = new File(K.userdir).getCanonicalPath()
						+ K.fileSeparator + lang;
			}

			if (K.compiled_def == null) {
				resolveOption("compiled-def", cmd);
			}

			if (K.k_definition != null && !K.k_definition.endsWith(".k")) {
				K.k_definition = K.k_definition + ".k";
			}

			if (K.compiled_def == null) {
				Error.report("Could not find a compiled K definition. Please ensure that either a compiled K definition exists in the current directory with its default name, or that --k-definition or --compiled-def have been specified.");
			}
			File compiledFile = new File(K.compiled_def);
			if (!compiledFile.exists()) {
				Error.report("\nCould not find compiled definition: "
						+ K.compiled_def
						+ "\nPlease compile the definition by using `kompile'.");
			}

			DefinitionHelper.dotk = new File(
					new File(K.compiled_def).getParent() + File.separator
							+ ".k");
			if (!DefinitionHelper.dotk.exists()) {
				DefinitionHelper.dotk.mkdirs();
			}
			DefinitionHelper.kompiled = new File(K.compiled_def);
			K.kdir = DefinitionHelper.dotk.getCanonicalPath();
			K.setKDir();
			/*
			 * System.out.println("K.k_definition=" + K.k_definition);
			 * System.out.println("K.syntax_module=" + K.syntax_module);
			 * System.out.println("K.main_module=" + K.main_module);
			 * System.out.println("K.compiled_def=" + K.compiled_def);
			 */

			if (GlobalSettings.verbose)
				sw.printIntermediate("Checking compiled definition");

			// in KAST variable we obtain the output from running kast process
			// on a program defined in K
			Term KAST = null;
			RunProcess rp = new RunProcess();

			if (!DefinitionHelper.initialized) {
				org.kframework.kil.Definition javaDef = (org.kframework.kil.Definition) BinaryLoader
						.fromBinary(new FileInputStream(K.compiled_def
								+ "/defx.bin"));

				if (GlobalSettings.verbose)
					sw.printIntermediate("Reading definition from binary");

				// This is essential for generating maude
				javaDef = new FlattenModules().compile(javaDef, null);

				if (GlobalSettings.verbose)
					sw.printIntermediate("Flattening modules");

				try {
					javaDef = (org.kframework.kil.Definition) javaDef
							.accept(new AddTopCellConfig());
				} catch (TransformerException e) {
					e.report();
				}

				if (GlobalSettings.verbose)
					sw.printIntermediate("Adding top cell to configuration");

				javaDef.preprocess();

				if (GlobalSettings.verbose)
					sw.printIntermediate("Preprocessing definition");

				K.definition = javaDef;

				if (GlobalSettings.verbose)
					sw.printIntermediate("Importing tables");

				org.kframework.kil.Configuration configKompiled = (org.kframework.kil.Configuration) BinaryLoader
						.fromBinary(new FileInputStream(K.compiled_def
								+ "/configuration.bin"));
				K.kompiled_cfg = configKompiled;

				if (GlobalSettings.verbose)
					sw.printIntermediate("Reading configuration from binary");
			}

			if (!cmd.hasOption("main-module")) {
				resolveOption("main-module", cmd);
			}
			if (!cmd.hasOption("syntax-module")) {
				resolveOption("syntax-module", cmd);
			}

			if (GlobalSettings.verbose)
				sw.printIntermediate("Resolving main and syntax modules");

			if (K.pgm != null) {
				KAST = rp.runParserOrDie(K.parser, K.pgm, false, null);
			} else {
				KAST = null;
			}

			if (GlobalSettings.verbose)
				sw.printIntermediate("Kast process");

			if (K.term != null) {
				if (K.parser.equals("kast")) {
					if (K.backend.equals("java-symbolic")) {
						GlobalSettings.whatParser = GlobalSettings.ParserType.RULES;
					} else {
						GlobalSettings.whatParser = GlobalSettings.ParserType.GROUND;
					}
				}
			}

			GlobalSettings.kem.print();

			if (!K.debug && !K.guidebug) {
				normalExecution(KAST, lang, rp, cmd_options);
			} else {
				if (K.do_search) {
					Error.report("Cannot specify --search with --debug. In order to search inside the debugger, use the step-all command.");
				}
				if (K.guidebug)
					guiDebugExecution(KAST, lang, null);
				else
					debugExecution(KAST, lang, null);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		execute_Krun(args);

	}

}

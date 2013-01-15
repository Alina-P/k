package org.kframework.main;

import org.apache.commons.cli.*;

public class KompileOptionsParser {
	Options options;
	HelpFormatter help;

	public KompileOptionsParser() {
		options = new Options();
		help = new HelpFormatter();

		// main options
		OptionGroup main = new OptionGroup();
		Option def = new Option("def", "definition", true, "main file to kompile");
		Option step = new Option("step", "step", true, "name of the compilation phase after which compilation should stop.");

		// verbose and help
		OptionGroup verb = new OptionGroup();
		Option help = new Option("h", "help", false, "prints this message and exits");
		Option version = new Option("version", false, "prints version number");
		Option verbose = new Option("v", "verbose", false, "verbose mode");
//		Option lint = new Option("lint", "lint", false, "Checks your definition for possible logical errors");
		verb.addOption(help);
		verb.addOption(version);
		verb.addOption(verbose);
//		verb.addOption(lint);

		// Option tempDisamb = new Option("tempDisamb", "tempDisamb", false, "temporary to test the java disambiguator");

		// verbose and help
		OptionGroup nofile = new OptionGroup();
		Option nofileopt = new Option("nofile", "nofilename", false, "don't include the long filenames in the XML.");
		nofile.addOption(nofileopt);

		// latex
		OptionGroup tex = new OptionGroup();
		Option pdf = new Option("pdf", false, "generate pdf from definition");
		Option latex = new Option("latex", false, "generate latex from definition");
		Option style = new Option("style", true, "pass styling information to the k.sty package");
		Option maudify = new Option("m", "maudify", false, "maudify the definition");
		Option compile = new Option("c", "compile", false, "compile the definition");
		Option toXml = new Option("xml", false, "generate xml from definition");
		Option toDoc = new Option("doc", "documentation", false, "generate the HTML documentation");

		tex.addOption(latex);
		tex.addOption(pdf);
		tex.addOption(compile);
		tex.addOption(maudify);
		tex.addOption(toXml);
		tex.addOption(toDoc);

		OptionGroup warn = new OptionGroup();
		Option warnings = new Option("w", "warnings", false, "display all warnings");
		warn.addOption(warnings);

		// xml
		OptionGroup xml = new OptionGroup();
		Option fromXml = new Option("fromxml", true, "load definition from xml");
		xml.addOption(fromXml);

		// language module name
		OptionGroup langGroup = new OptionGroup();
		Option lang = new Option("l", "lang", true, "start module");
		langGroup.addOption(lang);

		// language module name
		OptionGroup langSynGroup = new OptionGroup();
		Option langSyn = new Option("synmod", "syntax-module", true, "start module for syntax");
		langSynGroup.addOption(langSyn);

		// language module name
		// OptionGroup preludeGroup = new OptionGroup();
		// Option prelude = new Option("prelude", true, "Load a different prelude file.");
		// preludeGroup.addOption(prelude);

		// lib
		OptionGroup libGroup = new OptionGroup();
		Option lib = new Option("lib", true, "Specify extra-libraries for compile/runtime.");
		libGroup.addOption(lib);

		Option toHTML = new Option("html", false, "generate html from definition");

		Option kexp = new Option("kexp", false, "retrieve the KExp associated to a definition");

		Option unparse = new Option("unparse", false, "unparse a definition");

		Option addTopCell = new Option("addTopCell", false, "add a top cell to configuration and all rules");

		// transition
		Option transition = new Option("transition", true, "<arg> tags to become rewrite rules");
		Option superheat = new Option("superheat", true, "syntax <arg> tags triggering super heating nondetermistic choice for strictness");
		Option supercool = new Option("supercool", true, "rule <arg> tags triggering super cooling tags are space-separated and can include the tag default");

		// add options
		options.addOption(new Option("testFactory", false, "Option to test new class instantiation"));
		options.addOptionGroup(verb);
		options.addOptionGroup(main);
		options.addOptionGroup(langGroup);
		options.addOptionGroup(langSynGroup);
		options.addOptionGroup(tex);
		options.addOptionGroup(warn);
		options.addOptionGroup(xml);
		options.addOptionGroup(libGroup);
		options.addOptionGroup(nofile);
		// options.addOption(tempDisamb);
		options.addOption(toHTML);
		options.addOption(kexp);
		options.addOption(unparse);
		options.addOption(addTopCell);
		options.addOption(transition);
		options.addOption(supercool);
		options.addOption(superheat);
		options.addOption(def);
		options.addOption(step);
		options.addOption(style);
	}

	public CommandLine parse(String[] cmd) {
		CommandLineParser parser = new PosixParser();
		try {
			CommandLine cl = parser.parse(options, cmd);
			return cl;
		} catch (ParseException e) {
			org.kframework.utils.Error.silentReport(e.getLocalizedMessage());
			// e.printStackTrace();
		}

		org.kframework.utils.Error.helpExit(help, options);
		return null;
	}

	public Options getOptions() {
		return options;
	}

	public HelpFormatter getHelp() {
		return help;
	}
}

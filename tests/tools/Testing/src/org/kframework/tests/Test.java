package org.kframework.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.kframework.execution.Task;
import org.kframework.main.Configuration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Test {

	/* data read from config.xml */
	private String language;
	private String programsFolder;
	private String resultsFolder;
	private boolean recursive;
	private List<String> extensions;
	private List<String> excludePrograms;
	private Map<String, String> kompileOptions = new HashMap<String, String>();
	private Map<String, String> generalKrunOptions = new HashMap<String, String>();
	private List<Program> specialPrograms = new LinkedList<Program>();
	private boolean pdf;

	/* data needed for temporary stuff */
	private Document doc;
	public Element report;
	private List<Program> programs;
	private String reportDir = null;

	public Test(Element test) {
		init(test);
		initializePrograms();
	}

	private void initializePrograms() {
		programs = new LinkedList<Program>();

		String[] pgmsFolders = this.programsFolder.split("\\s+");

		if (resultsFolder != null && !new File(getResultsFolder()).exists())
			System.out.println("Folder: " + Configuration.getHome()
					+ Configuration.FS + resultsFolder + " does not exists.");

		for (int i = 0; i < pgmsFolders.length; i++) {
			String programsFolder = pgmsFolders[i];

			if (!new File(Configuration.getHome() + Configuration.FS
					+ programsFolder).exists())
				System.out.println("Folder: " + Configuration.getHome()
						+ Configuration.FS + programsFolder
						+ " does not exists.");

			if (programsFolder == null || programsFolder.equals(""))
				return;

			List<String> allProgramPaths = searchAll(Configuration.getHome()
					+ Configuration.FS + programsFolder, extensions, recursive);

			for (String programPath : allProgramPaths) {
				// ignore the programs from exclude list
				boolean excluded = false;
				for (String exclude : excludePrograms)
					if (programPath.equals(Configuration.getHome()
							+ Configuration.FS + programsFolder
							+ Configuration.FS + exclude))
						excluded = true;
				if (excluded)
					continue;

				Map<String, String> krunOptions = null;
				boolean special = false;
				// treat special programs
				for (Program p : specialPrograms) {
					if (p.programPath.equals(programPath)) {
						krunOptions = p.krunOptions;
						special = true;
						continue;
					}
				}
				if (!special)
					krunOptions = this.generalKrunOptions;

				String input = null;
				String output = null;
				String error = null;
				if (resultsFolder != null) {

					String inputFile = searchInputFile(getResultsFolder(),
							new File(programPath).getName(), recursive);
					input = getFileAsStringOrNull(inputFile);

					String outputFile = searchOutputFile(getResultsFolder(),
							new File(programPath).getName(), recursive);
					output = getFileAsStringOrNull(outputFile);

					String errorFile = searchErrorFile(getResultsFolder(),
							new File(programPath).getName(), recursive);
					error = getFileAsStringOrNull(errorFile);

				}

				// custom programPath
				programPath = programPath.replaceFirst(Configuration.getHome()
						+ Configuration.FS, "");

				Program p = new Program(programPath, krunOptions, this, input,
						output, error);
				programs.add(p);

			}
		}
	}

	private String getResultsFolder() {
		return Configuration.getHome() + Configuration.FS + resultsFolder;
	}

	private String getFileAsStringOrNull(String file) {
		String fileAsString = null;
		if (file != null)
			try {
				fileAsString = Task.readString(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		return fileAsString;
	}

	private String searchOutputFile(String resultsFolder2, String name,
			boolean recursive2) {
		return searchFile(resultsFolder2, name + ".out", recursive);
	}

	private String searchErrorFile(String resultsFolder2, String name,
			boolean recursive2) {
		return searchFile(resultsFolder2, name + ".err", recursive);
	}

	private String searchInputFile(String resultsFolder2, String name,
			boolean recursive) {
		return searchFile(resultsFolder2, name + ".in", recursive);
	}

	private String searchFile(String folder, String filename, boolean recursive) {
		String[] files = new File(folder).list();
		String file = null;
		if (files != null)
			for (int i = 0; i < files.length; i++) {
				if (new File(folder + Configuration.FS + files[i]).isFile())
					if (files[i].equals(filename))
						file = new File(folder + Configuration.FS + files[i])
								.getAbsolutePath();
				if (recursive)
					if (new File(folder + Configuration.FS + files[i])
							.isDirectory())
						file = searchFile(folder + Configuration.FS + files[i],
								filename, recursive);

				if (file != null)
					return file;
			}

		return file;
	}

	private List<String> searchAll(String programsFolder,
			List<String> extensions, boolean recursive) {

		List<String> paths = new LinkedList<String>();
		for (String extension : extensions)
			paths.addAll(searchAll(programsFolder, extension));

		if (recursive) {
			String[] files = new File(programsFolder).list();
			if (files != null)
				for (int i = 0; i < files.length; i++) {
					if (new File(programsFolder + Configuration.FS + files[i])
							.isDirectory()) {
						paths.addAll(searchAll(programsFolder
								+ Configuration.FS + files[i], extensions,
								recursive));
					}
				}
		}

		return paths;
	}

	private List<String> searchAll(String programsFolder2, String extension) {
		String[] files = new File(programsFolder2).list();
		List<String> fls = new LinkedList<String>();
		if (files != null) {
			for (int i = 0; i < files.length; i++)
				if (new File(programsFolder2 + Configuration.FS + files[i])
						.isFile()) {
					if (files[i].endsWith(extension))
						fls.add(programsFolder2 + Configuration.FS + files[i]);
				}
		}
		return fls;
	}

	private void init(Element test) {
		String homeDir = Configuration.getHome();

		// get full name
		language = test.getAttribute("language");

		// get programs dir
		programsFolder = test.getAttribute("folder");

		// get tests results
		resultsFolder = test.getAttribute("results");
		if (resultsFolder.equals(""))
			resultsFolder = null;

		// get tests results
		reportDir = test.getAttribute("report-dir");
		if (reportDir.equals(""))
			reportDir = null;

		// get pdf
		if (test.getAttribute("pdf").equals("yes")
				|| test.getAttribute("pdf").equals(""))
			pdf = true;
		else
			pdf = false;

		// set recursive
		String rec = test.getAttribute("recursive");
		if (rec.equals("") || rec.equals("yes"))
			recursive = true;
		else
			recursive = false;

		// extensions
		extensions = Arrays.asList(test.getAttribute("extensions")
				.split("\\s+"));

		// exclude programs
		excludePrograms = Arrays.asList(test.getAttribute("exclude").split(
				"\\s+"));

		// kompile options
		NodeList kompileOpts = test.getElementsByTagName("kompile-option");
		for (int i = 0; i < kompileOpts.getLength(); i++) {
			Element option = (Element) kompileOpts.item(i);
			kompileOptions.put(option.getAttribute("name"),
					option.getAttribute("value"));
		}

		// load programs with special option
		NodeList specialPgms = test.getElementsByTagName("program");
		for (int i = 0; i < specialPgms.getLength(); i++) {
			Element pgm = (Element) specialPgms.item(i);
			String programPath = pgm.getAttribute("name");

			Map<String, String> map = getKrunOptions(pgm);

			String input = null;
			String output = null;
			String error = null;
			if (resultsFolder != null) {

				String inputFile = searchInputFile(getResultsFolder(),
						new File(programPath).getName(), recursive);
				input = getFileAsStringOrNull(inputFile);

				String outputFile = searchOutputFile(getResultsFolder(),
						new File(programPath).getName(), recursive);
				output = getFileAsStringOrNull(outputFile);

				String errorFile = searchErrorFile(getResultsFolder(),
						new File(programPath).getName(), recursive);
				error = getFileAsStringOrNull(errorFile);

			}

			Program program = new Program(homeDir + Configuration.FS
					+ programPath, map, this, input, output, error);
			specialPrograms.add(program);
		}

		// general krun options
		NodeList genOpts = test.getElementsByTagName("all-programs");
		if (genOpts != null && genOpts.getLength() > 0) {
			Element all = (Element) genOpts.item(0);
			generalKrunOptions = getKrunOptions(all);
		}

		if (genOpts.getLength() == 0) {
			generalKrunOptions.put("--no-color", "");
			generalKrunOptions.put("--output-mode", "none");
		}

		// reports
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		report = getInitialElement(language);
		doc.appendChild(report);
	}

	private Map<String, String> getKrunOptions(Element parent) {
		Map<String, String> map = new HashMap<String, String>();
		NodeList opts = parent.getElementsByTagName("krun-option");
		for (int j = 0; j < opts.getLength(); j++) {
			Element krunOpt = (Element) opts.item(j);

			// unescape < and >
			String optValue = krunOpt.getAttribute("value");
			optValue = optValue.replaceAll("&lt;", "<");
			optValue = optValue.replaceAll("&gt;", ">");

			map.put(krunOpt.getAttribute("name"), optValue);
		}
		return map;
	}

	private Element getInitialElement(String definition) {
		Element testsuite = doc.createElement("testsuite");
		String name = "";
		if (reportDir != null)
			name = reportDir;
		else
			name = new File(language).getParent();

		testsuite.setAttribute("name",
				name.replaceFirst("/", "").replaceFirst("/", "\\."));
		return testsuite;
	}

	public Element createReportElement(String testcase, String status,
			String time, String output, String error, Task task,
			String expected, boolean failureCondition) {
		Element testcaseE = doc.createElement("testcase");
		testcaseE.setAttribute("name", testcase);
		testcaseE.setAttribute("status", status);
		testcaseE.setAttribute("time", time);

		Element sysout = doc.createElement("system-out");
		sysout.setTextContent(output);

		Element syserr = doc.createElement("system-err");
		syserr.setTextContent(error);

		testcaseE.appendChild(syserr);
		testcaseE.appendChild(sysout);

		if (failureCondition) {
			Element error_ = doc.createElement("error");
			error_.setTextContent(task.getStderr());
			error_.setAttribute("message", task.getStderr());
			testcaseE.appendChild(error_);

			Element failure = doc.createElement("failure");
			failure.setTextContent("Expecting:\n" + expected
					+ "\nbut returned:\n" + task.getStdout());
			testcaseE.appendChild(failure);
		}

		return testcaseE;
	}

	public Task getDefinitionTask(File homeDir) {
		ArrayList<String> command = new ArrayList<String>();
		command.add(Configuration.getKompile());
		command.add(language);
		command.add("-o");
		command.add(getCompiled());
		for (Entry<String, String> entry : kompileOptions.entrySet()) {
			command.add(entry.getKey());
			command.add(entry.getValue());
		}

		String[] arguments = new String[command.size()];
		int i = 0;
		for (String cmd : command) {
			arguments[i] = cmd;
			i++;
		}

		return new Task(arguments, null, homeDir);
	}

	public String compileStatus(Task task) {
		return "Compiling " + language + "...\t\t"
				+ (compiled(task) ? "success" : "failed");
	}

	public boolean compiled(Task task) {
		if (task.getExit() != 0)
			return false;

		if (!new File(getCompiled()).exists())
			return false;

		if (!task.getStderr().equals(""))
			return false;

		if (!task.getStdout().equals(""))
			return false;

		return true;
	}

	public String getCompiled() {
		return getLanguage().replaceAll("\\.k$", "") + "-kompiled";
	}

	private String getReportFilename() {
		if (reportDir != null)
			return reportDir + "-report.xml";

		return language.replaceFirst("\\.k$", "-report.xml").replaceAll(
				"\\/", ".");
	}

	public void reportCompilation(Task task) {
		String message = compiled(task) ? "success" : "failed";
		if (!task.getStdout().equals("") || !task.getStderr().equals(""))
			if (message.equals("success"))
				message = "unstable";

		report.appendChild(createReportElement(new File(language).getName(),
				message, task.getElapsed() + "", task.getStdout(),
				task.getStderr(), task, "", !compiled(task)));

		save();
	}

	public void reportPdfCompilation(Task task) {
		String message = compiledPdf(task) ? "success" : "failed";
		if (!task.getStdout().equals(""))
			if (message.equals("success"))
				message = "unstable";

		report.appendChild(createReportElement(new File(getXmlLanguage())
				.getName().replaceFirst("\\.k$", ".pdf"), message,
				task.getElapsed() + "", task.getStdout(), task.getStderr(),
				task, "", !compiledPdf(task)));

		save();
	}

	public boolean compiledPdf(Task task) {
		if (task.getExit() != 0)
			return false;

		if (!new File(getPdfCompiledFilename()).exists())
			return false;

		if (!task.getStderr().equals(""))
			return false;

		if (!task.getStdout().equals(""))
			return false;

		return true;
	}

	private String getPdfCompiledFilename() {
		return getLanguage().replaceAll("\\.k$", ".pdf");

	}

	public void save() {
		String reportPath = Configuration.JR + Configuration.FS
				+ getReportFilename();
		new File(Configuration.JR).mkdirs();
		try {

			File repFile = new File(reportPath);
			if (!repFile.exists())	
				repFile.createNewFile();
			
			FileWriter fstream = new FileWriter(Configuration.JR
					+ Configuration.FS + getReportFilename());
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(format(doc));
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String format(Document document) {
		Transformer transformer;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			// initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(document);
			transformer.transform(source, result);
			return result.getWriter().toString();

		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean getPdf() {
		return pdf;
	}

	public Task getPdfDefinitionTask(File homeDir) {
		ArrayList<String> command = new ArrayList<String>();
		command.add(Configuration.getKompile());
		command.add(getXmlLanguage());
		command.add("--pdf");
		command.add("-o");
		command.add(getPdfCompiledFilename());
		String[] arguments = new String[command.size()];
		int i = 0;
		for (String cmd : command) {
			arguments[i] = cmd;
			i++;
		}

		return new Task(arguments, null, homeDir);
	}

	private String getXmlLanguage() {
		return getCompiled() + Configuration.FS + "defx.bin";
		// return language;
	}

	public List<Program> getPrograms() {
		return programs;
	}

	public String getLanguage() {
		return language;
	}

}

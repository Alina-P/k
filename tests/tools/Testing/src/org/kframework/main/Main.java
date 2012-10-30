package org.kframework.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.kframework.execution.Execution;
import org.kframework.execution.Task;
import org.kframework.tests.Program;
import org.kframework.tests.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Main {

	public static void main(String[] args) {

		// a little bit hack-ish but it works until somebody complains
		System.out.println("UDIR: " + System.getProperty("user.dir"));
		if (System.getProperty("user.dir").contains("jenkins")) {
			// first copy the k-framework artifacts
			try {
				copyFolder(new File("/var/lib/jenkins/workspace/k-framework"),
						new File("k"));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// setup the new path
			Configuration.HOME_DIR = "/var/lib/jenkins/workspace/k-framework-tests/k";

			// build K
			try {
				ProcessBuilder pb = new ProcessBuilder("ant");
				pb.directory(new File(Configuration.HOME_DIR));
				Process process = pb.start();
				int exit = process.waitFor();
				String out = Task.readString(process.getInputStream());
				String err = Task.readString(process.getErrorStream());
				System.out.println(out);
				System.out.println(err);
				if (exit != 0)
					System.exit(exit);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// execute the right scripts
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			Configuration.kompile = Configuration.kompile + ".bat";
			Configuration.krun = Configuration.kompile + ".bat";
		}

		List<Test> alltests = new LinkedList<Test>();

		// load config
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			System.out.println(Configuration.config);
			Document doc = dBuilder.parse(new File(Configuration.config));
			Element root = doc.getDocumentElement();

			NodeList test = root.getElementsByTagName("test");
			for (int i = 0; i < test.getLength(); i++)
				alltests.add(new Test((Element) test.item(i)));

		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// compile definitions first
		Map<Test, Task> definitions = new HashMap<Test, Task>();
		Map<Test, Task> pdfDefinitions = new HashMap<Test, Task>();
		for (Test test : alltests) {
			Task def = test.getDefinitionTask();
			definitions.put(test, def);
			Execution.execute(def);

			// also compile pdf if set
			if (test.getPdf()) {
				Task pdfDef = test.getPdfDefinitionTask();
				pdfDefinitions.put(test, pdfDef);
				Execution.execute(pdfDef);
			}
		}

		Execution.finish();

		// report
		for (Entry<Test, Task> entry : definitions.entrySet()) {
			entry.getKey().reportCompilation(entry.getValue());
		}
		for (Entry<Test, Task> entry : pdfDefinitions.entrySet()) {
			entry.getKey().reportPdfCompilation(entry.getValue());
		}
		System.out.println();

		// console display
		System.out.println("Compilation failed for: ");
		for (Entry<Test, Task> entry : definitions.entrySet()) {
			if (!entry.getKey().compiled(entry.getValue()))
				System.out.println("Compiling "
						+ entry.getKey().getLanguage()
								.substring(Configuration.getHomeDir().length())
						+ " failed.");
		}
		System.out.println();

		// console display
		System.out.println("Pdf compilation failed for: ");
		for (Entry<Test, Task> entry : pdfDefinitions.entrySet()) {
			if (!entry.getKey().compiledPdf(entry.getValue()))
				System.out.println("Compiling pdf "
						+ entry.getKey().getLanguage()
								.substring(Configuration.getHomeDir().length())
						+ " failed.");
		}

		// execute all programs (for which corresponding definitions are
		// compiled)
		for (Entry<Test, Task> dentry : definitions.entrySet()) {
			Test test = dentry.getKey();
			if (test.compiled(dentry.getValue())) {

				// execute
				List<Program> pgms = test.getPrograms();
				Map<Program, Task> all = new HashMap<Program, Task>();
				for (Program p : pgms) {
					Task task = p.getTask();
					all.put(p, task);
					Execution.tpe.execute(task);
				}

				Execution.finish();

				// report
				for (Entry<Program, Task> entry : all.entrySet()) {
					entry.getKey().reportTest(entry.getValue());
				}

				// console
				System.out.println("\n");
				System.out.println("Failed "
						+ test.getLanguage().substring(
								Configuration.getHomeDir().length())
						+ " programs:");
				for (Entry<Program, Task> entry : all.entrySet()) {
					if (!entry.getKey().success(entry.getValue())) {
						System.out
								.println("Running "
										+ entry.getKey()
												.getAbsolutePath()
												.substring(
														Configuration
																.getHomeDir()
																.length())
										+ " failed.");
					}
				}
			}
		}

	}

	public static void copyFolder(File src, File dest) throws IOException {

		if (src.getName().startsWith("."))
			return;

		if (src.isDirectory()) {

			// if directory not exists, create it
			if (!dest.exists()) {
				dest.mkdir();
			}

			// list all the directory contents
			String files[] = src.list();

			for (String file : files) {
				// construct the src and dest file structure
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				// recursive copy
				copyFolder(srcFile, destFile);
			}

		} else {
			// if file, then copy it
			// Use bytes stream to support all file types
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest);

			byte[] buffer = new byte[1024];

			int length;
			// copy the file content in bytes
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}

			in.close();
			out.close();
		}
	}
}

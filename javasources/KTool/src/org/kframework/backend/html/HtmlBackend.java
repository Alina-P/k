package org.kframework.backend.html;

import org.kframework.backend.BasicBackend;
import org.kframework.kil.Definition;
import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.kompile.KompileFrontEnd;
import org.kframework.utils.Stopwatch;
import org.kframework.utils.file.FileUtil;
import org.kframework.utils.file.KPaths;
import org.kframework.utils.general.GlobalSettings;

import java.io.File;
import java.io.IOException;

public class HtmlBackend extends BasicBackend {

	public HtmlBackend(Stopwatch sw, DefinitionHelper definitionHelper) {
		super(sw, definitionHelper);
	}

	@Override
	public void run(Definition definition) throws IOException {
		String fileSep = System.getProperty("file.separator");
		String htmlIncludePath = KPaths.getKBase(false) + fileSep + "include" + fileSep + "html" + fileSep;
		HTMLFilter htmlFilter = new HTMLFilter(htmlIncludePath, definitionHelper);
		definition.accept(htmlFilter);

		String html = htmlFilter.getHTML();

		String output = KompileFrontEnd.output;
		if (output == null) {
			output = "./" + FileUtil.stripExtension(new File(definition.getMainFile()).getName()) + ".html";
		}

		output = new File(output).getAbsolutePath();

		FileUtil.saveInFile(output, html);
		if (GlobalSettings.verbose) {
			sw.printIntermediate("Generating HTML");
		}

	}

	@Override
	public String getDefaultStep() {
		return "FirstStep";
	}
}

package k.utils;

import java.io.File;
import java.io.IOException;

import k3.basic.Definition;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ro.uaic.info.fmse.disambiguate.AmbDuplicateFilter;
import ro.uaic.info.fmse.disambiguate.AmbFilter;
import ro.uaic.info.fmse.disambiguate.BestFitFilter;
import ro.uaic.info.fmse.disambiguate.CellTypesFilter;
import ro.uaic.info.fmse.disambiguate.CorrectKSeqFilter;
import ro.uaic.info.fmse.disambiguate.CorrectRewritePriorityFilter;
import ro.uaic.info.fmse.disambiguate.CorrectRewriteSortFilter;
import ro.uaic.info.fmse.disambiguate.FlattenListsFilter;
import ro.uaic.info.fmse.disambiguate.GetFitnessUnitFileCheckVisitor;
import ro.uaic.info.fmse.disambiguate.GetFitnessUnitKCheckVisitor;
import ro.uaic.info.fmse.disambiguate.GetFitnessUnitTypeCheckVisitor;
import ro.uaic.info.fmse.disambiguate.TypeInferenceSupremumFilter;
import ro.uaic.info.fmse.disambiguate.TypeSystemFilter;
import ro.uaic.info.fmse.disambiguate.VariableTypeInferenceFilter;
import ro.uaic.info.fmse.general.GlobalSettings;
import ro.uaic.info.fmse.lists.EmptyListsVisitor;
import ro.uaic.info.fmse.loader.CollectConsesVisitor;
import ro.uaic.info.fmse.loader.CollectSubsortsVisitor;
import ro.uaic.info.fmse.loader.UpdateReferencesVisitor;
import ro.uaic.info.fmse.pp.Preprocessor;

import com.thoughtworks.xstream.XStream;

public class DefinitionLoader {
	public static ro.uaic.info.fmse.k.Definition loadDefinition(File mainFile, String lang) throws IOException, Exception {
		ro.uaic.info.fmse.k.Definition javaDef;
		File canoFile = mainFile.getCanonicalFile();

		if (FileUtil.getExtension(mainFile.getAbsolutePath()).equals(".xml")) {
			// unmarshalling
			XStream xstream = new XStream();
			xstream.aliasPackage("k", "ro.uaic.info.fmse.k");

			javaDef = (ro.uaic.info.fmse.k.Definition) xstream.fromXML(canoFile);
			javaDef.preprocess();

		} else {
			File dotk = new File(canoFile.getParent() + "/.k");
			dotk.mkdirs();
			javaDef = parseDefinition(lang, canoFile, dotk);
		}
		return javaDef;
	}

	public static ro.uaic.info.fmse.k.Definition parseDefinition(String mainModule, File canonicalFile, File dotk) throws IOException, Exception {
		Stopwatch sw = new Stopwatch();
		// ------------------------------------- basic parsing
		Definition def = new Definition();
		def.slurp(canonicalFile, true);
		def.setMainFile(canonicalFile);
		def.setMainModule(mainModule);
		def.addConsToProductions();

		if (GlobalSettings.verbose)
			sw.printIntermediate("Basic Parsing   = ");

		// ------------------------------------- generate files
		ResourceExtractor.ExtractAllSDF(dotk);

		ResourceExtractor.ExtractProgramSDF(dotk);

		// ------------------------------------- generate parser TBL
		// cache the TBL if the sdf file is the same
		String oldSdf = "";
		if (new File(dotk.getAbsolutePath() + "/pgm/Program.sdf").exists())
			oldSdf = FileUtil.getFileContent(dotk.getAbsolutePath() + "/pgm/Program.sdf");
		FileUtil.saveInFile(dotk.getAbsolutePath() + "/pgm/Program.sdf", def.getSDFForPrograms());

		String newSdf = FileUtil.getFileContent(dotk.getAbsolutePath() + "/pgm/Program.sdf");

		if (GlobalSettings.verbose)
			sw.printIntermediate("File Gen Pgm    = ");

		if (!oldSdf.equals(newSdf))
			Sdf2Table.run_sdf2table(new File(dotk.getAbsoluteFile() + "/pgm"), "Program");

		if (GlobalSettings.verbose)
			sw.printIntermediate("Generate TBLPgm = ");

		// generate a copy for the definition and modify it to generate the intermediate data
		Definition def2 = def.clone();// (Definition) Cloner.copy(def);
		def2.makeConsLists();

		FileUtil.saveInFile(dotk.getAbsolutePath() + "/Integration.sbs", def2.getSubsortingAsStrategoTerms());
		FileUtil.saveInFile(dotk.getAbsolutePath() + "/Integration.cons", def2.getConsAsStrategoTerms());

		// ------------------------------------- generate parser TBL
		// cache the TBL if the sdf file is the same
		oldSdf = "";
		if (new File(dotk.getAbsolutePath() + "/def/Integration.sdf").exists())
			oldSdf = FileUtil.getFileContent(dotk.getAbsolutePath() + "/def/Integration.sdf");
		FileUtil.saveInFile(dotk.getAbsolutePath() + "/def/Integration.sdf", def.getSDFForDefinition());
		newSdf = FileUtil.getFileContent(dotk.getAbsolutePath() + "/def/Integration.sdf");

		if (GlobalSettings.verbose)
			sw.printIntermediate("File Gen Def    = ");

		if (!oldSdf.equals(newSdf))
			Sdf2Table.run_sdf2table(new File(dotk.getAbsoluteFile() + "/def"), "K3Disamb");

		if (GlobalSettings.verbose)
			sw.printIntermediate("Generate TBLDef = ");

		// ------------------------------------- import files in Stratego
		k3parser.KParser.ImportSbs(dotk.getAbsolutePath() + "/Integration.sbs");
		k3parser.KParser.ImportCons(dotk.getAbsolutePath() + "/Integration.cons");
		k3parser.KParser.ImportTbl(dotk.getAbsolutePath() + "/def/K3Disamb.tbl");

		if (GlobalSettings.verbose)
			sw.printIntermediate("Importing Files = ");

		// ------------------------------------- parse configs
		FileUtil.saveInFile(dotk.getAbsolutePath() + "/Integration.cells", def.getCellsFromConfigAsStrategoTerm());
		k3parser.KParser.ImportCells(dotk.getAbsolutePath() + "/Integration.cells");

		if (GlobalSettings.verbose)
			sw.printIntermediate("Parsing Configs = ");

		// ----------------------------------- parse rules
		def.parseRules();

		// ----------------------------------- preprocessiong steps
		Preprocessor preprocessor = new Preprocessor();
		Document preprocessedDef = preprocessor.run(def.getDefAsXML());

		XmlLoader.writeXmlFile(preprocessedDef, dotk.getAbsolutePath() + "/def.xml");

		if (GlobalSettings.verbose)
			sw.printIntermediate("Parsing Rules   = ");

		ro.uaic.info.fmse.k.Definition javaDef = new ro.uaic.info.fmse.k.Definition((Element) preprocessedDef.getFirstChild());

		javaDef.accept(new UpdateReferencesVisitor());
		javaDef.accept(new CollectConsesVisitor());
		javaDef.accept(new CollectSubsortsVisitor());
		// disambiguation steps

		if (GlobalSettings.tempDisamb) {
			javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new CellTypesFilter());
			javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new CorrectRewritePriorityFilter());
			javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new CorrectKSeqFilter());
			javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new BestFitFilter(new GetFitnessUnitFileCheckVisitor()));
			javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new VariableTypeInferenceFilter());
			javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new AmbDuplicateFilter());
			javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new TypeSystemFilter());
			javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new BestFitFilter(new GetFitnessUnitTypeCheckVisitor()));
			javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new BestFitFilter(new GetFitnessUnitKCheckVisitor()));
			javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new TypeInferenceSupremumFilter());
			javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new FlattenListsFilter());
			javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new CorrectRewriteSortFilter());
			if (GlobalSettings.verbose)
				sw.printIntermediate("Disambiguate    = ");
		}
		// last resort disambiguation
		javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new AmbFilter());

		javaDef = (ro.uaic.info.fmse.k.Definition) javaDef.accept(new EmptyListsVisitor());

		return javaDef;
	}
}

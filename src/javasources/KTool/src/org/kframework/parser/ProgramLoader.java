package org.kframework.parser;

import org.kframework.backend.symbolic.RemoveExtraVariable;
import org.kframework.backend.symbolic.SubstituteFreshVariablesWithSymbolic;
import org.kframework.compile.transformers.AddEmptyLists;
import org.kframework.compile.transformers.FlattenSyntax;
import org.kframework.compile.transformers.RemoveBrackets;
import org.kframework.compile.transformers.RemoveSyntacticCasts;
import org.kframework.compile.utils.CompilerStepDone;
import org.kframework.compile.utils.RuleCompilerSteps;
import org.kframework.compile.utils.TermContainsCells;
import org.kframework.kcheck.utils.ExtractCellContent;
import org.kframework.kil.*;
import org.kframework.kil.loader.Context;
import org.kframework.kil.loader.JavaClassesFactory;
import org.kframework.kil.loader.ResolveVariableAttribute;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.krun.K;
import org.kframework.parser.concrete.disambiguate.AmbFilter;
import org.kframework.parser.concrete.disambiguate.PreferAvoidFilter;
import org.kframework.parser.concrete.disambiguate.PriorityFilter;
import org.kframework.parser.utils.ReportErrorsVisitor;
import org.kframework.parser.utils.Sglr;
import org.kframework.utils.BinaryLoader;
import org.kframework.utils.Stopwatch;
import org.kframework.utils.XmlLoader;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.errorsystem.KException.ExceptionType;
import org.kframework.utils.errorsystem.KException.KExceptionGroup;
import org.kframework.utils.file.FileUtil;
import org.kframework.utils.general.GlobalSettings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;

public class ProgramLoader {

	/**
	 * Load program file to ASTNode.
	 * 
	 * Write pgm.xml cache in given dotk folder.
	 * 
	 * @param kappize
	 *            If true, then apply KAppModifier to AST.
	 */
	public static ASTNode loadPgmAst(String content, String filename, Boolean kappize, String startSymbol, Context context) throws IOException,
			TransformerException {
		File tbl = new File(context.kompiled.getCanonicalPath() + "/pgm/Program.tbl");

		// ------------------------------------- import files in Stratego
		ASTNode out;

		if (GlobalSettings.fastKast) {
			//out = Sglri.run_sglri(context.kompiled.getAbsolutePath() + "/pgm/Program.tbl", startSymbol, content);
			JavaClassesFactory.startConstruction(context);
			out = Sglr.run_sglri(context.kompiled.getAbsolutePath() + "/pgm/Program.tbl", startSymbol, content, filename);
			out.accept(new ReportErrorsVisitor(context, "file"));
			JavaClassesFactory.endConstruction();
		} else {
			org.kframework.parser.concrete.KParser.ImportTblPgm(tbl.getAbsolutePath());
			String parsed = org.kframework.parser.concrete.KParser.ParseProgramString(content, startSymbol);
			Document doc = XmlLoader.getXMLDoc(parsed);

			XmlLoader.addFilename(doc.getFirstChild(), filename);
			XmlLoader.reportErrors(doc);
			FileUtil.save(context.kompiled.getAbsolutePath() + "/pgm.xml", parsed);
			JavaClassesFactory.startConstruction(context);
			out = JavaClassesFactory.getTerm((Element) doc.getDocumentElement().getFirstChild().getNextSibling());
			JavaClassesFactory.endConstruction();
		}

		out = out.accept(new PriorityFilter(context));
		out = out.accept(new PreferAvoidFilter(context));
		out = out.accept(new AmbFilter(context));
		out = out.accept(new RemoveBrackets(context));

		if (kappize)
			out = out.accept(new FlattenSyntax(context));

		return out;
	}

	public static ASTNode loadPgmAst(String content, String filename, String startSymbol, Context context) throws IOException, TransformerException {
		return loadPgmAst(content, filename, true, startSymbol, context);
	}

	public static ASTNode loadPgmAst(File pgmFile, boolean kappize, String startSymbol, Context context) throws IOException, TransformerException {
		String filename = pgmFile.getCanonicalFile().getAbsolutePath();
		String content = FileUtil.getFileContent(filename);
		return loadPgmAst(content, filename, kappize, startSymbol, context);
	}

	/**
	 * Print maudified program to standard output.
	 * 
	 * Save it in kompiled cache under pgm.maude.
	 */
	public static Term processPgm(String content, String filename, Definition def, String startSymbol,
            Context context, GlobalSettings.ParserType whatParser) throws TransformerException {
		// compile a definition here
		Stopwatch sw = new Stopwatch();

		if (GlobalSettings.verbose)
			sw.printIntermediate("Importing Files");

		try {
			ASTNode out;
			if (whatParser == GlobalSettings.ParserType.GROUND) {
				org.kframework.parser.concrete.KParser.ImportTblGround(context.kompiled.getCanonicalPath() + "/ground/Concrete.tbl");
				out = DefinitionLoader.parseCmdString(content, "", filename, context);
				out = out.accept(new RemoveBrackets(context));
				out = out.accept(new AddEmptyLists(context));
				out = out.accept(new RemoveSyntacticCasts(context));
				out = out.accept(new FlattenSyntax(context));
				out = out.accept(new RemoveSyntacticCasts(context));
			} else if (whatParser == GlobalSettings.ParserType.RULES) {
				org.kframework.parser.concrete.KParser.ImportTbl(context.kompiled.getCanonicalPath() + "/def/Concrete.tbl");
				out = DefinitionLoader.parsePattern(content, filename, context);
				out = out.accept(new RemoveBrackets(context));
				out = out.accept(new AddEmptyLists(context));
				out = out.accept(new RemoveSyntacticCasts(context));
                out = out.accept(new SubstituteFreshVariablesWithSymbolic(context));
                TermContainsCells tcc = new TermContainsCells(context);
                out.accept(tcc);

                try {
                    // this appends wrapping cells to anything. Why is that?
					out = new RuleCompilerSteps(def, context).compile(
                            new Rule((Sentence) out),
                            null);
				} catch (CompilerStepDone e) {
					out = (ASTNode) e.getResult();
				}
				out = ((Rule) out).getBody();

                // if the parser is called on a simple program, with no wrapping cells
                // then return only the content of the <k> cell. This is because we add
                // cells in the previous step (RuleCompilerSteps).
                if (!tcc.getTermContainsCells()) {
                    ExtractCellContent extractor = new ExtractCellContent(context, "k");
                    out.accept(extractor);
                    out = extractor.getContent();
                    out = out.accept(new RemoveExtraVariable(context));
                }
			} else if (whatParser == GlobalSettings.ParserType.BINARY) {
                out = (org.kframework.kil.Cell) BinaryLoader.load(filename);
			} else {
                out = loadPgmAst(content, filename, startSymbol, context);
                out = out.accept(new ResolveVariableAttribute(context));
			}
			if (GlobalSettings.verbose) {
				sw.printIntermediate("Parsing Program");
			}

			return (Term) out;
		} catch (IOException e) {
			String msg = "Cannot parse program: " + e.getLocalizedMessage();
			throw new TransformerException(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, filename, "File system."));
		}
	}
}

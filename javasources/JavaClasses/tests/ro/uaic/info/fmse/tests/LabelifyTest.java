package ro.uaic.info.fmse.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ro.uaic.info.fmse.loader.JavaClassesFactory;
import ro.uaic.info.fmse.parsing.ASTNode;
import ro.uaic.info.fmse.transitions.labelify.KAppModifier;
import ro.uaic.info.fmse.utils.file.FileUtil;
import ro.uaic.info.fmse.utils.xml.XML;

public class LabelifyTest {

	@Test
	public void testGetTerm() {
		String file = "c:/work/k3/javasources/K3Syntax/test/imp/.k/def.xml";
		Document doc = XML.getDocument(FileUtil.readFileAsString(file));
		ASTNode out = JavaClassesFactory.getTerm(doc.getDocumentElement());

		file = "c:/work/k3/javasources/K3Syntax/test/imp/.k/pgm.xml";
		doc = XML.getDocument(FileUtil.readFileAsString(file));
		out = JavaClassesFactory.getTerm((Element) doc.getDocumentElement().getFirstChild().getNextSibling());
		out = out.accept(new KAppModifier());
		
		System.out.println(out.toMaude());

		assertTrue(doc != null);
	}
}

package ro.uaic.info.fmse.k;

import org.w3c.dom.Element;

import ro.uaic.info.fmse.parsing.ASTNode;

public abstract class DefinitionItem extends ASTNode {

	public DefinitionItem(Element element) {
		super(element);
	}
}

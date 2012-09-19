package org.kframework.kil;

import org.w3c.dom.Element;

public abstract class DefinitionItem extends ASTNode {

	public DefinitionItem() {
		super("File System", "generated");
	}

	public DefinitionItem(DefinitionItem di) {
		super(di);
	}

	public DefinitionItem(Element element) {
		super(element);
	}
}

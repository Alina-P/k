package org.kframework.kil.loader;

import org.kframework.kil.Production;
import org.kframework.kil.visitors.BasicVisitor;

public class CollectConsesVisitor extends BasicVisitor {
	@Override
	public void visit(Production node) {
		if (node.containsAttribute(Constants.CONS_cons_ATTR)) {
			String cons = node.getAttribute(Constants.CONS_cons_ATTR);
			DefinitionHelper.conses.put(cons, node);
			DefinitionHelper.putLabel(node, cons);
		}
		if (node.isListDecl()) {
			DefinitionHelper.listConses.put(node.getSort(), node);
			DefinitionHelper.putListLabel(node);
		}
	}
}

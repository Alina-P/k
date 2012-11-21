package org.kframework.kil;

import org.kframework.kil.loader.Constants;
import org.kframework.kil.loader.JavaClassesFactory;
import org.kframework.kil.visitors.Transformer;
import org.kframework.kil.visitors.Visitor;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.utils.xml.XML;
import org.w3c.dom.Element;

import java.util.ArrayList;

public class PriorityBlockExtended extends ASTNode {

	java.util.List<Constant> productions = new ArrayList<Constant>();
	String assoc;

	public java.util.List<Constant> getProductions() {
		return productions;
	}

	public void setProductions(java.util.List<Constant> productions) {
		this.productions = productions;
	}

	public String getAssoc() {
		return assoc;
	}

	public void setAssoc(String assoc) {
		this.assoc = assoc;
	}

	public PriorityBlockExtended() {
		super();
		this.assoc = "";
	}

	public PriorityBlockExtended(Element element) {
		super(element);

		java.util.List<Element> productions = XML.getChildrenElementsByTagName(element, Constants.CONST);
		for (Element production : productions)
			this.productions.add((Constant) JavaClassesFactory.getTerm(production));

		assoc = element.getAttribute(Constants.ASSOC_assoc_ATTR);
	}

	public PriorityBlockExtended(PriorityBlockExtended node) {
		super(node);
		this.assoc = node.assoc;
		this.productions.addAll(node.productions);
	}

	@Override
	public String toString() {
		String content = "";
		for (Term production : productions)
			content += production + "\n| ";

		if (content.length() > 2)
			content = content.substring(0, content.length() - 3);

		if (assoc.equals(""))
			return content;
		return assoc + ": " + content;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public ASTNode accept(Transformer visitor) throws TransformerException {
		return visitor.transform(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (!(obj instanceof PriorityBlockExtended))
			return false;
		PriorityBlockExtended pb = (PriorityBlockExtended) obj;

		if (!pb.getAssoc().equals(this.assoc))
			return false;

		if (pb.productions.size() != productions.size())
			return false;

		for (int i = 0; i < pb.productions.size(); i++) {
			if (!pb.productions.get(i).equals(productions.get(i)))
				return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int hash = assoc.hashCode();

		for (Term prd : productions)
			hash += prd.hashCode();
		return hash;
	}

	@Override
	public PriorityBlockExtended shallowCopy() {
		return new PriorityBlockExtended(this);
	}
}

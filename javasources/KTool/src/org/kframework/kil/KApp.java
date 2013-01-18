package org.kframework.kil;

import org.kframework.kil.loader.JavaClassesFactory;
import org.kframework.kil.visitors.Transformer;
import org.kframework.kil.visitors.Visitor;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.utils.xml.XML;
import org.w3c.dom.Element;

/**
 * Represents a term in sort K explicitly constructed as an application of a klabel to arguments.
 */
public class KApp extends Term {
	/** The label, always of sort KLabel, usually an instance of {@link Constant} */
	Term label;
	 /** The arguments, always of sort KList, usually an instance of {@link KList} or {@link Empty} */
	Term child;

	public KApp(String location, String filename) {
		super(location, filename, "K");
	}

	public KApp(String location, String filename, Term label, Term child) {
		super(location, filename, "K");
		this.label = label;
		this.child = child;
	}

	public KApp(Term label, Term child) {
		super("K");
		this.label = label;
		this.child = child;
	}

	public KApp(Element element) {
		super(element);
		Element elm = XML.getChildrenElements(element).get(0);
		Element elmBody = XML.getChildrenElements(elm).get(0);
		this.label = (Term) JavaClassesFactory.getTerm(elmBody);

		elm = XML.getChildrenElements(element).get(1);
		this.child = (Term) JavaClassesFactory.getTerm(elm);
	}

	public KApp(KApp node) {
		super(node);
		this.label = node.label;
		this.child = node.child;
	}

	public String toString() {
		return this.label + "(" + this.child + ")";
	}

	public Term getLabel() {
		return label;
	}

	public void setLabel(Term label) {
		this.label = label;
	}

	public Term getChild() {
		return child;
	}

	public void setChild(Term child) {
		this.child = child;
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
	public KApp shallowCopy() {
		return new KApp(this);
	}
}

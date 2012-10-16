package org.kframework.kil;

import org.kframework.kil.loader.Constants;
import org.kframework.kil.loader.JavaClassesFactory;
import org.kframework.kil.visitors.Modifier;
import org.kframework.kil.visitors.Transformer;
import org.kframework.kil.visitors.Visitor;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.utils.utils.xml.XML;
import org.w3c.dom.Element;


public class Rewrite extends Term {
	private Term left;
	private Term right;

	public Rewrite(Element element) {
		super(element);

		Element temp = XML.getChildrenElementsByTagName(element, Constants.LEFT).get(0);
		temp = XML.getChildrenElements(temp).get(0);
		left = (Term) JavaClassesFactory.getTerm(temp);
		temp = XML.getChildrenElementsByTagName(element, Constants.RIGHT).get(0);
		temp = XML.getChildrenElements(temp).get(0);
		right = (Term) JavaClassesFactory.getTerm(temp);
	}

	public Rewrite(Rewrite node) {
		super(node);
		this.left = node.left;
		this.right = node.right;
	}

	public Rewrite(Term eval1Left, Term eval1Right) {
		super(eval1Left.getSort());
		left = eval1Left;
		right = eval1Right;
	}

	public void setLeft(Term left) {
		this.left = left;
	}

	public Term getLeft() {
		return left;
	}

	public void setRight(Term right) {
		this.right = right;
	}

	public Term getRight() {
		return right;
	}

	@Override
	public String toString() {
		return left + " => " + right;
	}

	@Override
	public void applyToAll(Modifier visitor) {
		left = (Term) visitor.modify(left);
		right = (Term) visitor.modify(right);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public ASTNode accept(Transformer visitor) throws TransformerException {
		return visitor.transform(this);
	}

//	public String getComputedSort() {
//		if (sort.equals("List{K}")) {
//			String lsort = left.getSort();
//			String rsort = right.getSort();
//
//			if (!lsort.equals("List{K}") && !rsort.equals("List{K}"))
//				sort = "K";
//		}
//		return sort;
//	}

	@Override
	public Rewrite shallowCopy() {
		return new Rewrite(this);
	}
}

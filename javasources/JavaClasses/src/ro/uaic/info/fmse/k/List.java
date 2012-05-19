package ro.uaic.info.fmse.k;

import org.w3c.dom.Element;

import ro.uaic.info.fmse.parsing.ASTNode;
import ro.uaic.info.fmse.parsing.Transformer;
import ro.uaic.info.fmse.parsing.Visitor;

public class List extends Collection {

	public List(Element element) {
		super(element);
	}

	public List(List node) {
		super(node);
	}

	@Override
	public String toString() {
		return super.toString();
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}
	@Override
	public ASTNode accept(Transformer visitor) {
		return visitor.transform(this);
	}
}

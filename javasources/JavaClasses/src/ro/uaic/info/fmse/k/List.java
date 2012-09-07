package ro.uaic.info.fmse.k;

import org.w3c.dom.Element;

import ro.uaic.info.fmse.visitors.Transformer;
import ro.uaic.info.fmse.visitors.Visitor;

public class List extends Collection {

	public List(Element element) {
		super(element);
	}

	public List(List node) {
		super(node);
	}

	public List(String location, String filename) {
		super(location, filename, "List");
	}

	public List() {
		super("List");
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

	@Override
	public List shallowCopy() {
		return new List(this);
	}
}

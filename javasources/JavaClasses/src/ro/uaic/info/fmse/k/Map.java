package ro.uaic.info.fmse.k;

import org.w3c.dom.Element;

import ro.uaic.info.fmse.parsing.ASTNode;
import ro.uaic.info.fmse.parsing.Transformer;
import ro.uaic.info.fmse.parsing.Visitor;


public class Map extends Collection {

	public Map(Element element) {
		super(element);
		// TODO Auto-generated constructor stub
	}

	public Map(Map node) {
		super(node);
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

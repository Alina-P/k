package ro.uaic.info.fmse.k;

import org.w3c.dom.Element;

import ro.uaic.info.fmse.visitors.Transformer;
import ro.uaic.info.fmse.visitors.Visitor;

public class Map extends Collection {

	public Map(Element element) {
		super(element);
	}

	public Map(String location, String filename) {
		super(location, filename, "Map");
	}

	public Map(Map node) {
		super(node);
	}

	public Map() {
		super("Map");
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
	public Map shallowCopy() {
		return new Map(this);
	}
}

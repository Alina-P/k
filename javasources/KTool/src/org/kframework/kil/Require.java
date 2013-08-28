package org.kframework.kil;

import org.kframework.kil.loader.Constants;
import org.kframework.kil.visitors.Transformer;
import org.kframework.kil.visitors.Visitor;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.w3c.dom.Element;

/** A require directive */
public class Require extends DefinitionItem {
	/** The string argument to {@code require}, as written in the input file. */
	private String value;

	public Require(Element element) {
		super(element);
		value = element.getAttribute(Constants.VALUE_value_ATTR);
	}

	public Require(Require require) {
		super(require);
		value = require.value;
	}

	public Require(String value) {
		super();
		this.value = value;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public ASTNode accept(Transformer transformer) throws TransformerException {
		return transformer.transform(this);
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public Require shallowCopy() {
		return new Require(this);
	}

	@Override
        public String toString() {
          return "require \""+value+"\"";
        }
}

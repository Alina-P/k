package org.kframework.kil;

import org.kframework.kil.loader.Constants;
import org.kframework.kil.visitors.Transformer;
import org.kframework.kil.matchers.Matcher;
import org.kframework.kil.visitors.Visitor;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.w3c.dom.Element;

/** An import directive */
public class Import extends ModuleItem {

	String name;

	public Import(Element element) {
		super(element);
		name = element.getAttribute(Constants.NAME_name_ATTR);
	}

	public Import(String importName) {
		super((Element) null);
		name = importName;
	}

	public Import(Import import1) {
		super(import1);
		this.name = import1.name;
	}

	@Override
	public String toString() {
		return "  imports " + name;
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
  public void accept(Matcher matcher, ASTNode toMatch){
    matcher.match(this, toMatch);
  }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Import shallowCopy() {
		return new Import(this);
	}
}

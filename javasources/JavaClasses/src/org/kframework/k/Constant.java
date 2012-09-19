package org.kframework.k;

import org.kframework.exceptions.TransformerException;
import org.kframework.loader.Constants;
import org.kframework.visitors.Modifier;
import org.kframework.visitors.Transformer;
import org.kframework.visitors.Visitor;
import org.w3c.dom.Element;


public class Constant extends Term {
	String value;

	public Constant(String sort, String value) {
		super(sort);
		this.value = value;
	}
	
	public Constant(String location, String filename, String sort, String value) {
		super(location, filename, sort);
		this.value = value;
	}

	public Constant(Element element) {
		super(element);
		this.sort = element.getAttribute(Constants.SORT_sort_ATTR);
		this.value = element.getAttribute(Constants.VALUE_value_ATTR);
	}

	public Constant(Constant constant) {
		super(constant);
		this.value = constant.value;
	}

	public String toString() {
		return value + " ";
	}

	@Override
	public String toMaude() {
		if (sort.equals("#Id"))
			return "#id \"" + value + "\"";

		return value;
	}

	public String getSort() {
		return sort;
	}

	public String getValue() {
		return value;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public void applyToAll(Modifier visitor) {
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
	public Constant shallowCopy() {
		return new Constant(this);
	}
}

package org.kframework.kil;

import org.kframework.kil.loader.Constants;
import org.kframework.kil.visitors.Transformer;
import org.kframework.kil.visitors.Visitor;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.w3c.dom.Element;

public class Variable extends Term {
	private String name;
	private boolean userTyped = false;

	public Variable(Element element) {
		super(element);
		this.sort = element.getAttribute(Constants.SORT_sort_ATTR);
		this.name = element.getAttribute(Constants.NAME_name_ATTR);
		this.userTyped = element.getAttribute(Constants.TYPE_userTyped_ATTR).equals("true");
	}

	public Variable(String name, String sort) {
		super(sort);
		this.name = name;
	}

	public Variable(Variable variable) {
		super(variable);
		name = variable.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return name + ":" + sort + " ";
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
		if (!(obj instanceof Variable))
			return false;
		Variable var = (Variable) obj;

		return this.sort.equals(var.getSort()) && this.name.equals(var.getName());
	}

	@Override
	public int hashCode() {
		return sort.hashCode() + name.hashCode();
	}

	public void setUserTyped(boolean userTyped) {
		this.userTyped = userTyped;
	}

	public boolean isUserTyped() {
		return userTyped;
	}

	@Override
	public Variable shallowCopy() {
		return new Variable(this);
	}
}
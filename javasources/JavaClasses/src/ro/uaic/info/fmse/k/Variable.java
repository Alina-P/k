package ro.uaic.info.fmse.k;

import org.w3c.dom.Element;

import ro.uaic.info.fmse.loader.Constants;
import ro.uaic.info.fmse.visitors.Modifier;
import ro.uaic.info.fmse.visitors.Transformer;
import ro.uaic.info.fmse.visitors.Visitor;

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
		super("File System", "generated");
		this.sort = sort;
		this.name = name;
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
	public String toMaude() {
		if (name.equals("_"))
			return "?:" + sort;
		return name + ":" + sort;
	}

	public void accept(Modifier visitor) {
		visitor.modify(this);
	}

	@Override
	public void applyToAll(Modifier visitor) {
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
}
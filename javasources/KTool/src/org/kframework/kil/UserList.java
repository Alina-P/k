package org.kframework.kil;

import org.kframework.kil.loader.Constants;
import org.kframework.kil.visitors.Transformer;
import org.kframework.kil.visitors.Visitor;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.w3c.dom.Element;

/**
 * A production item for a cons-list with separator, like List{UserSort,";"}. Must be the only item in a {@link Production}.
 */
public class UserList extends ProductionItem {
	protected String sort;
	protected String separator;
	protected String listType;

    public UserList(String sort, String separator) {
        this.sort = sort;
        this.separator = separator;
        this.listType = "*";
    }

	public UserList(Element element) {
		super(element);

		sort = element.getAttribute(Constants.VALUE_value_ATTR);
		separator = element.getAttribute(Constants.SEPARATOR_separator_ATTR).trim();
		listType = element.getAttribute(Constants.TYPE_type_ATTR);
	}

	public UserList(UserList userList) {
		super(userList);
		sort = userList.sort;
		separator = userList.separator;
		listType = userList.listType;
	}

	public ProductionType getType() {
		return ProductionType.USERLIST;
	}

	@Override
	public String toString() {
		if (listType.equals("*"))
			return "List{" + sort + ",\"" + separator + "\"} ";
		else
			return "NeList{" + sort + ",\"" + separator + "\"} ";
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public ASTNode accept(Transformer transformer) throws TransformerException {
		return transformer.transform(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof UserList))
			return false;

		UserList srt = (UserList) obj;

		if (!sort.equals(srt.getSort()))
			return false;
		if (!separator.equals(srt.getSeparator()))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return this.separator.hashCode() + this.sort.hashCode();
	}

	@Override
	public UserList shallowCopy() {
		return new UserList(this);
	}

	public String getListType() {
		return listType;
	}

	public void setListType(String listType) {
		this.listType = listType;
	}
}

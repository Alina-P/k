package ro.uaic.info.fmse.k;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ro.uaic.info.fmse.loader.Constants;
import ro.uaic.info.fmse.parsing.Visitor;

public class UserList extends ProductionItem {
	protected String sort;
	protected String separator;

	public UserList(Element element) {
		super(element);

		sort = element.getAttribute(Constants.VALUE_value_ATTR);
		separator = element.getAttribute(Constants.SEPARATOR_separator_ATTR);
	}

	public ProductionType getType() {
		return ProductionType.USERLIST;
	}

	@Override
	public String toString() {
		return "List{" + sort + ",\"" + separator + "\"} ";
	}

	@Override
	public String toMaude() {
		return "";
	}

	@Override
	public Element toXml(Document doc) {
		Element userlist = doc.createElement(Constants.USERLIST);
		return userlist;
	}

	public void accept(Visitor visitor) {
		visitor.visit(this);
	}
}

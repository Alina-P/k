package ro.uaic.info.fmse.k;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ro.uaic.info.fmse.loader.Constants;

public class Terminal extends ProductionItem {

	String terminal;

	public Terminal(Element element) {
		super(element);
		terminal = element.getAttribute(Constants.VALUE_value_ATTR);
	}

	public ProductionType getType() {
		return ProductionType.TERMINAL;
	}

	@Override
	public String toString() {
		return "\"" + terminal + "\"";
	}
	
	@Override
	public String toMaude() {
		return "";
	}

	@Override
	public Element toXml(Document doc) {
		Element terminal = doc.createElement(Constants.TERMINAL);
		terminal.setTextContent(this.terminal);
		return terminal;
	}
}

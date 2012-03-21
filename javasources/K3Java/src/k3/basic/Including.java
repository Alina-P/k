package k3.basic;

import k.utils.Tag;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class Including extends Sentence {
	private static final long serialVersionUID = 1L;
	String includedModuleName;
	
	public Including clone() {
		return this;
	}

	public String getIncludedModuleName() {
		return includedModuleName;
	}

	public void setIncludedModuleName(String includedModuleName) {
		this.includedModuleName = includedModuleName;
	}

	public Including(Node node) {
		NamedNodeMap attr = node.getAttributes();
		Node item = attr.getNamedItem(Tag.value);
		includedModuleName = item.getNodeValue();
	}

	@Override
	public SentenceType getType() {
		return SentenceType.INCLUDING;
	}
}

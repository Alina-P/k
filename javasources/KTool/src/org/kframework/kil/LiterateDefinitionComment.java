package org.kframework.kil;

import org.kframework.kil.loader.Constants;
import org.kframework.kil.visitors.Modifier;
import org.kframework.kil.visitors.Transformer;
import org.kframework.kil.visitors.Visitor;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.w3c.dom.Element;

public class LiterateDefinitionComment extends DefinitionItem implements LiterateComment {
	private String value;
	private LiterateCommentType lcType;

	public LiterateDefinitionComment(Element element) {
		super(element);
		setValue(element.getAttribute(Constants.VALUE_value_ATTR));
		if (element.hasAttribute("special")) {
			String special = element.getAttribute("special");
			if (special.equals("latex"))
				this.lcType = LiterateCommentType.LATEX;
			else if (special.equals("preamble"))
				this.lcType = LiterateCommentType.PREAMBLE;
		} else
			this.lcType = LiterateCommentType.COMMON;
	}

	public LiterateDefinitionComment(LiterateDefinitionComment literateDefinitionComment) {
		super(literateDefinitionComment);
		value = literateDefinitionComment.value;
		lcType = literateDefinitionComment.lcType;
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

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public LiterateCommentType getType() {
		return lcType;
	}

	@Override
	public LiterateDefinitionComment shallowCopy() {
		return new LiterateDefinitionComment(this);
	}

	@Override
	public String toString() {
		String shortStr = value;
		if (value.indexOf("\n") > 0)
			value.substring(0, value.indexOf("\n") - 1);
		return shortStr;
	}
}

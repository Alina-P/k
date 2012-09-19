package org.kframework.k;

import org.kframework.exceptions.TransformerException;
import org.kframework.loader.Constants;
import org.kframework.visitors.Modifier;
import org.kframework.visitors.Transformer;
import org.kframework.visitors.Visitor;
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

	public LiterateDefinitionComment(
			LiterateDefinitionComment literateDefinitionComment) {
		super(literateDefinitionComment);
		value = literateDefinitionComment.value;
		lcType = literateDefinitionComment.lcType; 
	}

	@Override
	public String toMaude() {
		return "";
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
}

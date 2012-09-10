package ro.uaic.info.fmse.k;

import org.w3c.dom.Element;

import ro.uaic.info.fmse.exceptions.TransformerException;
import ro.uaic.info.fmse.loader.Constants;
import ro.uaic.info.fmse.loader.DefinitionHelper;
import ro.uaic.info.fmse.transitions.maude.MaudeHelper;
import ro.uaic.info.fmse.visitors.Modifier;
import ro.uaic.info.fmse.visitors.Transformer;
import ro.uaic.info.fmse.visitors.Visitor;

public class Empty extends Term {

	public Empty(String sort) {
		super(sort);
	}

	public Empty(String location, String filename, String sort) {
		super(location, filename, sort);
	}

	public Empty(Element element) {
		super(element);
		this.sort = element.getAttribute(Constants.SORT_sort_ATTR);
	}

	public Empty(Empty empty) {
		super(empty);
	}

	public String toString() {
		return "." + sort + " ";
	}

	@Override
	public String toMaude() {
		if (MaudeHelper.basicSorts.contains(sort)) {
			if (!sort.equals("List{K}"))
				return "(.)." + sort;
			else
				return "." + sort;
		}
		// search for the separator for the empty list
		Production prd = DefinitionHelper.listConses.get(getSort());
		UserList ul = (UserList) prd.getItems().get(0);
		return ".List`{\"" + ul.separator + "\"`}";
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
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
	public Empty shallowCopy() {
		return new Empty(this);
	}
}

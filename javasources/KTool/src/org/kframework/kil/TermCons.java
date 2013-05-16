package org.kframework.kil;

import java.util.ArrayList;
import java.util.List;

import org.kframework.kil.ProductionItem.ProductionType;
import org.kframework.kil.loader.Constants;
import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.kil.loader.JavaClassesFactory;
import org.kframework.kil.matchers.Matcher;
import org.kframework.kil.visitors.Transformer;
import org.kframework.kil.visitors.Visitor;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.utils.StringUtil;
import org.kframework.utils.xml.XML;
import org.w3c.dom.Element;

import aterm.ATermAppl;
import aterm.ATermList;

/**
 * Applications that are not in sort K, or have not yet been flattened.
 */
public class TermCons extends Term {
	/** A unique identifier corresponding to a production, matching the SDF cons */
	protected final String cons;
	protected java.util.List<Term> contents;

	public TermCons(Element element) {
		super(element);
		this.sort = element.getAttribute(Constants.SORT_sort_ATTR);
		this.cons = element.getAttribute(Constants.CONS_cons_ATTR);

		contents = new ArrayList<Term>();
		List<Element> children = XML.getChildrenElements(element);
		for (Element e : children)
			contents.add((Term) JavaClassesFactory.getTerm(e));
	}

	public TermCons(ATermAppl atm) {
		super(atm);
		this.cons = atm.getName();
		this.sort = StringUtil.getSortNameFromCons(cons);

		contents = new ArrayList<Term>();
		if (atm.getArity() == 0) {
			contents = new ArrayList<Term>();
		} else if (atm.getArgument(0) instanceof ATermList) {
			ATermList list = (ATermList) atm.getArgument(0);
			while (!list.isEmpty()) {
				contents.add((Term) JavaClassesFactory.getTerm(list.getFirst()));
				list = list.getNext();
			}
			contents.add(new Empty(sort));
		} else {
			for (int i = 0; i < atm.getArity(); i++) {
				contents.add((Term) JavaClassesFactory.getTerm(atm.getArgument(i)));
			}
		}
	}

	public TermCons(String sort, String cons) {
		super(sort);
		this.cons = cons;
		contents = new ArrayList<Term>();
	}

	public TermCons(String location, String filename, String sort, String listCons, List<Term> genContents) {
		super(location, filename, sort);
		cons = listCons;
		contents = genContents;
	}

	public TermCons(TermCons node) {
		super(node);
		this.cons = node.cons;
		this.contents = new ArrayList<Term>(node.contents);
	}

	public TermCons(String psort, String listCons, List<Term> genContents) {
		super(psort);
		cons = listCons;
		contents = genContents;
	}

	public Production getProduction(DefinitionHelper definitionHelper) {
		return definitionHelper.conses.get(getCons());
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder("TermCons(");
		str.append(cons);
		for (Term t : contents) {
			str.append(',');
			str.append(t);
		}
		str.append(')');
		return str.toString();
	}
	
	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public String getCons() {
		return cons;
	}

	public java.util.List<Term> getContents() {
		return contents;
	}

	public void setContents(java.util.List<Term> contents) {
		this.contents = contents;
	}

	public Term getSubterm(int idx) {
		return contents.get(idx);
	}

	public Term setSubterm(int idx, Term term) {
		return contents.set(idx, term);
	}

	public int arity(DefinitionHelper definitionHelper) {
		return getProduction(definitionHelper).getArity();
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
	public void accept(Matcher matcher, Term toMatch) {
		matcher.match(this, toMatch);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (!(obj instanceof TermCons))
			return false;
		TermCons tc = (TermCons) obj;

		if (!tc.getSort().equals(this.sort))
			return false;
		if (!tc.cons.equals(cons))
			return false;

		if (tc.contents.size() != contents.size())
			return false;

		for (int i = 0; i < tc.contents.size(); i++) {
			if (!tc.contents.get(i).equals(contents.get(i)))
				return false;
		}

		return true;
	}

	@Override
	public boolean contains(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (obj instanceof Bracket)
			return contains(((Bracket) obj).getContent());
		if (obj instanceof Cast)
			return contains(((Cast) obj).getContent());
		if (!(obj instanceof TermCons))
			return false;
		TermCons tc = (TermCons) obj;

		if (!tc.getSort().equals(this.sort))
			return false;
		if (!tc.cons.equals(cons))
			return false;

		if (tc.contents.size() != contents.size())
			return false;

		for (int i = 0; i < tc.contents.size(); i++) {
			if (!contents.get(i).contains(tc.contents.get(i)))
				return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int hash = sort.hashCode() + cons.hashCode();

		for (Term t : contents)
			hash += t.hashCode();
		return hash;
	}

	@Override
	public TermCons shallowCopy() {
		return new TermCons(this);
	}
}

package ro.uaic.info.fmse.k;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import ro.uaic.info.fmse.compile.utils.MetaK;
import ro.uaic.info.fmse.errorsystem.KException;
import ro.uaic.info.fmse.errorsystem.KException.ExceptionType;
import ro.uaic.info.fmse.errorsystem.KException.KExceptionGroup;
import ro.uaic.info.fmse.general.GlobalSettings;
import ro.uaic.info.fmse.loader.JavaClassesFactory;
import ro.uaic.info.fmse.utils.xml.XML;
import ro.uaic.info.fmse.visitors.Modifier;

public abstract class Collection extends Term {

	protected java.util.List<Term> contents;
	
	public Collection(String sort) {
		super(sort);
		contents = new ArrayList<Term>();
	}

	public Collection(Collection c) {
		super(c);
		this.contents = c.contents;
	}

	public Collection(String location, String filename, String sort) {
		super(location, filename, sort);
		contents = new ArrayList<Term>();
	}

	public Collection(Element element) {
		super(element);

		contents = new ArrayList<Term>();
		List<Element> children = XML.getChildrenElements(element);
		for (Element e : children)
			contents.add((Term) JavaClassesFactory.getTerm(e));
	}

	@Override
	public String toString() {
		String content = "";
		for (Term t : contents)
			content += t;
		return content;
	}

	@Override
	public String toMaude() {
		String content = "";
		
		if (contents.size()==0) {
			return new Empty(sort).toMaude();
		}
		
		if (contents.size()==1) {
			return contents.get(0).toMaude();
		}

		for (Term term : contents) {
			if (term == null) {
				GlobalSettings.kem.register(new KException(ExceptionType.ERROR, 
						KExceptionGroup.INTERNAL, 
						"NULL Term encountered when printing collection " + contents + ".", 
						getFilename(), getLocation(), 0));				
			}
			content += term.toMaude() + ",";
		}

		if (content.length() > 1)
			content = content.substring(0, content.length() - 1);
        String constructor = MetaK.getMaudeConstructor(sort);
		return constructor + "(" + content + ")";
	}

	public java.util.List<Term> getContents() {
		return contents;
	}

	public void setContents(java.util.List<Term> contents) {
		this.contents = contents;
	}

	@Override
	public void applyToAll(Modifier visitor) {
		for (int i = 0; i < this.contents.size(); i++) {
			Term elem = (Term) visitor.modify(this.contents.get(i));
			this.contents.set(i, elem);
		}
	}
	
	@Override
	public abstract Collection shallowCopy();
}

package org.kframework.kil;

import org.w3c.dom.Element;

/** Subclasses wrap a term as an item in the corresponding collection */
public abstract class CollectionItem extends Term {

	protected Term value;

	public CollectionItem(CollectionItem i) {
		super(i);
		this.value = i.value;
	}
	
	public CollectionItem(String location, String filename, String sort) {
		super(location, filename, sort);
	}

	public CollectionItem(Element element) {
		super(element);
	}

	public CollectionItem(String sort) {
		super(sort);
	}

	public void setItem(Term value) {
		this.value = value;
	}
	
	public Term getItem() {
		return value;
	}
	
	@Override
	public abstract CollectionItem shallowCopy();

}
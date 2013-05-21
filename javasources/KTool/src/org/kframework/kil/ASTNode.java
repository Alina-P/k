package org.kframework.kil;

import java.io.Serializable;
import java.util.Set;

import org.kframework.compile.utils.SyntaxByTag;
import org.kframework.kil.loader.Constants;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.Transformable;
import org.kframework.kil.visitors.Visitable;
import org.w3c.dom.Element;

import aterm.ATermAppl;

/**
 * Base class for K AST. Useful for Visitors and Transformers.
 * 
 * @see Visitable
 * @see Transformable
 */
public abstract class ASTNode implements Visitable, Transformable, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Used on any node for metadata such as location, also used on {@link Rule} and {@link Production} for the attribute list.
	 */
	protected Attributes attributes;

	/**
	 * Initializes an ASTNode from XML describing the parse tree
	 * 
	 * @param elem
	 *            The XML element describing the ASTNode
	 */
	public ASTNode(Element elem) {
		this(getElementLocation(elem), getElementFile(elem));
	}

	/**
	 * Initializes an ASTNode from the corresponding Stratego datastructure.
	 * 
	 * @param elem
	 *            the Stratego object representing an ASTNode
	 */
	public ASTNode(ATermAppl elem) {
		String loc = elem.getAnnotations().getFirst().toString().substring(8);
		this.setLocation(loc);
	}

	/**
	 * Retrieves the location from an XML element
	 * 
	 * @param elem
	 * @return the location stored in XML or Constants.GENERATED_LOCATION if no location found.
	 */
	private static String getElementLocation(Element elem) {
		if (elem != null && elem.hasAttribute(Constants.LOC_loc_ATTR))
			return elem.getAttribute(Constants.LOC_loc_ATTR);
		else
			return Constants.GENERATED_LOCATION;
	}

	/**
	 * Retrieves the file name from an XML element
	 * 
	 * @param elem
	 * @return the file name stored in XML or Constants.GENERATED_FILENAME if no filename found.
	 */
	private static String getElementFile(Element elem) {
		if (elem != null && elem.hasAttribute(Constants.LOC_loc_ATTR))
			return elem.getAttribute(Constants.FILENAME_filename_ATTR);
		else
			return Constants.GENERATED_FILENAME;
	}

	/**
	 * Copy constructor
	 * 
	 * @param astNode
	 */
	public ASTNode(ASTNode astNode) {
		attributes = astNode.attributes;
	}

	/**
	 * Default constructor (generated at runtime)
	 */
	public ASTNode() {
		this(Constants.GENERATED_LOCATION, Constants.GENERATED_FILENAME);
	}

	/**
	 * Constructor with specified location and filename.
	 * 
	 * @param loc
	 * @param file
	 */
	public ASTNode(String loc, String file) {
		// attributes = new Attributes();
		setLocation(loc);
		setFilename(file);
	}

	/**
	 * Retrieves the location of the current ASTNode.
	 * 
	 * @return recorded location or Constants.GENERATED_LOCATION if no recorded location found.
	 */
	public String getLocation() {
		return getAttribute("location");
	}

	/**
	 * Sets the location or removes it if appropriate.
	 * 
	 * @param loc
	 */
	public void setLocation(String loc) {
		putAttribute("location", loc);
	}

	/**
	 * Retrieves the filename of the current ASTNode.
	 * 
	 * @return recorded filename or Constants.GENERATED_FILENAME if no recorded location found.
	 */
	public String getFilename() {
		return getAttribute("filename");
	}

	/**
	 * Sets the filename or removes it if appropriate.
	 * 
	 * @param file
	 */
	public void setFilename(String file) {
		putAttribute("filename", file);
	}

	/*
	 * methods for easy attributes manipulation
	 */

	/**
	 * Appends an attribute to the list of attributes.
	 * 
	 * @param key
	 * @param val
	 */
	public void addAttribute(String key, String val) {
		addAttribute(new Attribute(key, val));
	}

	/**
	 * Appends an attribute to the list of attributes.
	 * 
	 * @param attr
	 */
	public void addAttribute(Attribute attr) {
		if (attributes == null)
			attributes = new Attributes();

		attributes.contents.add(attr);
	}

	/**
	 * @param key
	 * @return whether the attribute key occurs in the list of attributes.
	 */
	public boolean containsAttribute(String key) {
		if (attributes == null)
			return false;

		return attributes.containsKey(key);
	}

	/**
	 * Retrieves the attribute by key from the list of attributes
	 * 
	 * @param key
	 * @return a value for key in the list of attributes or the default value.
	 */
	public String getAttribute(String key) {
		final String defaultValue = Constants.defaultAttributeValues.get(key);
		if (attributes == null)
			return defaultValue;
		final String value = attributes.get(key);
		if (value == null)
			return defaultValue;
		return value;
	}

	/**
	 * Updates the value of an attribute in the list of attributes.
	 * 
	 * @param key
	 * @param val
	 */
	public void putAttribute(String key, String val) {
		final String defaultValue = Constants.defaultAttributeValues.get(key);
		if (val.equals(defaultValue)) {
			if (getAttribute(key).equals(defaultValue))
				return;
			attributes.remove(key);
			return;
		}
		if (attributes == null)
			attributes = new Attributes();

		attributes.set(key, val);
	}

	/**
	 * @return the attributes object associated to this ASTNode.
	 */
	public Attributes getAttributes() {
		return attributes;
	}

	/**
	 * Sets the attributes object associated to this ASTNode.
	 * 
	 * @param attrs
	 */
	public void setAttributes(Attributes attrs) {
		attributes = attrs;
	}

	/**
	 * Retrieves the syntax production descendants of this ASTNode by attribute key.
	 * 
	 * @param key
	 * @return Set<Production> object containing the production descendants
	 */
	public Set<Production> getSyntaxByTag(String key, Context context) {
		return SyntaxByTag.get(this, key, context);
	}

	/**
	 * @return a copy of the ASTNode containing the same fields.
	 */
	public abstract ASTNode shallowCopy();
}

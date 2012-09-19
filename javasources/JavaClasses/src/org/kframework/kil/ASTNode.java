package org.kframework.kil;

import org.kframework.backend.maude.IMaude;
import org.kframework.kil.loader.Constants;
import org.kframework.kil.visitors.Modifiable;
import org.kframework.kil.visitors.Modifier;
import org.kframework.kil.visitors.Transformable;
import org.kframework.kil.visitors.Visitable;
import org.w3c.dom.Element;


public abstract class ASTNode implements IMaude, Modifiable, Visitable,
		Transformable {
	protected String location;
	protected String filename;
	protected Attributes attributes = null;


	public ASTNode(ASTNode di) {
		this.location = di.location;
		this.filename = di.filename;
		this.attributes = di.attributes;
	}

	public ASTNode(String location, String filename) {
		this.location = location;
		this.filename = filename;
	}

	public ASTNode(Element element) {
		if (element != null) {
			this.filename = element
					.getAttribute(Constants.FILENAME_filename_ATTR);
			this.location = element.getAttribute(Constants.LOC_loc_ATTR);
		}
	}

	public ASTNode() {
		this.location = Constants.GENERATED_LOCATION;
		this.filename = Constants.GENERATED_FILENAME;	
	}

	public String getMaudeLocation() {
		String location = this.location;
		location = location.replaceAll(",", ":");
		location = location.replaceFirst("\\(", "(" + this.filename + ":");
		if (!location.startsWith("(")) location = "(" + location + ")";

		return location;
	}

	public abstract void applyToAll(Modifier visitor);

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public abstract ASTNode shallowCopy();
}

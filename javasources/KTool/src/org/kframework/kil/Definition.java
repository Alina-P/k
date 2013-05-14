package org.kframework.kil;

import org.kframework.compile.sharing.TokenSorts;
import org.kframework.kil.loader.AddConsesVisitor;
import org.kframework.kil.loader.CollectConfigCellsVisitor;
import org.kframework.kil.loader.CollectConsesVisitor;
import org.kframework.kil.loader.CollectLocationsVisitor;
import org.kframework.kil.loader.CollectPrioritiesVisitor;
import org.kframework.kil.loader.CollectStartSymbolPgmVisitor;
import org.kframework.kil.loader.CollectSubsortsVisitor;
import org.kframework.kil.loader.Constants;
import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.kil.loader.JavaClassesFactory;
import org.kframework.kil.loader.UpdateAssocVisitor;
import org.kframework.kil.loader.UpdateReferencesVisitor;
import org.kframework.kil.visitors.Transformer;
import org.kframework.kil.visitors.Visitor;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.parser.DefinitionLoader;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.errorsystem.KException.ExceptionType;
import org.kframework.utils.errorsystem.KException.KExceptionGroup;
import org.kframework.utils.general.GlobalSettings;
import org.kframework.utils.xml.XML;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a language definition.
 * Includes contents from all {@code required}-d files.
 * @see DefinitionLoader
 * @see BasicParser
 */
public class Definition extends ASTNode {

	private List<DefinitionItem> items;
	private String mainFile;
	private String mainModule;
	/** An index of all modules in {@link #items} by name */
	private Map<String, Module> modulesMap;
	private String mainSyntaxModule;
    private final Set<String> tokenNames;

	public Definition() {
		super("File System", "generated");
        tokenNames = new HashSet<String>();
	}

	public Definition(Definition d) {
		super(d);
		this.mainFile = d.mainFile;
		this.mainModule = d.mainModule;
		this.mainSyntaxModule = d.mainSyntaxModule;
		this.items = d.items;
        this.tokenNames = new HashSet<String>(d.tokenNames);
	}

	public Definition(Element element) {
		super(element);

		mainFile = element.getAttribute(Constants.MAINFILE);
		mainModule = element.getAttribute(Constants.MAINMODULE);
		// mainSyntaxModule = element.getAttribute(Constants.MAINSYNTAXMODULE);
		items = new ArrayList<DefinitionItem>();

		List<Element> elements = XML.getChildrenElements(element);
		for (Element e : elements)
			items.add((DefinitionItem) JavaClassesFactory.getTerm(e));

        tokenNames = new HashSet<String>();
	}

	public void appendDefinitionItem(DefinitionItem di) {
		items.add(di);
	}

	public void appendBeforeDefinitionItem(DefinitionItem di) {
		items.add(0, di);
	}

	@Override
	public String toString() {
		String content = "";
		for (DefinitionItem di : items)
			content += di + " \n";

		return "DEF: " + mainFile + " -> " + mainModule + "\n" + content;
	}

	public void setItems(List<DefinitionItem> items) {
		this.items = items;
	}

	public List<DefinitionItem> getItems() {
		return items;
	}

	public void setMainFile(String mainFile) {
		this.mainFile = mainFile;
	}

	public String getMainFile() {
		return mainFile;
	}

	public void setMainModule(String mainModule) {
		this.mainModule = mainModule;
	}

	public String getMainModule() {
		return mainModule;
	}

	public void setMainSyntaxModule(String mainSyntaxModule) {
		this.mainSyntaxModule = mainSyntaxModule;
	}

	public String getMainSyntaxModule() {
		return mainSyntaxModule;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public ASTNode accept(Transformer visitor) throws TransformerException {
		return visitor.transform(this);
	}

	public void preprocess(DefinitionHelper definitionHelper) {
		// Collect information
		this.accept(new UpdateReferencesVisitor(definitionHelper));
		this.accept(new AddConsesVisitor(definitionHelper));
		this.accept(new CollectConsesVisitor(definitionHelper));
		this.accept(new CollectSubsortsVisitor(definitionHelper));
		this.accept(new CollectPrioritiesVisitor(definitionHelper));
		this.accept(new CollectStartSymbolPgmVisitor(definitionHelper));
		this.accept(new CollectConfigCellsVisitor(definitionHelper));
		this.accept(new UpdateAssocVisitor(definitionHelper));
		this.accept(new CollectLocationsVisitor(definitionHelper));
        TokenSorts tokenSortsVisitor = new TokenSorts(definitionHelper);
        this.accept(tokenSortsVisitor);
        tokenNames.addAll(tokenSortsVisitor.getNames());
        // TODO: fix #Id
        tokenNames.add("#Id");
		definitionHelper.initialized = true;
	}

	public Map<String, Module> getModulesMap() {
		return modulesMap;
	}

	public void setModulesMap(Map<String, Module> modulesMap) {
		this.modulesMap = modulesMap;
	}

	public Module getSingletonModule() {
		List<Module> modules = new LinkedList<Module>();
		for (DefinitionItem i : this.getItems()) {
			if (i instanceof Module)
				modules.add((Module) i);
		}
		if (modules.size() != 1) {
			String msg = "Should have been only one module when calling this method.";
			GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.INTERNAL, msg, this.getFilename(), this.getLocation()));
		}
		return modules.get(0);
	}

	public Definition updateSingletonModule(Module mod) {
		int moduleCount = 0;
		List<DefinitionItem> newDefinitionItems = new ArrayList<DefinitionItem>();
		for (DefinitionItem i : this.getItems()) {
			if (i instanceof Module) {
				moduleCount++;
				newDefinitionItems.add(mod);
			} else {
				newDefinitionItems.add(i);
			}
		}
		if (moduleCount != 1) {
			String msg = "Should have been only one module when calling this method.";
			GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.INTERNAL, msg, this.getFilename(), this.getLocation()));
		}
		Definition result = new Definition(this);
		result.setItems(newDefinitionItems);
		return result;
	}

    /**
     * Returns a list containing the names of the token productions in this definition.
     */
    public Set<String> tokenNames() {
        return tokenNames;
    }

	@Override
	public Definition shallowCopy() {
		return new Definition(this);
	}

	public Configuration getSingletonConfiguration() throws ConfigurationNotUnique, ConfigurationNotFound {
		Configuration result = null;
		for (DefinitionItem i : this.getItems()) {
			if (i instanceof Module) {
				if (((Module)i).isPredefined()) {
					continue;
				}
				for (ModuleItem j : ((Module) i).getItems()) {
					if (j instanceof Configuration) {
						if (result != null) {
							throw new ConfigurationNotUnique();
						} else {
							result = (Configuration)j;
						}
					}
				}
			}
		}
		if (result == null) {
			throw new ConfigurationNotFound();
		}
		return result;
	}
}

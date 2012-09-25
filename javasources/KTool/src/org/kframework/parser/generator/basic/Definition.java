package org.kframework.parser.generator.basic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.kframework.compile.utils.MetaK;
import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.parser.basic.KParser;
import org.kframework.parser.generator.basic.Item.ItemType;
import org.kframework.parser.generator.basic.ModuleItem.ModuleType;
import org.kframework.parser.generator.basic.Sentence.SentenceType;
import org.kframework.parser.latex.K3LatexParser;
import org.kframework.utils.Error;
import org.kframework.utils.StringUtil;
import org.kframework.utils.Tag;
import org.kframework.utils.XmlLoader;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.errorsystem.KMessages;
import org.kframework.utils.errorsystem.KException.ExceptionType;
import org.kframework.utils.errorsystem.KException.KExceptionGroup;
import org.kframework.utils.general.GlobalSettings;
import org.kframework.utils.utils.file.FileUtil;
import org.kframework.utils.utils.file.KPaths;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Definition implements Cloneable {
	private List<ModuleItem> modules;
	private Map<String, Module> modulesMap;
	private List<String> filePaths;
	private File mainFile;
	private String mainModule;

	public Definition() {
		modulesMap = new HashMap<String, Module>();
		modules = new ArrayList<ModuleItem>();
		filePaths = new ArrayList<String>();
	}

	public Definition clone() {
		try {
			super.clone();
			Definition def2 = new Definition();
			def2.mainFile = mainFile;

			for (ModuleItem mi : modules)
				if (mi.getType() == ModuleType.MODULE) {
					Module m = (Module) mi;
					Module m2 = m.clone();
					def2.modules.add(m2);
					def2.modulesMap.put(m2.getModuleName(), m2);
				} else
					def2.modules.add(mi.clone());
			for (String f : filePaths)
				def2.filePaths.add(f);

			return def2;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Given a file, this method parses it and creates a list of modules of all the included files.
	 * 
	 * @param filepath
	 */
	public void slurp(File file, boolean firstTime) {
		if (!file.exists()) {
			GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, "File: " + file.getName() + " not found.", file.getAbsolutePath(), "File system."));
		}
		try {
			String cannonicalPath = file.getCanonicalPath();
			if (!filePaths.contains(cannonicalPath)) {
				if (file.isDirectory())
					GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, file.getName() + " is a directory, not a file.", file.getAbsolutePath(), "File system."));
				if (!file.getAbsolutePath().endsWith(".k"))
					GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, file.getName() + " is not a K file.", file.getAbsolutePath(), "File system."));

				String content = FileUtil.getFileContent(file.getAbsolutePath());

				String parsed = KParser.ParseKString(content);
				Document doc = XmlLoader.getXMLDoc(parsed);
				XmlLoader.addFilename(doc.getFirstChild(), file.getAbsolutePath());
				XmlLoader.reportErrors(doc);

				String parsedLatex = null;
				Document docLatex = null;
				if (GlobalSettings.literate) {
					// parse the string again to extract the comments
					parsedLatex = K3LatexParser.ParseKString(content);
					docLatex = XmlLoader.getXMLDoc(parsedLatex);
					XmlLoader.addFilename(docLatex.getFirstChild(), file.getAbsolutePath());
					XmlLoader.reportErrors(docLatex);
				}

				if (firstTime) {
					// add automatically the autoinclude.k file
					if (GlobalSettings.verbose)
						System.out.println("Including file: " + "autoinclude.k");
					File newFilePath = buildInclPath(file, "autoinclude.k");
					slurp(newFilePath, false);
				}

				if (!file.getAbsolutePath().startsWith(new File(KPaths.getKBase(false) + "/include/").getAbsolutePath()))
					DefinitionHelper.addFileRequirement(buildInclPath(file, "autoinclude.k").getCanonicalPath(), file.getCanonicalPath());

				NodeList xmlIncludes = doc.getDocumentElement().getElementsByTagName(Tag.require);
				for (int i = 0; i < xmlIncludes.getLength(); i++) {
					String inclFile = xmlIncludes.item(i).getAttributes().getNamedItem("value").getNodeValue();
					if (GlobalSettings.verbose)
						System.out.println("Including file: " + inclFile);
					File newFilePath = buildInclPath(file, inclFile);
					slurp(newFilePath, false);
					DefinitionHelper.addFileRequirement(newFilePath.getCanonicalPath(), file.getCanonicalPath());
				}

				NodeList xmlModules = doc.getDocumentElement().getElementsByTagName(Tag.module);
				NodeList xmlComments = null;
				if (GlobalSettings.literate)
					xmlComments = docLatex.getDocumentElement().getElementsByTagName(Tag.comment);
				// TODO: insert latex comments in the def.xml

				java.util.List<ModuleItem> modulesTemp = new ArrayList<ModuleItem>();

				for (int i = 0; i < xmlModules.getLength(); i++) {
					Module km = new Module(xmlModules.item(i), cannonicalPath);
					// set the module type as predefined if it is located in the /include directory
					// used later for latex and when including SHARED module
					if (file.getAbsolutePath().startsWith(new File(KPaths.getKBase(false) + "/include/").getAbsolutePath()))
						km.setPredefined(true);
					if (GlobalSettings.literate)
						km.addComments(xmlComments);

					for (int j = 0; j < xmlIncludes.getLength(); j++)
						modulesTemp.add(new Require(xmlIncludes.item(j)));
					modulesTemp.add(km);
					modulesMap.put(km.getModuleName(), km);
				}

				if (GlobalSettings.literate)
					modules.addAll(Definition.mergeModuleAndComments(modulesTemp, xmlComments));
				else
					modules.addAll(modulesTemp);

				filePaths.add(cannonicalPath);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static List<ModuleItem> mergeModuleAndComments(List<ModuleItem> lm, NodeList lnl) {
		List<ModuleItem> lmi = new ArrayList<ModuleItem>();
		List<CommentDef> lcd = new ArrayList<CommentDef>();

		// select only the comments that are outside modules
		for (int i = 0; i < lnl.getLength(); i++) {
			Element elm = (Element) lnl.item(i);
			boolean intraModule = false;
			CommentDef cd = new CommentDef(elm);
			for (ModuleItem m : lm) {
				if (cd.getStartLine() >= m.getStartLine() && cd.getEndLine() <= m.getEndLine()) {
					intraModule = true;
					break;
				}
			}
			if (!intraModule)
				lcd.add(cd);
		}

		int i = 0;
		int j = 0;

		while (i < lm.size() && j < lcd.size()) {
			ModuleItem s = lm.get(i);
			CommentDef com = lcd.get(j);
			if (s.getStartLine() < com.getStartLine()) {
				lmi.add(s);
				i++;
			} else {
				lmi.add(com);
				j++;
			}
		}
		while (i < lm.size()) {
			lmi.add(lm.get(i));
			i++;
		}
		while (j < lcd.size()) {
			lmi.add(lcd.get(j));
			j++;
		}
		return lmi;
	}

	private File buildInclPath(File currFile, String inclFile) throws IOException {
		File file = currFile;
		String base = file.getParentFile().getAbsolutePath();
		String filepath = base + System.getProperty("file.separator") + inclFile;
		file = new File(filepath);

		if (!file.exists()) {
			file = new File(KPaths.getKBase(false) + "/include/" + inclFile);
			if (file.exists()) {
				return new File(file.getCanonicalPath());
			}

			file = new File(filepath);
			GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, "File: " + file.getName() + " not found.", file.getAbsolutePath(), "File system."));
		}
		return new File(file.getCanonicalPath());
	}

	public String getSDFForDefinition() {
		String sdf = "module Integration\n\n";
		sdf += "imports Common\n";
		sdf += "imports KTechnique\n";
		sdf += "imports KBuiltinsBasic\n\n";
		sdf += "exports\n\n";
		sdf += "context-free syntax\n";

		List<Production> outsides = new ArrayList<Production>();
		List<Production> constants = new ArrayList<Production>();
		Set<Sort> sorts = new HashSet<Sort>(); // list of inserted sorts that need to avoid the priority filter
		Set<Sort> startSorts = new HashSet<Sort>(); // list of sorts that are start symbols
		Set<Subsort> subsorts = new HashSet<Subsort>(); // list of sorts that are start symbols
		Set<Production> listProds = new HashSet<Production>(); // list of sorts declared as being list
		Set<Sort> userSorts = new HashSet<Sort>(); // list of sorts declared by the user (to be declared later as Start symbols if no declaration for Start was found)

		for (ModuleItem mi : modules) {
			if (mi.getType() == ModuleType.MODULE) {
				Module m = (Module) mi;
				for (Sentence s : m.getSentences()) {
					if (s.getType() == SentenceType.SYNTAX) {
						Syntax syn = (Syntax) s;
						userSorts.add(syn.getSort());
						List<Priority> prilist = new ArrayList<Priority>();
						for (Priority prt : syn.getPriorities()) {
							Priority p = new Priority();
							p.setBlockAssoc(prt.getBlockAssoc());

							// filter the productions according to their form
							for (Production prd : prt.getProductions()) {
								if (prd.getAttributes().containsKey("onlyLabel")) {
									// if a production has this attribute, don't add it to the list
								} else if (prd.isSubsort()) {
									outsides.add(prd);
									subsorts.add(new Subsort(prd.getProdSort(), (Sort) prd.getItems().get(0)));
									if (prd.getProdSort().equals(new Sort("Start")))
										startSorts.add((Sort) prd.getItems().get(0));
									// add the small sort to the user sorts to add it to the variable declarations
									userSorts.add((Sort) prd.getItems().get(0));
								} else if (prd.getItems().get(0).getType() == ItemType.TERMINAL && prd.getItems().size() == 1 && prd.getProdSort().isConstantSort()) {
									constants.add(prd);
								} else if (prd.getItems().get(0).getType() == ItemType.TERMINAL && prd.getItems().get(prd.getItems().size() - 1).getType() == ItemType.TERMINAL) {
									outsides.add(prd);
								} else if (prd.isListDecl()) {
									outsides.add(prd);
									listProds.add(prd);
									subsorts.add(new Subsort(prd.getProdSort(), ((UserList) prd.getItems().get(0)).getSort()));
								} else {
									p.getProductions().add(prd);
								}
							}
							if (p.getProductions().size() > 0)
								prilist.add(p);
						}
						if (prilist.size() > 0) {
							if (prilist.size() <= 1 && syn.getPriorities().get(0).getBlockAssoc() == null) {
								// weird bug in SDF - if you declare only one production in a priority block, it gives parse errors
								// you need to have at least 2 productions or a block association
								Priority prt = prilist.get(0);
								for (Production p : prt.getProductions())
									outsides.add(p);
							} else {
								sdf += "context-free priorities\n";

								for (Priority prt : prilist) {
									if (prt.getBlockAssoc() == null)
										sdf += "{\n";
									else
										sdf += "{ " + prt.getBlockAssoc() + ":\n";
									for (Production p : prt.getProductions()) {
										sdf += "	";
										List<Item> items = p.getItems();
										for (int i = 0; i < items.size(); i++) {
											Item itm = items.get(i);
											if (itm.getType() == ItemType.TERMINAL) {
												Terminal t = (Terminal) itm;
												if (t.getTerminal().equals(":"))
													sdf += "DouaPuncteDz ";
												else
													sdf += "\"" + t.getTerminal() + "\" ";
											} else if (itm.getType() == ItemType.SORT) {
												Sort srt = (Sort) itm;
												// if we are on the first or last place and this sort is not a list, just print the sort
												if (i == 0 || i == items.size() - 1) {
													sdf += StringUtil.escapeSortName(srt.getSortName()) + " ";
												} else {
													// if this sort should be inserted to avoid the priority filter, then add it to the list
													sorts.add(srt);
													sdf += "InsertDz" + StringUtil.escapeSortName(srt.getSortName()) + " ";
												}
											}
										}
										sdf += "-> " + StringUtil.escapeSortName(p.getProdSort().getSortName());
										sdf += getSDFAttributes(p.getAttributes()) + "\n";
									}
									sdf += "} > ";
								}
								sdf = sdf.substring(0, sdf.length() - 3) + "\n\n";
							}
						}
					}
				}
			}
		}

		Set<Subsort> sbs = getSubsorts();
		for (Production p1 : listProds)
			for (Production p2 : listProds)
				if (p1 != p2) {
					Sort srt1 = ((UserList) p1.getItems().get(0)).getSort();
					Sort srt2 = ((UserList) p2.getItems().get(0)).getSort();
					if (sbs.contains(new Subsort(srt1, srt2)))
						subsorts.add(new Subsort(p1.getProdSort(), p2.getProdSort()));
				}

		sdf += "%% subsorts 1\n";
		sdf += "context-free priorities\n{\n";
		// 1
		// print Sort -> K > A -> B > K -> Sort
		for (Sort s : userSorts) {
			if (!s.isBaseSort()) {
				sdf += "	" + StringUtil.escapeSortName(s.getSortName()) + " -> K";
				// sdf += " {cons(\"K12" + StringUtil.escapeSortName(s.getSortName()) + "\")}";
				sdf += "\n";
			}
		}
		sdf += "} > {\n";
		for (Subsort subs : subsorts) {
			Sort s1 = (Sort) subs.getSmallSort();
			Sort s2 = subs.getBigSort();
			if (!s1.isBaseSort() && !s2.isBaseSort()) {
				sdf += "	" + StringUtil.escapeSortName(s1.getSortName()) + " -> " + StringUtil.escapeSortName(s2.getSortName());
				// sdf += " {cons(\"" + StringUtil.escapeSortName(s2.getSortName()) + "12" + StringUtil.escapeSortName(s1.getSortName()) + "\")}";
				sdf += "\n";
			}
		}
		sdf += "} > {\n";
		for (Sort s : userSorts) {
			if (!s.isBaseSort()) {
				sdf += "	K -> " + StringUtil.escapeSortName(s.getSortName());
				// sdf += " {cons(\"" + StringUtil.escapeSortName(s.getSortName()) + "12K\")}";
				sdf += "\n";
			}
		}
		sdf += "}\n\n";

		// TODO: add type warnings option in command line
		if (GlobalSettings.typeWarnings) {
			// 2
			sdf += "%% subsorts 2\n";
			// print Sort -> K > K -> Sort
			for (Sort s : userSorts) {
				if (!s.isBaseSort()) {
					sdf += "context-free priorities\n{\n";
					sdf += "        K -> " + StringUtil.escapeSortName(s.getSortName());
					// sdf += " {cons(\"" + StringUtil.escapeSortName(s.getSortName()) + "12K\")}";
					sdf += "\n";
					sdf += "} .> {\n";
					for (Sort ss : userSorts) {
						if (!ss.isBaseSort() && (ss.equals(s) || sbs.contains(new Subsort(s, ss)))) {
							sdf += "        " + StringUtil.escapeSortName(ss.getSortName()) + " -> K";
							// sdf += " {cons(\"K12" + StringUtil.escapeSortName(ss.getSortName()) + "\")}";
							sdf += "\n";
						}
					}
					sdf += "}\n\n";
				}
			}
		} else {
			// 2
			sdf += "%% subsorts 2\n";
			// print K -> Sort > Sort -> K
			sdf += "context-free priorities\n{\n";
			for (Sort s : userSorts) {
				if (!s.isBaseSort()) {
					sdf += "	K -> " + StringUtil.escapeSortName(s.getSortName());
					// sdf += " {cons(\"" + StringUtil.escapeSortName(s.getSortName()) + "12K\")}";
					sdf += "\n";
				}
			}
			sdf += "} .> {\n";
			for (Sort s : userSorts) {
				if (!s.isBaseSort()) {
					sdf += "	" + StringUtil.escapeSortName(s.getSortName()) + " -> K";
					// sdf += " {cons(\"K12" + StringUtil.escapeSortName(s.getSortName()) + "\")}";
					sdf += "\n";
				}
			}
			sdf += "}\n";
		}

		sdf += "context-free syntax\n";

		for (Production p : outsides) {
			if (p.isListDecl()) {
				UserList si = (UserList) p.getItems().get(0);
				sdf += "	" + StringUtil.escapeSortName(si.getSort().getSortName()) + " \"" + si.getTerminal() + "\" " + StringUtil.escapeSortName(p.getProdSort().getSortName()) + " -> " + StringUtil.escapeSortName(p.getProdSort().getSortName());
				sdf += " {cons(\"" + p.getAttributes().get("cons") + "\")}\n";
				sdf += "	\"." + p.getProdSort().getSortName() + "\" -> " + StringUtil.escapeSortName(p.getProdSort().getSortName());
				sdf += " {cons(\"" + StringUtil.escapeSortName(p.getProdSort().getSortName()) + "1Empty\")}\n";
			} else if (p.getAttributes().containsKey("bracket")) {
				// don't add bracket attributes added by the user
			} else {
				sdf += "	";
				List<Item> items = p.getItems();
				for (int i = 0; i < items.size(); i++) {
					Item itm = items.get(i);
					if (itm.getType() == ItemType.TERMINAL) {
						Terminal t = (Terminal) itm;
						if (t.getTerminal().equals(":"))
							sdf += "DouaPuncteDz ";
						else
							sdf += "\"" + t.getTerminal() + "\" ";
					} else if (itm.getType() == ItemType.SORT) {
						Sort srt = (Sort) itm;
						sdf += StringUtil.escapeSortName(srt.getSortName()) + " ";
					}
				}
				sdf += "-> " + StringUtil.escapeSortName(p.getProdSort().getSortName());
				sdf += getSDFAttributes(p.getAttributes()) + "\n";
			}
		}
		for (Sort ss : sorts)
			sdf += "	" + StringUtil.escapeSortName(ss.getSortName()) + " -> InsertDz" + StringUtil.escapeSortName(ss.getSortName()) + "\n";

		sdf += "\n\n";
		// print variables, HOLEs
		for (Sort s : userSorts) {
			if (!s.isBaseSort()) {
				sdf += "	VARID  \":\" \"" + s.getSortName() + "\"        -> VariableDz            {cons(\"" + StringUtil.escapeSortName(s.getSortName()) + "12Var\")}\n";
			}
		}
		sdf += "\n";
		for (Sort s : userSorts) {
			if (!s.isBaseSort()) {
				sdf += "	\"HOLE\" \":\" \"" + s.getSortName() + "\"      -> VariableDz            {cons(\"" + StringUtil.escapeSortName(s.getSortName()) + "12Hole\")}\n";
			}
		}

		sdf += "\n";
		sdf += "	VariableDz -> K\n";

		sdf += "\n\n";
		sdf += "	DzDzInt		-> DzInt	{cons(\"DzInt1Const\")}\n";
		sdf += "	DzDzBool	-> DzBool	{cons(\"DzBool1Const\")}\n";
		sdf += "	DzDzId		-> DzId		{cons(\"DzId1Const\")}\n";
		sdf += "	DzDzString	-> DzString	{cons(\"DzString1Const\")}\n";
		sdf += "	DzDzFloat	-> DzFloat	{cons(\"DzFloat1Const\")}\n";

		sdf += "\n";
		sdf += "	DzDzINT		-> DzDzInt\n";
		// sdf += "	DzDzID		-> DzDzId\n";
		sdf += "	DzDzBOOL	-> DzDzBool\n";
		sdf += "	DzDzSTRING	-> DzDzString\n";
		sdf += "	DzDzFLOAT	-> DzDzFloat\n";
		sdf += "	\":\" -> DouaPuncteDz {cons(\"DouaPuncte\")}\n";

		sdf += "\n";

		sdf += "context-free restrictions\n";
		sdf += "	VariableDz -/- ~[\\:\\;\\(\\)\\<\\>\\~\\n\\r\\t\\,\\ \\[\\]\\=\\+\\-\\*\\/\\|\\{\\}\\.]\n";
		sdf += "	DouaPuncteDz -/- [A-Z]\n\n";

		sdf += "lexical syntax\n";
		for (Production p : constants) {
			sdf += "	\"" + p.getItems().get(0) + "\" -> Dz" + StringUtil.escapeSortName(p.getProdSort().getSortName()) + "\n";
		}

		sdf += "\n\n%% sort predicates\n";
		// print is<Sort> predicates (actually KLabel)
		for (Sort s : userSorts) {
			sdf += "	\"is" + s.getSortName() + "\"      -> DzKLabel\n";
		}

		sdf += "\n\n";

		sdf += "\n%% terminals reject\n";
		for (Terminal t : getTerminals(false)) {
			if (t.getTerminal().matches("$?[A-Z][^\\:\\;\\(\\)\\<\\>\\~\\n\\r\\t\\,\\ \\[\\]\\=\\+\\-\\*\\/\\|\\{\\}\\.]*")) {
				sdf += "	\"" + t.getTerminal() + "\" -> VARID {reject}\n";
			}
		}

		sdf += "\n";
		sdf += getFollowRestrictionsForTerminals(false);

		sdf += "lexical restrictions\n";
		sdf += "%% some restrictions to ensure greedy matching for user defined constants\n";
		sdf += "	DzDzId  -/- [a-zA-Z0-9]\n";
		sdf += "	DzDzInt -/- [0-9]\n";
		sdf += "	\"is\" -/- [\\#A-Z]\n";

		return sdf + "\n";
	}

	public String getSDFForPrograms() {
		String sdf = "module Program\n\n";
		sdf += "imports Common\n";
		sdf += "imports KBuiltinsBasic\n";
		sdf += "exports\n\n";
		sdf += "context-free syntax\n";

		List<Production> outsides = new ArrayList<Production>();
		List<Production> constants = new ArrayList<Production>();
		Set<Sort> sorts = new HashSet<Sort>(); // list of inserted sorts that need to avoid the priority filter
		Set<Sort> startSorts = new HashSet<Sort>(); // list of sorts that are start symbols
		Set<Sort> listSorts = new HashSet<Sort>(); // list of sorts declared as being list
		Set<Sort> userSort = new HashSet<Sort>(); // list of sorts declared by the user (to be declared later as Start symbols if no declaration for Start was found)

		// gather modules for syntax
		String mainSynModName;
		if (GlobalSettings.synModule == null) {
			mainSynModName = mainModule + "-SYNTAX";
			GlobalSettings.synModule = mainModule + "-SYNTAX";
			if (!this.modulesMap.containsKey(mainSynModName)) {
				mainSynModName = mainModule;
				GlobalSettings.synModule = mainModule;
				GlobalSettings.kem.register(new KException(ExceptionType.HIDDENWARNING, KExceptionGroup.PARSER, "Could not find a specilized module for syntax. Using main module instead.", this.mainFile.getAbsolutePath(), this.mainModule));
			}
		} else
			mainSynModName = GlobalSettings.synModule;
		Module mainSyntax = modulesMap.get(mainSynModName);
		Set<Module> synMods = new HashSet<Module>();
		List<Module> synQue = new LinkedList<Module>();

		synQue.add(mainSyntax);
		// if (mainSyntax == null) {
		// Error.silentReport("Could not find a module for program syntax: " + mainSynModName);
		// } else {

		Module bshm = modulesMap.get("BUILTIN-SYNTAX-HOOKS");
		if (bshm != null)
			synQue.add(bshm);
		else
			Error.silentReport("Could not find module BUILTIN-SYNTAX-HOOKS (automatically included in the main syntax module)!");
		while (!synQue.isEmpty()) {
			Module m = synQue.remove(0);
			if (!synMods.contains(m)) {
				synMods.add(m);
				List<Sentence> ss = m.getSentences();
				for (Sentence s : ss)
					if (s.getType() == SentenceType.INCLUDING) {
						String mname = ((Including) s).getIncludedModuleName();
						Module mm = modulesMap.get(mname);
						// if the module starts with # it means it is predefined in maude
						if (!mname.startsWith("#"))
							if (mm != null)
								synQue.add(mm);
							else if (!MetaK.isKModule(mname))
								Error.silentReport("Could not find module: " + mname + " imported from: " + m.getModuleName());
					}
			}
		}

		for (Module m : synMods) {
			for (Sentence s : m.getSentences()) {
				if (s.getType() == SentenceType.SYNTAX) {
					Syntax syn = (Syntax) s;
					userSort.add(syn.getSort());
					List<Priority> prilist = new ArrayList<Priority>();
					for (Priority prt : syn.getPriorities()) {
						Priority p = new Priority();
						p.setBlockAssoc(prt.getBlockAssoc());

						// filter the productions according to their form
						for (Production prd : prt.getProductions()) {
							if (prd.isSubsort()) {
								outsides.add(prd);
								if (prd.getProdSort().equals(new Sort("Start")))
									startSorts.add((Sort) prd.getItems().get(0));
							} else if (prd.getItems().get(0).getType() == ItemType.TERMINAL && prd.getItems().size() == 1 && prd.getProdSort().getSortName().startsWith("#")) {
								constants.add(prd);
							} else if (prd.getItems().get(0).getType() == ItemType.TERMINAL && prd.getItems().get(prd.getItems().size() - 1).getType() == ItemType.TERMINAL) {
								outsides.add(prd);
							} else if (prd.getItems().get(0).getType() == ItemType.USERLIST) {
								outsides.add(prd);
								listSorts.add(prd.getProdSort());
							} else {
								p.getProductions().add(prd);
							}
						}
						if (p.getProductions().size() > 0)
							prilist.add(p);
					}
					if (prilist.size() > 0) {
						if (prilist.size() <= 1 && syn.getPriorities().get(0).getBlockAssoc() == null) {
							// weird bug in SDF - if you declare only one production in a priority block, it gives parse errors
							// you need to have at least 2 productions or a block association
							Priority prt = prilist.get(0);
							for (Production p : prt.getProductions())
								outsides.add(p);
						} else {
							sdf += "context-free priorities\n";

							for (Priority prt : prilist) {
								if (prt.getBlockAssoc() == null)
									sdf += "{\n";
								else
									sdf += "{ " + prt.getBlockAssoc() + ":\n";
								for (Production p : prt.getProductions()) {
									sdf += "	";
									List<Item> items = p.getItems();
									for (int i = 0; i < items.size(); i++) {
										Item itm = items.get(i);
										if (itm.getType() == ItemType.TERMINAL) {
											Terminal t = (Terminal) itm;
											sdf += "\"" + t.getTerminal() + "\" ";
										} else if (itm.getType() == ItemType.SORT) {
											Sort srt = (Sort) itm;
											// if we are on the first or last place and this sort is not a list, just print the sort
											if (i == 0 || i == items.size() - 1) {
												sdf += StringUtil.escapeSortName(srt.getSortName()) + " ";
											} else {
												// if this sort should be inserted to avoid the priority filter, then add it to the list
												sorts.add(srt);
												sdf += "InsertDz" + StringUtil.escapeSortName(srt.getSortName()) + " ";
											}
										}
									}
									sdf += "-> " + StringUtil.escapeSortName(p.getProdSort().getSortName());
									sdf += getSDFAttributes(p.getAttributes()) + "\n";
								}
								sdf += "} > ";
							}
							sdf = sdf.substring(0, sdf.length() - 3) + "\n\n";
						}
					}
				}
			}
		}

		sdf += "context-free start-symbols\n";
		sdf += "	Start\n";
		sdf += "context-free syntax\n";

		for (Production p : outsides) {
			if (p.isListDecl()) {
				UserList si = (UserList) p.getItems().get(0);
				sdf += "	{" + StringUtil.escapeSortName(si.getSort().getSortName()) + " \"" + si.getTerminal() + "\"}* -> " + StringUtil.escapeSortName(p.getProdSort().getSortName()) + " {cons(\"" + p.getAttributes().get("cons") + "\")}\n";
			} else {
				sdf += "	";
				List<Item> items = p.getItems();
				for (int i = 0; i < items.size(); i++) {
					Item itm = items.get(i);
					if (itm.getType() == ItemType.TERMINAL) {
						Terminal t = (Terminal) itm;
						sdf += "\"" + t.getTerminal() + "\" ";
					} else if (itm.getType() == ItemType.SORT) {
						Sort srt = (Sort) itm;
						sdf += StringUtil.escapeSortName(srt.getSortName()) + " ";
					}
				}
				sdf += "-> " + StringUtil.escapeSortName(p.getProdSort().getSortName());
				sdf += getSDFAttributes(p.getAttributes()) + "\n";
			}
		}
		for (Sort ss : sorts)
			sdf += "	" + StringUtil.escapeSortName(ss.getSortName()) + " -> InsertDz" + StringUtil.escapeSortName(ss.getSortName()) + "\n";

		sdf += "\n%% start symbols\n";
		if (startSorts.size() == 0) {
			for (Sort s : userSort) {
				if (!s.getSortName().equals("Start"))
					sdf += "	" + StringUtil.escapeSortName(s.getSortName()) + "		-> Start\n";
			}
		}

		sdf += "\n\n";
		sdf += "	DzDzInt		-> DzInt	{cons(\"DzInt1Const\")}\n";
		sdf += "	DzDzBool	-> DzBool	{cons(\"DzBool1Const\")}\n";
		sdf += "	DzDzId		-> DzId		{cons(\"DzId1Const\")}\n";
		sdf += "	DzDzString	-> DzString	{cons(\"DzString1Const\")}\n";
		sdf += "	DzDzFloat	-> DzFloat	{cons(\"DzFloat1Const\")}\n";

		sdf += "\n";
		sdf += "	DzDzINT		-> DzDzInt\n";
		sdf += "	DzDzID		-> DzDzId\n";
		sdf += "	DzDzBOOL	-> DzDzBool\n";
		sdf += "	DzDzSTRING	-> DzDzString\n";
		sdf += "	DzDzFLOAT	-> DzDzFloat\n";

		sdf += "\n";

		sdf += "lexical syntax\n";
		for (Production p : constants) {
			sdf += "	\"" + p.getItems().get(0) + "\" -> Dz" + StringUtil.escapeSortName(p.getProdSort().getSortName()) + "\n";
		}

		sdf += "\n\n";

		for (Terminal t : getTerminals(true)) {
			if (t.getTerminal().matches("[a-zA-Z][a-zA-Z0-9]*")) {
				sdf += "	\"" + t.getTerminal() + "\" -> DzDzID {reject}\n";
			}
		}

		sdf += "\n";
		sdf += getFollowRestrictionsForTerminals(true);

		return sdf + "\n";
	}

	public Set<Terminal> getTerminals(boolean syntax) {
		Set<Terminal> termins = new HashSet<Terminal>();
		Set<Module> synMods = new HashSet<Module>();

		if (syntax) {
			List<Module> synQue = new LinkedList<Module>();

			synQue.add(this.modulesMap.get(GlobalSettings.synModule));

			while (!synQue.isEmpty()) {
				Module m = synQue.remove(0);
				if (!synMods.contains(m)) {
					synMods.add(m);
					List<Sentence> ss = m.getSentences();
					for (Sentence s : ss)
						if (s.getType() == SentenceType.INCLUDING) {
							String mname = ((Including) s).getIncludedModuleName();
							Module mm = modulesMap.get(mname);
							// if the module starts with # it means it is predefined in maude
							if (!mname.startsWith("#"))
								if (mm != null)
									synQue.add(mm);
								else if (!MetaK.isKModule(mname))
									Error.silentReport("Could not find module: " + mname + " imported from: " + m.getModuleName());
						}
				}
			}
		} else {
			for (ModuleItem mi : this.modules)
				if (mi.getType() == ModuleType.MODULE)
					synMods.add((Module) mi);
		}

		for (Module m : synMods)
			for (Sentence s : m.getSentences()) {
				if (s.getType() == SentenceType.SYNTAX) {
					Syntax syn = (Syntax) s;
					List<Production> prods = syn.getProductions();

					for (Production p : prods) {
						if (!(p.getProdSort().getSortName().equals("#Id") && p.getItems().size() == 1 && p.getItems().get(0).getType() == ItemType.TERMINAL))
							// reject those terminals that are not declared as #Id
							for (Item i : p.getItems())
								if (i.getType() == ItemType.TERMINAL) {
									termins.add((Terminal) i);
								}
					}
				}
			}
		return termins;
	}

	public String getFollowRestrictionsForTerminals(boolean syntax) {
		Set<Terminal> terminals = getTerminals(syntax);
		Set<Ttuple> mytuples = new HashSet<Definition.Ttuple>();
		String varid = "[A-Z][^:\\;\\(\\)\\<\\>\\~\\n\\r\\t\\,\\ \\[\\]\\=\\+\\-\\*\\/\\|\\{\\}\\.]*";

		for (Terminal t1 : terminals) {
			for (Terminal t2 : terminals) {
				if (t1 != t2) {
					String a = t1.getTerminal();
					String b = t2.getTerminal();
					if (a.startsWith(b)) {
						Ttuple tt = new Ttuple();
						tt.key = a;
						tt.value = b;
						String ending = tt.key.substring(tt.value.length());
						if (ending.matches(varid))
							mytuples.add(tt);
					}
				}
			}
		}

		String sdf = "lexical restrictions\n";
		sdf += "	%% follow restrictions\n";
		for (Ttuple tt : mytuples) {
			sdf += "	\"" + tt.value + "\" -/- ";
			String ending = tt.key.substring(tt.value.length());
			for (int i = 0; i < ending.length(); i++) {
				String ch = "" + ending.charAt(i);
				if (ch.matches("[a-zA-Z]"))
					sdf += "[" + ch + "].";
				else
					sdf += "[\\" + ch + "].";
			}
			sdf = sdf.substring(0, sdf.length() - 1) + "\n";
		}

		return sdf;
	}

	/**
	 * Using this class to collect the prefixes amongst terminals
	 * 
	 * @author RaduFmse
	 * 
	 */
	public class Ttuple {
		public String key;
		public String value;

		public boolean equals(Object o) {
			if (o.getClass() == Ttuple.class)
				return false;
			Ttuple tt = (Ttuple) o;
			if (key.equals(tt.key) && value.equals(tt.value))
				return true;
			return false;
		}

		public int hashCode() {
			return key.hashCode() + value.hashCode();
		}
	}

	public Map<Production, List<Production>> getProductionDittos() {
		Map<Production, List<Production>> prods2 = new HashMap<Production, List<Production>>();
		for (ModuleItem mi : modules)
			if (mi.getType() == ModuleType.MODULE) {
				Module m = (Module) mi;
				for (Sentence s : m.getSentences()) {
					if (s.getType() == SentenceType.SYNTAX) {
						Syntax syn = (Syntax) s;
						List<Production> prods = syn.getProductions();

						for (Production p : prods) {
							if (!p.isSubsort()) {
								Production p2 = p.clone();// (Production) Cloner.copy(p);
								p2.collapseSorts();
								if (prods2.containsKey(p2)) {
									List<Production> lprod = prods2.get(p2);
									lprod.add(p);
								} else {
									List<Production> lprod = new ArrayList<Production>();
									lprod.add(p);
									prods2.put(p2, lprod);
								}
							}
						}
					}
				}
			}

		return prods2;
	}

	public Set<Subsort> getSubsorts() {
		// collect the existing subsorting, and add the default ones
		Set<Subsort> sbs = Subsort.getDefaultSubsorts();
		for (ModuleItem mi : modules)
			if (mi.getType() == ModuleType.MODULE) {
				Module m = (Module) mi;
				for (Sentence s : m.getSentences()) {
					if (s.getType() == SentenceType.SYNTAX) {
						Syntax syn = (Syntax) s;
						if (!syn.getSort().isBaseSort())
							sbs.add(new Subsort(new Sort("K"), syn.getSort(), syn.getSort().getFilename(), syn.getSort().getLocation()));
						for (Production p : syn.getProductions()) {
							if (p.isSubsort()) {
								// this is a subsort, add it to the list
								Sort s2 = (Sort) p.getItems().get(0);
								sbs.add(new Subsort(syn.getSort(), s2, s2.getFilename(), s2.getLocation()));
							} else if (p.isListDecl()) {
								UserList ul = (UserList) p.getItems().get(0);
								sbs.add(new Subsort(ul.getSort(), p.getProdSort(), ul.getFilename(), ul.getLocation()));
							}
						}
					}
				}
			}

		// closure for sorts
		boolean finished = false;
		while (!finished) {
			finished = true;
			Set<Subsort> ssTemp = new HashSet<Subsort>();
			for (Subsort s1 : sbs) {
				for (Subsort s2 : sbs) {
					if (s1.getBigSort().equals(s2.getSmallSort())) {
						Subsort sTemp = new Subsort(s2.getBigSort(), s1.getSmallSort(), "Transitive Closure", "(0,0,0,0)");
						if (!sbs.contains(sTemp)) {
							ssTemp.add(sTemp);
							finished = false;
						}
					}
				}
			}
			sbs.addAll(ssTemp);
		}

		return sbs;
	}

	public String getSubsortingAsStrategoTerms() {
		Set<Subsort> sbs = this.getSubsorts();
		String term = "[  ";
		for (Subsort ss : sbs) {
			term += "(\"" + ss.getBigSort() + "\", \"" + ss.getSmallSort() + "\")\n, ";
		}

		return term.substring(0, term.length() - 2) + "]";
	}

	public String getConsAsStrategoTerms() {
		String str = "[  ";
		for (ModuleItem mi : modules)
			if (mi.getType() == ModuleType.MODULE) {
				Module m = (Module) mi;
				for (Sentence s : m.getSentences()) {
					if (s.getType() == SentenceType.SYNTAX) {
						Syntax syn = (Syntax) s;
						for (Production p : syn.getProductions()) {
							if (p.getItems().size() > 1 || p.getItems().get(0).getType() == ItemType.TERMINAL || p.isListDecl()) {
								// if it contains at least one non-terminal - add it to the list
								boolean hasNonTerminal = false;
								String tempStr = "(\"" + p.getAttributes().get("cons") + "\",   \"" + syn.getSort() + "\", [";

								for (Item i : p.getItems()) {
									if (i.getType() == ItemType.SORT) {
										hasNonTerminal = true;
										tempStr += "\"" + i + "\", ";
									}
								}
								if (hasNonTerminal) {
									str += tempStr.substring(0, tempStr.length() - 2) + "])\n, ";
								} else
									str += tempStr + "])\n, ";
							}
						}
					}
				}
			}

		return str.substring(0, str.length() - 2) + "]";
	}

	private String getSDFAttributes(Map<String, String> attrs) {
		String str = " {";
		if (attrs.size() == 0)
			return "";

		if (attrs.containsKey("prefer"))
			str += "prefer, ";
		if (attrs.containsKey("avoid"))
			str += "avoid, ";
		if (attrs.containsKey("left"))
			str += "left, ";
		if (attrs.containsKey("right"))
			str += "right, ";
		if (attrs.containsKey("non-assoc"))
			str += "non-assoc, ";
		if (attrs.containsKey("bracket"))
			str += "bracket, ";
		if (attrs.containsKey("cons"))
			str += "cons(\"" + attrs.get("cons") + "\"), ";

		if (str.endsWith(", "))
			return str.substring(0, str.length() - 2) + "}";
		else
			return str + "}";
	}

	public File getMainFile() {
		return mainFile;
	}

	public void setMainFile(File mainFile) {
		this.mainFile = mainFile;
	}

	public List<Sentence> getAllSentences() {
		List<Sentence> sts = new ArrayList<Sentence>();
		for (ModuleItem mi : modules)
			if (mi.getType() == ModuleType.MODULE) {
				Module m = (Module) mi;
				sts.addAll(m.getSentences());
			}
		return sts;
	}

	public String getCellsFromConfigAsStrategoTerm() {
		List<Sentence> sts = this.getAllSentences();
		Map<String, Cell> cells = new HashMap<String, Cell>();
		for (Sentence s : sts) {
			if (s.getType() == SentenceType.CONFIGURATION) {
				Configuration c = (Configuration) s;
				c.parse();
				cells.putAll(c.getCellLabels());
			}
		}

		// String term = "[  ";
		// for (Map.Entry<String, Cell> c : cells.entrySet()) {
		// term += "(\"" + c.getKey() + "\", \"" + c.getValue().getSort() + "\")\n, ";
		// DefinitionHelper.cells.put(c.getKey(), c.getValue().getSort());
		// }
		//
		// return term.substring(0, term.length() - 2) + "]";
		return "[]";
	}

	public void parseRules() {
		List<Sentence> sts = this.getAllSentences();
		for (Sentence s : sts) {
			if (s.getType() == SentenceType.RULE) {
				Rule r = (Rule) s;
				r.parse();
			} else if (s.getType() == SentenceType.CONTEXT) {
				Context c = (Context) s;
				c.parse();
			}
		}
	}

	public Document getDefAsXML() {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();

			Element el = doc.createElement("def");

			el.setAttribute("mainFile", this.mainFile.getCanonicalPath());
			el.setAttribute("mainModule", this.mainModule);

			for (ModuleItem mi : modules)
				el.appendChild(doc.importNode(mi.getXmlTerm(), true));

			doc.appendChild(el);

			return doc;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (DOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void makeConsLists() throws Exception {
		for (ModuleItem mi : modules)
			if (mi.getType() == ModuleType.MODULE) {
				Module m = (Module) mi;
				for (Sentence s : m.getSentences()) {
					if (s.getType() == SentenceType.SYNTAX) {
						Syntax syn = (Syntax) s;
						for (Priority pry : syn.getPriorities()) {
							List<Production> prods = pry.getProductions();

							for (Production p : prods) {
								if (p.isListDecl()) {
									// this is the list decl
									UserList sl = (UserList) p.getItems().get(0);

									p.getItems().clear(); // clear the production
									p.getItems().add(p.getProdSort()); // and replace it with the cons list
									p.getItems().add(new Terminal(sl.getTerminal()));
									p.getItems().add(p.getProdSort());
									p.getAttributes().put("right", "");

									Production sbs = new Production(); // also add the element subsorted to the list sort
									sbs.setProdSort(p.getProdSort());
									sbs.getItems().add(sl.getSort());
									prods.add(sbs);

									Production idElem = new Production(); // also add the identity element
									idElem.setProdSort(p.getProdSort());
									idElem.getItems().add(new Terminal("." + p.getProdSort().getSortName()));
									String cons = p.getAttributes().get("cons");
									if (!cons.endsWith("ListSyn"))
										throw new Exception("Why isn't this cons ending in ListSyn: " + cons);
									cons = cons.substring(0, cons.length() - "ListSyn".length());
									idElem.getAttributes().put("cons", cons + "Empty");
									prods.add(idElem);
									break;
								}
							}
						}
					}
				}
			}
	}

	public void addConsToProductions() {
		// add cons to productions that don't have it already
		for (ModuleItem mi : modules)
			if (mi.getType() == ModuleType.MODULE) {
				Module m = (Module) mi;
				for (Sentence s : m.getSentences()) {
					if (s.getType() == SentenceType.SYNTAX) {
						Syntax syn = (Syntax) s;
						for (Priority pry : syn.getPriorities()) {
							List<Production> prods = pry.getProductions();

							for (Production p : prods) {
								if (p.getAttributes().containsKey("bracket")) {
									// don't add cons to bracket production
									String cons = p.getAttributes().get("cons");
									if (cons != null)
										GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, "'bracket' productions are not allowed to have cons: '" + cons + "'", p.getFilename(), p.getLocation()));
								} else if (p.getItems().size() == 1 && p.getItems().get(0).getType() == ItemType.TERMINAL && p.getProdSort().getSortName().startsWith("#")) {
									// don't add any cons, if it is a constant
									// a constant is a single terminal for a builtin sort
									String cons = p.getAttributes().get("cons");
									if (cons != null)
										GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, "Constants are not allowed to have cons: '" + cons + "'", p.getFilename(), p.getLocation()));
								} else if (p.isSubsort()) {
									// cons are not allowed for subsortings
									String cons = p.getAttributes().get("cons");
									if (cons != null)
										GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, "Subsortings are not allowed to have cons: '" + cons + "'", p.getFilename(), p.getLocation()));
								} else {
									if (!p.getAttributes().containsKey("cons")) {
										String cons;
										if (p.isListDecl())
											cons = StringUtil.escapeSortName(p.getProdSort().getSortName()) + "1" + "ListSyn";
										else
											cons = StringUtil.escapeSortName(p.getProdSort().getSortName()) + "1" + StringUtil.getUniqueId() + "Syn";
										p.getAttributes().put("cons", cons);
										Element el = p.xmlTerm.getOwnerDocument().createElement("tag");
										el.setAttribute("key", "cons");
										el.setAttribute("loc", "generated");
										el.setAttribute("value", cons);

										Node attributes = p.xmlTerm.getLastChild().getPreviousSibling();

										// if the production doesn't have an attributes tag, add it manually
										if (!attributes.getNodeName().equals(Tag.attributes)) {
											Element attributes2 = p.xmlTerm.getOwnerDocument().createElement(Tag.attributes);
											p.xmlTerm.appendChild(attributes2);
											attributes = attributes2;
										}

										attributes.appendChild(el);
									} else {
										// check if the provided cons is correct
										String cons = p.getAttributes().get("cons");
										String escSort = StringUtil.escapeSortName(p.getProdSort().getSortName());

										if (!cons.startsWith(escSort))
											GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, "The cons attribute must start with '" + escSort + "' and not with " + cons, p.getFilename(), p.getLocation()));
										if (!cons.endsWith("Syn")) // a normal cons must end with 'Syn'
											GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, "The cons attribute must end with 'Syn' and not with " + cons, p.getFilename(), p.getLocation()));
										if (p.isListDecl() && !cons.endsWith("ListSyn")) // if this is a list, it must end with 'ListSyn'
											GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, "The cons attribute must end with 'ListSyn' and not with " + cons, p.getFilename(), p.getLocation()));
									}
								}
							}
						}
					}
				}
			}
	}

	public void setMainModule(String mainModule) {
		this.mainModule = mainModule;
		if (!this.modulesMap.containsKey(this.mainModule))
			GlobalSettings.kem.register(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, KMessages.ERR1002 + mainModule + ". Use --lang <arg> to specify another.", this.mainFile.getName(), "definition"));
	}

	public String getMainModule() {
		return mainModule;
	}
}
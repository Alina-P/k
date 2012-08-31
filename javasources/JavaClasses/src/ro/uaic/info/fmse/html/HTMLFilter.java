package ro.uaic.info.fmse.html;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.uaic.info.fmse.html.HTMLPatternsVisitor.HTMLPatternType;
import ro.uaic.info.fmse.k.Attribute;
import ro.uaic.info.fmse.k.Attributes;
import ro.uaic.info.fmse.k.Cell;
import ro.uaic.info.fmse.k.Collection;
import ro.uaic.info.fmse.k.Configuration;
import ro.uaic.info.fmse.k.Constant;
import ro.uaic.info.fmse.k.Context;
import ro.uaic.info.fmse.k.Definition;
import ro.uaic.info.fmse.k.Empty;
import ro.uaic.info.fmse.k.Hole;
import ro.uaic.info.fmse.k.KApp;
import ro.uaic.info.fmse.k.KSequence;
import ro.uaic.info.fmse.k.ListOfK;
import ro.uaic.info.fmse.k.LiterateComment.LiterateCommentType;
import ro.uaic.info.fmse.k.LiterateDefinitionComment;
import ro.uaic.info.fmse.k.LiterateModuleComment;
import ro.uaic.info.fmse.k.MapItem;
import ro.uaic.info.fmse.k.Module;
import ro.uaic.info.fmse.k.Production;
import ro.uaic.info.fmse.k.ProductionItem;
import ro.uaic.info.fmse.k.ProductionItem.ProductionType;
import ro.uaic.info.fmse.k.Rewrite;
import ro.uaic.info.fmse.k.Rule;
import ro.uaic.info.fmse.k.Sort;
import ro.uaic.info.fmse.k.Syntax;
import ro.uaic.info.fmse.k.Term;
import ro.uaic.info.fmse.k.TermComment;
import ro.uaic.info.fmse.k.TermCons;
import ro.uaic.info.fmse.k.Terminal;
import ro.uaic.info.fmse.k.UserList;
import ro.uaic.info.fmse.k.Variable;
import ro.uaic.info.fmse.loader.Constants;
import ro.uaic.info.fmse.loader.DefinitionHelper;
import ro.uaic.info.fmse.visitors.BasicVisitor;

public class HTMLFilter extends BasicVisitor {
	String endl = System.getProperty("line.separator");
	private String result = "";
	private String css = "";
	private String preamble = "";
	private String title = "";
	private String author = "";
	private String organization = "";
	
	// this set indicates which color classes have already been added to the css string
	private HashSet<String> usedColors = new HashSet<String>();
	
	// keys : name of a cell -> values : color of that cell
	private Map<String, String> cellColors = new HashMap<String,String>();
	
	// keys : name of a standard HTML5 color -> values : java.awt.Color representation of that color
	// this is created in the constructor of the HTMLFilter class
	private Map<String,Color> HTMLColors = new HashMap<String,Color>();
	
	private HTMLPatternsVisitor patternsVisitor = new HTMLPatternsVisitor();
	
	private boolean firstAttribute;
	private boolean parentParens = false;
	private boolean firstProduction = false;
	
	private Properties Latex2HTMLzero = new Properties();
	private Properties Latex2HTMLone = new Properties();
	private String includePath = new String();
	
	public HTMLFilter(String includePath) {
		this.includePath = includePath;
		initializeCss();
		createHTMLColors();
		loadProperties();
	}
	
	public String getHTML() {
		// adds the headers and the css to the result to create the complete HTML code for the page
		parsePreamble();
		String html = 
			"<!DOCTYPE html>" + endl + 
			"<html lang=\"en\">" + endl + 
			"<head>" + endl + 
			"	<title>" + title + "</title>" + endl + 
			"	<style>" + endl + 
			css + 
			"	</style>" + endl + 
			// this file is maybe not encoded in utf-8...
			"	<meta charset=\"utf-8\" />" + endl + 
			// MathJax->
			"<script type=\"text/javascript\" " + endl +
			"src=\"http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML\">" + endl +
			"</script>" + endl +
			// <-MathJax
			"</head>" + endl + 
			"<body>" + endl;
		html += 
			result +
			"</body>" + endl +
			"</html>" + endl;
		return html;
	}

	public void setResult(String result) {
		this.result = result;
	}
	
	public String getResult() {
		return result;
	}

	public void setPreamble(String preamble) {
		this.preamble = preamble;
	}

	public String getPreamble() {
		return preamble;
	}
	
	public boolean isParentParens() {
		return parentParens;
	}

	private void setParentParens(boolean parentParens) {
		this.parentParens = parentParens;
	}

	@Override
	public void visit(Definition def) {
		def.accept(patternsVisitor);
		title = def.getMainModule();
		super.visit(def);

	}

	@Override
	public void visit(Module mod) {
		if (mod.isPredefined())
			return;
		result += "<div>" + "MODULE <span class=\"large\">" + mod.getName() + "</span> <br /> <br />" + endl;
		super.visit(mod);
		result += "END MODULE </div>" + endl + "<br />" + endl;
	}

	@Override
	public void visit(Syntax syn) {
		// These are rendered using a table to position each symbol nicely.
		result += "<table> <tr> <td> SYNTAX ";
		firstProduction = true;
		super.visit(syn);
		result += "</table>" + endl + "<br />" + endl;
	}

	@Override
	public void visit(Sort sort) {
		result += "<span class =\"italic\"> " + makeGreek(sort.getName()) + " </span>";
	}

	@Override
	public void visit(Production p) {
		if (firstProduction) {
			result += "</td><td> ::= </td> <td>";
			firstProduction = false;
		} else {
			result += "<tr> <td> </td> <td class = \"textCentered\"> |  </td> <td>";
		}
		
		if (p.getItems().get(0).getType() != ProductionType.USERLIST && p.getAttributes().containsKey(Constants.CONS_cons_ATTR) 
				&& patternsVisitor.getPatterns().containsKey(p.getAttributes().get(Constants.CONS_cons_ATTR))
				&& patternsVisitor.getPatternType(p.getAttributes().get(Constants.CONS_cons_ATTR)) != HTMLPatternType.DEFAULT) {
		/* This condition pretty much is : "Does this production have a Latex or HTML attribute?"
		 * If yes, use the attribute to print it. 
		 * The information about the attribute is in HTMLPatternVisitor.
		 * If no, just process it normally, that is super.visit(p) and process each element normally.*/
			
			String pattern = patternsVisitor.getPatterns().get(p.getAttributes().get(Constants.CONS_cons_ATTR));
			boolean isLatex = patternsVisitor.getPatternType(p.getAttributes().get(Constants.CONS_cons_ATTR)) == HTMLPatternType.LATEX;
			int n = 1;
			HTMLFilter termFilter = new HTMLFilter(includePath);
			for (ProductionItem pi : p.getItems()) {
				if (pi.getType() != ProductionType.TERMINAL) {
					termFilter.result = "";
					pi.accept(termFilter);
					pattern = pattern.replace("{#" + n++ + "}", isLatex ? "\\)" + termFilter.getResult() + "\\(" : termFilter.getResult());
				}
			}
			/* The \( and \) symbols are used to indicate which portions of the code should be 
			 * treated by MathJax. */
			result += isLatex ? "\\(" + pattern + "\\)" : pattern;
		} else {
			super.visit(p);
		}
		p.getAttributes().accept(this);
		result += "</td> </tr>" + endl;
	}

	@Override
	public void visit(Terminal pi) {
		result += makeGreek(htmlify(pi.getTerminal())) +" ";
	}

	@Override
	public void visit(UserList ul) {
		result += "<span class =\"italic\">" + "List</span>{#<span class =\"italic\">" + ul.getSort() + "</span>,\"" + ul.getSeparator() + "\"} </span>"  + endl;
	}

	@Override
	public void visit(Configuration conf) {
		result += "<div> CONFIGURATION : <br />";
		super.visit(conf);
		result += "</div> <br />" ;
	}

	@Override
	public void visit(Cell c) {
		String blockClasses = "block";
		String tabClasses = "tab";
		
		//this condition checks how the edges of the cell should be.
		if (c.getElipses().equals("left")) {
			blockClasses += " left";
			tabClasses += " straightEdge";
		} else if (c.getElipses().equals("right")) {
			blockClasses += " right";
			tabClasses += " curvedEdge";
		} else if (c.getElipses().equals("both")) {
			blockClasses += " left right";
			tabClasses += " straightEdge";
		} else {
			tabClasses += " curvedEdge";
		}
		
		// This condition checks the color of the cell.
		if (c.getAttributes().containsKey("color") && HTMLColors.containsKey(c.getAttributes().get("color").toLowerCase())) {
			cellColors.put(c.getLabel(), c.getAttributes().get("color").toLowerCase());
		}
		/* If the cell does not have a color attribute or a recognizable name for that color,
		 * getCellColor() returns "default" -> (black and white). */
		String cellName = makeGreek(htmlify(c.getLabel()));
		String color = getCellColor(cellName);
		
		// If the color has not been used yet, the css string has to be updated.
		if(usedColors.add(color))
			addToCss(color);
		
		result += "<div class=\"cell\"> <div class=\"" + tabClasses + " " + color + "\">";
		result += "<span class = \"bold\">" + cellName + "</span> </div>";
		result += "<br />";
		result += "<div class=\"" + blockClasses + " " + color + "\" ";
		
		/* This condition makes sure the cell and the tag of the cell 
		are not too small for their content */
		if(cellName.length() > 5) {
			double mw = Math.floor(cellName.length() * 0.7 *10 +0.5 )/10.0;
			result += "style=\"min-width:"+mw+"em\"";
		}
			
		result += "> <div class=\"center\">";
		super.visit(c);
		result += "</div> </div> </div>" + endl;
	}

	public void visit(Collection col) {
		List<Term> contents = col.getContents();
		printList(contents, "");
	}
	
	private void printList(List<Term> contents, String str) {
		boolean first = true;
		for (Term trm : contents) {
			if (first) {
				first = false;
			} else {
				result += str;
			}
			trm.accept(this);
		}
	}
	
	public void visit(TermComment tc) {
		result += "<br />";
		super.visit(tc);
	}

	@Override
	public void visit(Variable var) {
		result +="<span ";
		if (var.getSort() != null) {
			result += "title =\"" + var.getSort() + "\"";
		}
		result+=">" + makeGreek(var.getName());
		result+=" </span> ";
	}

	@Override
	public void visit(Empty e) {
		result += " <span title=\""+ e.getSort()+"\" class = \"bold\"> &nbsp;&nbsp;&middot;&nbsp;&nbsp;</span> ";
	}

	@Override
	public void visit(Rule rule) {
		result += "<div> <span ";
		if (!rule.getLabel().equals("")) {
			result += "title =\"Rule Label: " + rule.getLabel() + "\"";
		}
		result += "> RULE </span>";
		result += "<div class=\"cell\"> ";
		rule.getBody().accept(this);
		result += "</div> ";
		if (rule.getCondition() != null) {
			result += " when ";
			rule.getCondition().accept(this);
		}
		rule.getAttributes().accept(this);
		result += "</div> <br />" + endl;
	}

	@Override
	public void visit(Context cxt) {
		result += "<div> CONTEXT ";
		cxt.getBody().accept(this);
		if (cxt.getCondition() != null) {
			result += " when ";
			cxt.getCondition().accept(this);
		}
		cxt.getAttributes().accept(this);
		result += "</div> <br />" + endl;
	}

	@Override
	public void visit(Hole hole) {
		result += "&#9633;";
	}

	@Override
	public void visit(Rewrite rew) {
		
		result += "<div class=\"textCentered\"> <em> ";
		rew.getLeft().accept(this);
		result += "</em> <hr class=\"reduce\"/> <em> ";
		rew.getRight().accept(this);
		result += "</em> </div>";
	}

	@Override
	public void visit(TermCons trm) {
		HTMLPatternType type = patternsVisitor.getPatternType(trm.getCons());
		if(type == null)
		{
			Production pr = DefinitionHelper.conses.get(trm.getCons());
			pr.accept(patternsVisitor);
			type = patternsVisitor.getPatternType(trm.getCons());
		}
		/* This condition pretty much is : "Does this term have a Latex or HTML attribute?" */
		if(type != HTMLPatternType.DEFAULT) {
			
		/* If yes, use the attribute to print it. 
		 * The information about the attribute is in HTMLPatternVisitor. */
			String pattern = patternsVisitor.getPatterns().get(trm.getCons());
			String regex = "\\{#\\d+\\}$";
			Pattern p = Pattern.compile(regex);
			if (parentParens && (pattern.indexOf("{#") == 0 
					|| p.matcher(pattern).matches())) {
				pattern = "(" + pattern + ")";
			}		
			int n = 1;
			HTMLFilter termFilter = new HTMLFilter(includePath);
			for (Term t : trm.getContents()) {
				termFilter.result = "";
				regex = "\\{#\\d+\\}\\{#" + n + "\\}";
				p = Pattern.compile(regex);
				if (pattern.contains("{#" + n + "}{#") || p.matcher(pattern).matches()) {
					termFilter.setParentParens(true);				
				}
				t.accept(termFilter);
				if(type == HTMLPatternType.LATEX)
					pattern = pattern.replace("{#" + n++ + "}", "\\) " + termFilter.getResult() + "\\(");
				else
					pattern = pattern.replace("{#" + n++ + "}", termFilter.getResult());
			}
			/* The \( and \) symbols are used to indicate which portions of the code should be 
			 * treated by MathJax.*/
			if(type == HTMLPatternType.LATEX)
				result += "\\("+pattern+"\\)";
			else
				result += pattern;
			
		} else {
			/* If the termCons does not have a Latex or HTML attribute, 
			 * the term is printed by using the informations in the termCons's
			 * production and in its list of terms (contents). */
			boolean empty = true;
			Production pr = trm.getProduction();
	
			if (pr.getItems().size() > 0) {
				if (pr.getItems().get(0).getType() == ProductionType.USERLIST) {
					String separator = ((UserList) pr.getItems().get(0)).getSeparator();
					trm.getContents().get(0).accept(this);
					result += " " + separator + " ";
					trm.getContents().get(1).accept(this);
					result += " ";
					empty = false;
				} else
					for (int i = 0, j = 0; i < pr.getItems().size(); i++) {
						ProductionItem pi = pr.getItems().get(i);
						if (pi.getType() == ProductionType.TERMINAL) {
							pi.accept(this);
							empty = false;
						} else if (pi.getType() == ProductionType.SORT) {
							Term t = trm.getContents().get(j++);
							t.accept(this);	
							empty = false;
						}
					}
			}
			if(empty)
				result += "&nbsp; &nbsp; &middot; &nbsp; &nbsp;";
		}
	}

	@Override
	public void visit(Constant c) {
		result += "<span title =\"" + c.getSort() + "\"> " + makeGreek(c.getValue()) + " </span> ";
	}

	@Override
	public void visit(MapItem mi) {
		mi.getKey().accept(this);
		result += "<span text-size=\"large\"> &#x21a6; </span>";
		mi.getItem().accept(this);
	}

	@Override
	public void visit(KSequence k) {
		printList(k.getContents(), "&#x219d; ");
	}

	@Override
	public void visit(KApp app) {
		app.getLabel().accept(this);
		result += "(";
		app.getChild().accept(this);
		result += ")";
	}

	@Override
	public void visit(ListOfK list) {
		printList(list.getContents(), "<span class=\"bold\">, </span>");
	}

	@Override
	public void visit(LiterateDefinitionComment comment) {
		/*MathJax does not render these comments correctly. 
		 * It's told explicitly to ignore them with the tex2jax_ignore class. */
		if (comment.getType() == LiterateCommentType.LATEX) {
			result += "<div class=\"commentBlock definitionComment tex2jax_ignore\">" + endl;
			result += parseComment(comment.getValue());
			result += "</div><div><br /></div>" + endl;
		} else if (comment.getType() == LiterateCommentType.PREAMBLE) {
			preamble += comment.getValue();
		}
	}

	@Override
	public void visit(LiterateModuleComment comment) {
		/*MathJax does not render these comments correctly. 
		 * It's told explicitly to ignore them with the tex2jax_ignore class. */
		if (comment.getType() == LiterateCommentType.LATEX) {
			result += "<div class=\"commentBlock moduleComment tex2jax_ignore\">" + endl;
			result += parseComment(comment.getValue());
			result += "</div><div><br /></div>" + endl;
		} else if (comment.getType() == LiterateCommentType.PREAMBLE) {
			preamble += comment.getValue();
		}
	}
	
	@Override
	public void visit(Attributes attributes) {
		firstAttribute = true;
		for (Attribute entry : attributes.getContents()) {
			entry.accept(this);
		}
		if(!firstAttribute)
			result += "</span> ]";
	}

	@Override
	public void visit(Attribute entry) {
		if (DefinitionHelper.isTagGenerated(entry.getKey()))
			return;
		if (DefinitionHelper.isParsingTag(entry.getKey()))
			return;
		
		// The latex and/or html attributes are processed in the HTMLPatternVisitor, not here.
		if (entry.getKey().equals("latex"))
			return;
		if (entry.getKey().equals("html")) {
			return;
		}
			
		
		if (firstAttribute) {
			result += " [ <span class =\"attribute\"> ";
			firstAttribute = false;
		} else {
			result += ", ";
		}
		result += makeGreek(entry.getKey());
		String value = makeGreek(entry.getValue());
		
		if (!value.isEmpty())
			result += "(" + value + ")";
	}
	
	private String makeGreek(String name) {
		/* Replaces Greek characters by their HTML representation */
		return name.replace("Alpha", "&alpha;").replace("Beta", "&beta;").replace("Gamma", "&gamma;").replace("Delta", "&delta;").replace("VarEpsilon", "&vepsilon;").replace("Epsilon", "&epsilon;").replace("Zeta", "&zeta;").replace("Eta", "&eta;")
				.replace("Theta", "&theta;").replace("Kappa", "&kappa;").replace("Lambda", "&lambda;").replace("Mu", "&mu;").replace("Nu", "&nu;").replace("Xi", "&xi;").replace("Pi", "&pi;").replace("VarRho", "&varrho;").replace("Rho", "&rho;")
				.replace("VarSigma", "&vsigma;").replace("Sigma", "&sigma;").replace("GAMMA", "&Gamma;").replace("DELTA", "&Delta;").replace("THETA", "&Theta;").replace("LAMBDA", "&Lambda;").replace("XI", "&Xi;").replace("PI", "&Pi;")
				.replace("SIGMA", "&Sigma;").replace("UPSILON", "&Upsilon;").replace("PHI", "&Phi;");
	}
	
	private String htmlify(String name) {
		return name.replace("<", "&lt;");
	}
	
	private String HTMLColorToString(Color a) {
		// This function is used to output a Color in the html #RRGGBB format.
		return "#" + toHex(a.getRed()) + toHex(a.getGreen()) + toHex(a.getBlue());
	}
	
	private String toHex(int c) {
		/* This function expects an integer between 0-255.
		 * It returns it's hexadecimal representation.
		 */
		if(0 <= c && c <= 15)
			return "0" + Integer.toHexString(c);
		else if(16 <= c && c <= 255)
			return Integer.toHexString(c);
		else
			return "ERROR in String toHex(int c), c = "+c;
	}
	
	private String parseComment(String comment) {
		/* This function parses the comments and transforms their Latex instructions to html
		 * using the config files loaded in the Latex2HTMLone and Latex2HTMLzero Properties.
		 * 
		 * These config files are in /dist/include/html for now.
		 * */
		
		/* This loop takes care of the latex instructions that contain arguments.
		 * These arguments are found by multipleLatexExtracts(). They can then be used to replace 
		 * the latex instruction by the corresponding html representation.  */
		for (Enumeration e = Latex2HTMLone.keys() ; e.hasMoreElements() ;) {
			String key = (String) e.nextElement();
			if(key != null) {
				
				Vector<Integer> startIndexs = findStartIndexs(key);
				int numberOfIndexs = startIndexs.size();
				Vector<Vector<String>> contents = multipleLatexExtracts(comment,regexify(key),startIndexs);
				for(Vector<String> c : contents) {
					String copyKey = key;
					String property = Latex2HTMLone.getProperty(key);
					for(int i = 1; i < numberOfIndexs+1; i++) {
						copyKey = copyKey.replace("#"+i, c.get(i-1));
						property = property.replace("#"+i,c.get(i-1));
					}
					comment = comment.replace(copyKey,property);
				}
			}
				
	     }
		
		/* This loop takes care of the latex instructions take can be transformed to HTML by a direct replacement.  */
		for (Enumeration e = Latex2HTMLzero.keys() ; e.hasMoreElements() ;) {
			String key = (String) e.nextElement();
			
			if(key != null)
				comment = comment.replace(key,Latex2HTMLzero.getProperty(key));	
	     }
		return comment;
	}
	
	private Vector<Vector<String>> multipleLatexExtracts(String from, String regex, Vector<Integer> startIndexs) {
		/* This function uses regex to match the begin of each latex instruction
		 * in the text contained in the String from. It then extracts the argument(s) of 
		 * each instruction found by matching opening and closing curly braces by a linear
		 * search in the string. The startIndexs are the places where it should start it's search
		 * for each argument.
		 *  */
		/* The return argument means : 
		 * outside Vector -> each extract 
		 * inside Vector  -> the different strings of an extract
		 * */
		Vector<Vector<String>> results = new Vector<Vector<String>>();
		Pattern p = Pattern.compile(regex,Pattern.DOTALL);
		Matcher m = p.matcher(from);
		while(m.find()) {
			/* The offset is used in extracts where there is more than one
			 * argument. It takes into account the length of the previous arguments. */ 
			int offset = 0;
			if(!m.group().isEmpty()) {
				Vector<String> contents = new Vector<String>();
				for(int start : startIndexs) {
					
					int a = m.start()+start+offset;
					int i = a;
					for(int braceCount = 1; braceCount > 0 && i < from.length(); i++) {
						if(from.charAt(i) == '{')
								braceCount++;
						else if (from.charAt(i) == '}')
							braceCount--;
					}
					contents.add(from.substring(a,i-1));
					offset += from.substring(a,i-1).length() - 2;
				}
				results.add(contents);
			}
		}
		return results;
	}
	
	private Vector<Integer> findStartIndexs(String from) {
		// The startIndexs are the positions of the different #1, #2, ... in the string.
		Vector<Integer> result = new Vector<Integer>();
		for(int i = 1; from.contains("#"+i); i++) {
				result.add(from.indexOf("#"+i));
		}
		return result;
	}
	
	
	private String regexify(String regex) {
		String a = regex;
		for(int i = 1; a.contains("#" + i); i++)
			a = a.replace("#"+i, ".*?");
		
		return a.replace("\\", "\\\\").replace("{","\\{").replace("}", "\\}");
	}
	
	private Color alter(Color a ) {
		// This makes the color lighter to make it a suitable background for a cell.
		float hsb[] = Color.RGBtoHSB(a.getRed(), a.getGreen(), a.getBlue(), null);
		hsb[1] /= 4;
		hsb[2] = (float) (240.0/255.0);
		return new Color( Color.HSBtoRGB( hsb[0], hsb[1], hsb[2] ) );
	}
	
	private void loadProperties() {
		try {
		    Latex2HTMLzero.load(new FileInputStream(includePath + "Latex2HTMLzero.properties"));
		} catch (IOException e) {
			System.out.println("error loading " + includePath + "Latex2HTMLzero.properties");
		}
		
		try {
		    Latex2HTMLone.load(new FileInputStream(includePath + "Latex2HTMLone.properties"));
		} catch (IOException e) {
			System.out.println("error loading Latex2HTMLone.properties");
		}
	}
	
	private void addToCss(String color) {
		css += "." + color + endl
				+ "{" + endl
				+ "border-color: " + HTMLColorToString( HTMLColors.get(color).darker().darker() ) + ";" + endl
				+ "background-color: " + HTMLColorToString( alter(HTMLColors.get(color)) ) + ";" + endl
				+ "}" + endl;
	}
	
	private String getCellColor(String cellName) {
		if(cellColors.get(cellName) != null)
			return cellColors.get(cellName);
		else
			return "defaultColor";		
	}

	private void parsePreamble() {
		/* This function parses the preamble and detects the title, organization and author7
		 *  specified in the preamble. It adds these information to the beginning of result. */
		
		if(preamble.contains("\\title{"))
			title = parseComment(latexExtract(preamble,"\\title{"));
		organization = latexExtract(preamble,"\\organization{");
		author = latexExtract(preamble,"\\author{");
		
		if(organization != null) {
			result = "<div> <br /> </div>" + endl + result;
			result = "<span>" + parseComment(organization) + " </span> " + endl + result;
		}
		if(author != null) {
			result = "<div> <br /> </div>" + endl + result;
			result = "<span>" + parseComment(author) + "</span> " + endl + result;
		}
		
		result = "<div> <br /> </div>" + endl + result;
		result = "<h1>" + title + " </h1> " + endl + result;
		
	}
	
	// a simple version of multipleLatexExtracts that does not use regex
	private String latexExtract(String from, String instruction)
	{
		int a = from.indexOf(instruction);
		if(a != -1) {
			a += instruction.length();
			int i = a;
			for(int b = 1; b > 0 && i < from.length(); i++) {
				if(from.charAt(i) == '{')
					b++;
				else if (from.charAt(i) == '}')
					b--;
			}
			return from.substring(a,i-1);
		}
		return null;
	}
	
	private void initializeCss() {
	/*  This function initializes the css string. 
		It's added directly in the header of the html page.
		The content of this string should be put in an external file
		an loaded during execution. 
	*/
		css += ".bold" + endl
				+ "{" + endl
				+ "font-weight: bold;"+endl
				+ "}" + endl;
		
		css += ".italic" + endl
				+ "{" + endl
				+ "font-style: italic;"+endl
				+ "}" + endl;
		
		css += "em" + endl
				+ "{" + endl
				+ "font-style: italic;"+endl
				+ "}" + endl;
		
		css += ".large" + endl
				+ "{" + endl
				+ "font-size: large;"+endl
				+ "}" + endl;
		
		css += ".xlarge" + endl
				+ "{" + endl
				+ "font-size: x-large;"+endl
				+ "}" + endl;
		
		css += ".courier" + endl
				+ "{" + endl
				+ "font-family: courier;"+endl
				+ "}" + endl;
		
		css += ".attribute" + endl
				+ "{" + endl
				+ "color: blue;"+endl
				+ "}" + endl;
		
		css += ".center" + endl
				+ "{" + endl
				+ "text-align: center;"+endl
				+ "}" + endl;
		
		css += ".textCentered" + endl
				+ "{" + endl
				+ "text-align: center;"+endl
				+ "display: inline-block;"+endl
				+ "vertical-align: top;"+endl
				+ "min-width: 20px;"+endl
				+ "}" + endl;
		
		css += ".cell" + endl
				+ "{" + endl
				+ "display: inline-block;"+endl
				+ "vertical-align: top;"+endl
				+ "text-align:left;" +endl
				+ "}" + endl;
		
		css += "hr.reduce" + endl
				+ "{" + endl
				+ "color: black;"+endl
				+ "background-color: black;"+endl
				+ "height: 3px;"+endl
				+ "}" + endl;
		
		css += "hr.reduce:after" + endl
				+ "{" + endl
				+ "width: 0;"+endl
				+ "height: 0;"+endl
				+ "border-left : 6px solid transparent;"+endl
				+ "border-right : 6px solid transparent;"+endl
				+ "border-top : 6px solid black;"+endl
				+ "position: absolute;"+endl
				+ "content: \"\";"+endl
				+ "}" + endl;
		
		css += ".tab" + endl
				+ "{" + endl
				+ "display: inline-block;"+endl
				+ "padding : 2px;"+endl
				+ "margin-left : 0;"+endl
				+ "border-top : 3px solid;"+endl
				+ "border-left : 3px solid;"+endl
				+ "border-right : 3px solid;"+endl
				+ "text-align: left;"+endl
				+ "}" + endl;
		
		css += ".tab.curvedEdge" + endl
				+ "{" + endl
				+ "position: relative;"+endl
				+ "left: 30px;"+endl
				+ "top : 3px;"+endl
				+ "}" + endl;
		
		css += ".tab.straightEdge" + endl
				+ "{" + endl
				+ "position: relative;"+endl
				+ "left: 8px;"+endl
				+ "top : 3px;"+endl
				+ "}" + endl;
		
		css += ".block" + endl
				+ "{" + endl
				+ "color : black;"+endl
				+ "border-width: 3px;"+endl
				+ "display: inline-block; "+endl
				+ "padding: 10px;"+endl
				+ "padding-left: 20px;"+endl
				+ "padding-right: 20px;"+endl
				+ "border-radius: 30px;"+endl
				+ "border-style: solid;"+endl
				+ "min-width: 60px;"+endl
				+ "text-align: left;"+endl
				+ "}" + endl;
		
		css += ".commentBlock" + endl
				+ "{" + endl
				+ "color : black;"+endl
				+ "border-width: 1px;"+endl
				+ "display: inline-block; "+endl
				+ "padding: 10px;"+endl
				+ "padding-left: 20px;"+endl
				+ "padding-right: 20px;"+endl
				+ "border-radius: 30px;"+endl
				+ "border-style: solid;"+endl
				+ "text-align: left;"+endl
				+ "}" + endl;
		
		css += ".block.right" + endl
				+ "{" + endl
				+ "border-top-right-radius: 0px;"+endl
				+ "border-bottom-right-radius: 0px;"+endl
				+ "border-right-style: dotted;"+endl
				+ "}" + endl;
		
		css += ".block.left" + endl
				+ "{" + endl
				+ "border-top-left-radius: 0px;"+endl
				+ "border-bottom-left-radius: 0px;"+endl
				+ "border-left-style: dotted;"+endl
				+ "}" + endl;	
		
		css += ".defaultColor" + endl
				+ "{" + endl
				+ "border-color: #000000;"+endl
				+ "background-color: #FFFFFF;"+endl
				+ "}" + endl;
		
		css += ".definitionComment" + endl
				+ "{" + endl
				+ "border-color: #000000;"+endl
				+ "background-color: #f2f2f2;"+endl
				+ "}" + endl;
		
		css += ".moduleComment" + endl
				+ "{" + endl
				+ "border-color: #000000;"+endl
				+ "background-color: #e5e5e5;"+endl
				+ "}" + endl;
	}
	
	private void createHTMLColors() {
		usedColors.add("defaultColor");
		
		HTMLColors.put("aliceblue" , new Color(240, 248, 255));
		HTMLColors.put("antiquewhite" , new Color(250, 235, 215));
		HTMLColors.put("aqua" , new Color(0, 255, 255));
		HTMLColors.put("aquamarine" , new Color(127, 255, 212));
		HTMLColors.put("azure" , new Color(240, 255, 255));
		HTMLColors.put("beige" , new Color(245, 245, 220));
		HTMLColors.put("bisque" , new Color(255, 228, 196));
		HTMLColors.put("black" , new Color(0, 0, 0));
		HTMLColors.put("blanchedalmond" , new Color(255, 235, 205));
		HTMLColors.put("blue" , new Color(0, 0, 255));
		HTMLColors.put("blueviolet" , new Color(138, 43, 226));
		HTMLColors.put("brown" , new Color(165, 42, 42));
		HTMLColors.put("burlywood" , new Color(222, 184, 135));
		HTMLColors.put("cadetblue" , new Color(95, 158, 160));
		HTMLColors.put("chartreuse" , new Color(127, 255, 0));
		HTMLColors.put("chocolate" , new Color(210, 105, 30));
		HTMLColors.put("coral" , new Color(255, 127, 80));
		HTMLColors.put("cornflowerblue" , new Color(100, 149, 237));
		HTMLColors.put("cornsilk" , new Color(255, 248, 220));
		HTMLColors.put("crimson" , new Color(220, 20, 60));
		HTMLColors.put("cyan" , new Color(0, 255, 255));
		HTMLColors.put("darkblue" , new Color(0, 0, 139));
		HTMLColors.put("darkcyan" , new Color(0, 139, 139));
		HTMLColors.put("darkgoldenrod" , new Color(184, 134, 11));
		HTMLColors.put("darkgray" , new Color(169, 169, 169));
		HTMLColors.put("darkgreen" , new Color(0, 100, 0));
		HTMLColors.put("darkgrey" , new Color(169, 169, 169));
		HTMLColors.put("darkkhaki" , new Color(189, 183, 107));
		HTMLColors.put("darkmagenta" , new Color(139, 0, 139));
		HTMLColors.put("darkolivegreen" , new Color(85, 107, 47));
		HTMLColors.put("darkorchid" , new Color(153, 50, 204));
		HTMLColors.put("darkred" , new Color(139, 0, 0));
		HTMLColors.put("darksalmon" , new Color(233, 150, 122));
		HTMLColors.put("darkseagreen" , new Color(143, 188, 143));
		HTMLColors.put("darkslateblue" , new Color(72, 61, 139));
		HTMLColors.put("darkslategray" , new Color(47, 79, 79));
		HTMLColors.put("darkslategrey" , new Color(47, 79, 79));
		HTMLColors.put("darkturquoise" , new Color(0, 206, 209));
		HTMLColors.put("darkviolet" , new Color(148, 0, 211));
		HTMLColors.put("darkorange" , new Color(255, 140, 0));
		HTMLColors.put("deeppink" , new Color(255, 20, 147));
		HTMLColors.put("deepskyblue" , new Color(0, 191, 255));
		HTMLColors.put("dimgray" , new Color(105, 105, 105));
		HTMLColors.put("dimgrey" , new Color(105, 105, 105));
		HTMLColors.put("dodgerblue" , new Color(30, 144, 255));
		HTMLColors.put("firebrick" , new Color(178, 34, 34));
		HTMLColors.put("floralwhite" , new Color(255, 250, 240));
		HTMLColors.put("forestgreen" , new Color(34, 139, 34));
		HTMLColors.put("fuchsia" , new Color(255, 0, 255));
		HTMLColors.put("gainsboro" , new Color(220, 220, 220));
		HTMLColors.put("ghostwhite" , new Color(248, 248, 255));
		HTMLColors.put("gold" , new Color(255, 215, 0));
		HTMLColors.put("goldenrod" , new Color(218, 165, 32));
		HTMLColors.put("gray" , new Color(128, 128, 128));
		HTMLColors.put("green" , new Color(0, 128, 0));
		HTMLColors.put("greenyellow" , new Color(173, 255, 47));
		HTMLColors.put("grey" , new Color(128, 128, 128));
		HTMLColors.put("honeydew" , new Color(240, 255, 240));
		HTMLColors.put("hotpink" , new Color(255, 105, 180));
		HTMLColors.put("indianred" , new Color(205, 92, 92));
		HTMLColors.put("indigo" , new Color(75, 0, 130));
		HTMLColors.put("ivory" , new Color(255, 255, 240));
		HTMLColors.put("khaki" , new Color(240, 230, 140));
		HTMLColors.put("lavender" , new Color(230, 230, 250));
		HTMLColors.put("lavenderblush" , new Color(255, 240, 245));
		HTMLColors.put("lawngreen" , new Color(124, 252, 0));
		HTMLColors.put("lemonchiffon" , new Color(255, 250, 205));
		HTMLColors.put("lightblue" , new Color(173, 216, 230));
		HTMLColors.put("lightcoral" , new Color(240, 128, 128));
		HTMLColors.put("lightcyan" , new Color(224, 255, 255));
		HTMLColors.put("lightgoldenrodyellow" , new Color(250, 250, 210));
		HTMLColors.put("lightgray" , new Color(211, 211, 211));
		HTMLColors.put("lightgreen" , new Color(144, 238, 144));
		HTMLColors.put("lightgrey" , new Color(211, 211, 211));
		HTMLColors.put("lightpink" , new Color(255, 182, 193));
		HTMLColors.put("lightsalmon" , new Color(255, 160, 122));
		HTMLColors.put("lightseagreen" , new Color(32, 178, 170));
		HTMLColors.put("lightskyblue" , new Color(135, 206, 250));
		HTMLColors.put("lightslategray" , new Color(119, 136, 153));
		HTMLColors.put("lightslategrey" , new Color(119, 136, 153));
		HTMLColors.put("lightsteelblue" , new Color(176, 196, 222));
		HTMLColors.put("lightyellow" , new Color(255, 255, 224));
		HTMLColors.put("lime" , new Color(0, 255, 0));
		HTMLColors.put("limegreen" , new Color(50, 205, 50));
		HTMLColors.put("linen" , new Color(250, 240, 230));
		HTMLColors.put("magenta" , new Color(255, 0, 255));
		HTMLColors.put("maroon" , new Color(128, 0, 0));
		HTMLColors.put("mediumaquamarine" , new Color(102, 205, 170));
		HTMLColors.put("mediumblue" , new Color(0, 0, 205));
		HTMLColors.put("mediumorchid" , new Color(186, 85, 211));
		HTMLColors.put("mediumpurple" , new Color(147, 112, 216));
		HTMLColors.put("mediumseagreen" , new Color(60, 179, 113));
		HTMLColors.put("mediumslateblue" , new Color(123, 104, 238));
		HTMLColors.put("mediumspringgreen" , new Color(0, 250, 154));
		HTMLColors.put("mediumturquoise" , new Color(72, 209, 204));
		HTMLColors.put("mediumvioletred" , new Color(199, 21, 133));
		HTMLColors.put("midnightblue" , new Color(25, 25, 112));
		HTMLColors.put("mintcream" , new Color(245, 255, 250));
		HTMLColors.put("mistyrose" , new Color(255, 228, 225));
		HTMLColors.put("moccasin" , new Color(255, 228, 181));
		HTMLColors.put("navajowhite" , new Color(255, 222, 173));
		HTMLColors.put("navy" , new Color(0, 0, 128));
		HTMLColors.put("oldlace" , new Color(253, 245, 230));
		HTMLColors.put("olive" , new Color(128, 128, 0));
		HTMLColors.put("olivedrab" , new Color(107, 142, 35));
		HTMLColors.put("orange" , new Color(255, 165, 0));
		HTMLColors.put("orangered" , new Color(255, 69, 0));
		HTMLColors.put("orchid" , new Color(218, 112, 214));
		HTMLColors.put("palegoldenrod" , new Color(238, 232, 170));
		HTMLColors.put("palegreen" , new Color(152, 251, 152));
		HTMLColors.put("paleturquoise" , new Color(175, 238, 238));
		HTMLColors.put("palevioletred" , new Color(216, 112, 147));
		HTMLColors.put("papayawhip" , new Color(255, 239, 213));
		HTMLColors.put("peachpuff" , new Color(255, 218, 185));
		HTMLColors.put("peru" , new Color(205, 133, 63));
		HTMLColors.put("pink" , new Color(255, 192, 203));
		HTMLColors.put("plum" , new Color(221, 160, 221));
		HTMLColors.put("powderblue" , new Color(176, 224, 230));
		HTMLColors.put("purple" , new Color(128, 0, 128));
		HTMLColors.put("red" , new Color(255, 0, 0));
		HTMLColors.put("rosybrown" , new Color(188, 143, 143));
		HTMLColors.put("royalblue" , new Color(65, 105, 225));
		HTMLColors.put("saddlebrown" , new Color(139, 69, 19));
		HTMLColors.put("salmon" , new Color(250, 128, 114));
		HTMLColors.put("sandybrown" , new Color(244, 164, 96));
		HTMLColors.put("seagreen" , new Color(46, 139, 87));
		HTMLColors.put("seashell" , new Color(255, 245, 238));
		HTMLColors.put("sienna" , new Color(160, 82, 45));
		HTMLColors.put("silver" , new Color(192, 192, 192));
		HTMLColors.put("skyblue" , new Color(135, 206, 235));
		HTMLColors.put("slateblue" , new Color(106, 90, 205));
		HTMLColors.put("slategray" , new Color(112, 128, 144));
		HTMLColors.put("slategrey" , new Color(112, 128, 144));
		HTMLColors.put("snow" , new Color(255, 250, 250));
		HTMLColors.put("springgreen" , new Color(0, 255, 127));
		HTMLColors.put("steelblue" , new Color(70, 130, 180));
		HTMLColors.put("tan" , new Color(210, 180, 140));
		HTMLColors.put("teal" , new Color(0, 128, 128));
		HTMLColors.put("thistle" , new Color(216, 191, 216));
		HTMLColors.put("tomato" , new Color(255, 99, 71));
		HTMLColors.put("turquoise" , new Color(64, 224, 208));
		HTMLColors.put("violet" , new Color(238, 130, 238));
		HTMLColors.put("wheat" , new Color(245, 222, 179));
		HTMLColors.put("white" , new Color(255, 255, 255));
		HTMLColors.put("whitesmoke" , new Color(245, 245, 245));
		HTMLColors.put("yellow" , new Color(255, 255, 0));
		HTMLColors.put("yellowgreen" , new Color(154, 205, 50));
	}
}
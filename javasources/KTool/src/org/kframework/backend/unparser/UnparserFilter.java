package org.kframework.backend.unparser;

import org.kframework.compile.transformers.AddEmptyLists;
import org.kframework.compile.utils.MetaK;
import org.kframework.kil.*;
import org.kframework.kil.ProductionItem.ProductionType;
import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.kil.visitors.BasicVisitor;
import org.kframework.utils.ColorUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class UnparserFilter extends BasicVisitor {
	private Indenter result = new Indenter();
	private boolean firstPriorityBlock = false;
	private boolean firstProduction = false;
	private boolean inConfiguration = false;
	private boolean addParentheses;
	private int inTerm = 0;
	private boolean color = false;
	private static int TAB = 4;
	private java.util.List<String> variableList = new java.util.LinkedList<String>();
	private java.util.Map<Production, Integer> priorities = null;
	private java.util.Stack<ASTNode> stack = new java.util.Stack<ASTNode>();

	public UnparserFilter() {
		this(false);
	}

	public UnparserFilter(boolean inConfiguration) {
		this(inConfiguration, false);
	}

	public UnparserFilter(boolean inConfiguration, boolean color) {
		this(inConfiguration, color, true);
	}

	public UnparserFilter(boolean inConfiguration, boolean color, boolean addParentheses) {
		this.inConfiguration = inConfiguration;
		this.color = color;
		this.inTerm = 0;
		this.addParentheses = addParentheses;
	}

	public String getResult() {
		return result.toString();
	}

	@Override
	public void visit(Definition def) {
		prepare(def);
		super.visit(def);
		postpare();
	}

	@Override
	public void visit(Import imp) {
		prepare(imp);
		result.write("imports " + imp.getName());
		result.endLine();
		postpare();
	}

	@Override
	public void visit(Module mod) {
		prepare(mod);
		if (!mod.isPredefined()) {
			result.write("module " + mod.getName());
			result.endLine();
			result.endLine();
			result.indent(TAB);
			super.visit(mod);
			result.unindent();
			result.write("endmodule");
			result.endLine();
			result.endLine();
		}
		postpare();
	}

	@Override
	public void visit(Syntax syn) {
		prepare(syn);
		firstPriorityBlock = true;
		result.write("syntax " + syn.getSort().getName());
		result.indentToCurrent();
		for (PriorityBlock pb : syn.getPriorityBlocks()) {
			pb.accept(this);
		}
		result.unindent();
		result.endLine();
		postpare();
	}

	@Override
	public void visit(PriorityBlock priorityBlock) {
		prepare(priorityBlock);
		if (firstPriorityBlock) {
			result.write(" ::=");
		} else {
			result.write("  >");
		}
		firstPriorityBlock = false;
		firstProduction = true;
		super.visit(priorityBlock);
		postpare();
	}

	@Override
	public void visit(Production prod) {
		prepare(prod);
		if (firstProduction) {
			result.write(" ");
		} else {
			result.write("  | ");
		}
		firstProduction = false;
		for (int i = 0; i < prod.getItems().size(); ++i) {
			ProductionItem pi = prod.getItems().get(i);
			pi.accept(this);
			if (i != prod.getItems().size() - 1) {
				result.write(" ");
			}
		}
		prod.getAttributes().accept(this);
		result.endLine();
		postpare();
	}

	@Override
	public void visit(Sort sort) {
		prepare(sort);
		result.write(sort.getName());
		super.visit(sort);
		postpare();
	}

	@Override
	public void visit(Terminal terminal) {
		prepare(terminal);
		result.write("\"" + terminal.getTerminal() + "\"");
		super.visit(terminal);
		postpare();
	}

	@Override
	public void visit(UserList userList) {
		prepare(userList);
		result.write("List{" + userList.getSort() + ",\"" + userList.getSeparator() + "\"}");
		super.visit(userList);
		postpare();
	}

	@Override
	public void visit(KList listOfK) {
		prepare(listOfK);
		java.util.List<Term> termList = listOfK.getContents();
		for (int i = 0; i < termList.size(); ++i) {
			termList.get(i).accept(this);
			if (i != termList.size() - 1) {
				result.write(",, ");
			}
		}
		postpare();
	}

	@Override
	public void visit(Attributes attributes) {
		prepare(attributes);
		java.util.List<String> reject = new LinkedList<String>();
		reject.add("cons");
		reject.add("kgeneratedlabel");
		reject.add("prefixlabel");
		reject.add("filename");
		reject.add("location");

		List<Attribute> attributeList = new LinkedList<Attribute>();
		List<Attribute> oldAttributeList = attributes.getContents();
		for (int i = 0; i < oldAttributeList.size(); ++i) {
			if (!reject.contains(oldAttributeList.get(i).getKey())) {
				attributeList.add(oldAttributeList.get(i));
			}
		}

		if (!attributeList.isEmpty()) {
			result.write(" ");
			result.write("[");
			for (int i = 0; i < attributeList.size(); ++i) {
				attributeList.get(i).accept(this);
				if (i != attributeList.size() - 1) {
					result.write(", ");
				}
			}
			result.write("]");
		}
		postpare();
	}

	@Override
	public void visit(Attribute attribute) {
		prepare(attribute);
		result.write(attribute.getKey());
		if (!attribute.getValue().equals("")) {
			result.write("(" + attribute.getValue() + ")");
		}
		postpare();
	}

	@Override
	public void visit(Configuration configuration) {
		prepare(configuration);
		result.write("configuration");
		result.endLine();
		result.indent(TAB);
		inConfiguration = true;
		configuration.getBody().accept(this);
		inConfiguration = false;
		result.unindent();
		result.endLine();
		result.endLine();
		postpare();
	}

	@Override
	public void visit(Cell cell) {
		prepare(cell);
		String attributes = "";
		for (Entry<String, String> entry : cell.getCellAttributes().entrySet()) {
			if (entry.getKey() != "ellipses") {
				attributes += " " + entry.getKey() + "=\"" + entry.getValue() + "\"";
			}
		}
		String colorCode = "";
		if (color) {
			Cell declaredCell = DefinitionHelper.cells.get(cell.getLabel());
			if (declaredCell != null) {
				String declaredColor = declaredCell.getCellAttributes().get("color");
				if (declaredColor != null) {
					colorCode = ColorUtil.RgbToAnsi(ColorUtil.colors.get(declaredColor));
					result.write(colorCode);
				}
			}
		}

		result.write("<" + cell.getLabel() + attributes + ">");
		if (inConfiguration && inTerm == 0) {
			result.endLine();
			result.indent(TAB);
		} else {
			result.write(" ");
		}
		if (cell.hasLeftEllipsis()) {
			result.write("... ");
		}
		if (!colorCode.equals("")) {
			result.write(ColorUtil.ANSI_NORMAL);
		}
		cell.getContents().accept(this);
		result.write(colorCode);
		if (cell.hasRightEllipsis()) {
			result.write(" ...");
		}
		if (inConfiguration && inTerm == 0) {
			result.endLine();
			result.unindent();
		} else {
			result.write(" ");
		}
		result.write("</" + cell.getLabel() + ">");
		if (!colorCode.equals("")) {
			result.write(ColorUtil.ANSI_NORMAL);
		}
		postpare();
	}

	@Override
	public void visit(Variable variable) {
		prepare(variable);
		if (variable.isFresh())
			result.write("?");
		result.write(variable.getName());
		if (!variableList.contains(variable.getName())) {
			result.write(":" + variable.getSort());
			variableList.add(variable.getName());
		}
		postpare();
	}

	@Override
	public void visit(Empty empty) {
		prepare(empty);
		result.write("." + empty.getSort());
		postpare();
	}

	@Override
	public void visit(ListTerminator terminator) {
		prepare(terminator);
		if (terminator.getSort().equals("K"))
			result.write(".List{\"" + terminator.getSeparator() + "\"}");
		else
			result.write("." + terminator.getSort());
		postpare();
	}

	@Override
	public void visit(Rule rule) {
		prepare(rule);
		result.write("rule ");
		if (!"".equals(rule.getLabel())) {
			result.write("[" + rule.getLabel() + "]: ");
		}
		variableList.clear();
		rule.getBody().accept(this);
		if (rule.getCondition() != null) {
			result.write(" when ");
			rule.getCondition().accept(this);
		}
		rule.getAttributes().accept(this);
		result.endLine();
		result.endLine();
		postpare();
	}

	@Override
	public void visit(KApp kapp) {
		prepare(kapp);
		kapp.getLabel().accept(this);
		result.write("(");
		kapp.getChild().accept(this);
		result.write(")");
		postpare();
	}

	@Override
	public void visit(KSequence ksequence) {
		prepare(ksequence);
		java.util.List<Term> contents = ksequence.getContents();
		if (!contents.isEmpty()) {
            for (int i = 0; i < contents.size(); i++) {
                contents.get(i).accept(this);
                if (i != contents.size() - 1) {
                    result.write(" ~> ");
                }
            }
        } else {
            result.write(".K");
        }
		postpare();
	}

	@Override
	public void visit(TermCons termCons) {
		prepare(termCons);
		inTerm++;
		Production production = termCons.getProduction();
		if (production.isListDecl()) {
			UserList userList = (UserList) production.getItems().get(0);
			String separator = userList.getSeparator();
			java.util.List<Term> contents = termCons.getContents();
			contents.get(0).accept(this);
			ASTNode temp = stack.pop();
			ASTNode parent = null;
			if (!stack.empty())
				parent = stack.peek();
			stack.push(temp);
			boolean needsEmpty = true;
			if (parent instanceof TermCons) {
				TermCons tcParent = (TermCons) parent;
				int i;
				for (i = 0; i < tcParent.getContents().size(); i++) {
					if (termCons == tcParent.getContents().get(i))
						break;
				}
				if (AddEmptyLists.isAddEmptyList(tcParent.getProduction().getChildSort(i), contents.get(0).getSort()))
					needsEmpty = false;
			}
			if (needsEmpty || !(contents.get(1) instanceof Empty)) {
				result.write(separator + " ");
				contents.get(1).accept(this);
			}
		} else {
			int where = 0;
			for (int i = 0; i < production.getItems().size(); ++i) {
				ProductionItem productionItem = production.getItems().get(i);
				if (productionItem.getType() != ProductionType.TERMINAL) {
					termCons.getContents().get(where++).accept(this);
				} else {
					result.write(((Terminal) productionItem).getTerminal());
				}
				if (i != production.getItems().size() - 1) {
					result.write(" ");
				}
			}
		}
		inTerm--;
		postpare();
	}

	@Override
	public void visit(Rewrite rewrite) {
		prepare(rewrite);
		rewrite.getLeft().accept(this);
		result.write(" => ");
		rewrite.getRight().accept(this);
		postpare();
	}

    @Override
    public void visit(KLabelConstant kLabelConstant) {
        /* andreis: prepare and postpare seem unnecessary for KLabelConstant */
        //prepare(kLabelConstant);
        result.write(kLabelConstant.getLabel());
        //postpare();
    }

	@Override
	public void visit(Constant constant) {
		prepare(constant);
		result.write(constant.getValue());
		postpare();
	}

    @Override
    public void visit(Builtin builtin) {
        /* andreis: prepare and postpare seem unnecessary for KLabelConstant */
        prepare(builtin);
        result.write(builtin.toString());
        postpare();
    }

	@Override
	public void visit(Collection collection) {
		prepare(collection);
		java.util.List<Term> contents = collection.getContents();
		for (int i = 0; i < contents.size(); ++i) {
			contents.get(i).accept(this);
			if (i != contents.size() - 1) {
				if (inConfiguration && inTerm == 0) {
					result.endLine();
				} else {
					result.write(" ");
				}
			}
		}
		postpare();
	}

	@Override
	public void visit(CollectionItem collectionItem) {
		prepare(collectionItem);
		super.visit(collectionItem);
		postpare();
	}

	@Override
	public void visit(BagItem bagItem) {
		prepare(bagItem);
		result.write("BagItem(");
		super.visit(bagItem);
		result.write(")");
		postpare();
	}

	@Override
	public void visit(ListItem listItem) {
		prepare(listItem);
		result.write("ListItem(");
		super.visit(listItem);
		result.write(")");
		postpare();
	}

	@Override
	public void visit(SetItem setItem) {
		prepare(setItem);
		result.write("SetItem(");
		super.visit(setItem);
		result.write(")");
		postpare();
	}

	@Override
	public void visit(MapItem mapItem) {
		prepare(mapItem);
		mapItem.getKey().accept(this);
		result.write(" |-> ");
		mapItem.getValue().accept(this);
		postpare();
	}

	@Override
	public void visit(Hole hole) {
		prepare(hole);
		result.write("HOLE");
		postpare();
	}

	@Override
	public void visit(FreezerHole hole) {
		prepare(hole);
		result.write("HOLE(" + hole.getIndex() + ")");
		postpare();
	}

	@Override
	public void visit(Freezer freezer) {
		prepare(freezer);
		freezer.getTerm().accept(this);
		postpare();
	}

	@Override
	public void visit(KInjectedLabel kInjectedLabel) {
		prepare(kInjectedLabel);
		Term term = kInjectedLabel.getTerm();
		if (MetaK.isKSort(term.getSort())) {
			result.write(kInjectedLabel.getInjectedSort(term.getSort()));
			result.write("2KLabel ");
		} else {
			result.write("# ");
		}
		term.accept(this);
		postpare();
	}

	@Override
	public void visit(KLabel kLabel) {
		prepare(kLabel);
		result.endLine();
		result.write("Don't know how to pretty print KLabel");
		result.endLine();
		super.visit(kLabel);
		postpare();
	}

	@Override
	public void visit(TermComment termComment) {
		prepare(termComment);
		result.write("<br/>");
		super.visit(termComment);
		postpare();
	}

	@Override
	public void visit(org.kframework.kil.List list) {
		prepare(list);
		super.visit(list);
		postpare();
	}

	@Override
	public void visit(org.kframework.kil.Map map) {
		prepare(map);
		super.visit(map);
		postpare();
	}

	@Override
	public void visit(Bag bag) {
		prepare(bag);
		super.visit(bag);
		postpare();
	}

	@Override
	public void visit(org.kframework.kil.Set set) {
		prepare(set);
		super.visit(set);
		postpare();
	}

	@Override
	public void visit(org.kframework.kil.Ambiguity ambiguity) {
		prepare(ambiguity);
		result.write("amb(");
		result.endLine();
		result.indent(TAB);
		java.util.List<Term> contents = ambiguity.getContents();
		for (int i = 0; i < contents.size(); ++i) {
			contents.get(i).accept(this);
			if (i != contents.size() - 1) {
				result.write(",");
				result.endLine();
			}
		}
		result.endLine();
		result.unindent();
		result.write(")");
		postpare();
	}

	@Override
	public void visit(org.kframework.kil.Context context) {
		prepare(context);
		result.write("context ");
		variableList.clear();
		context.getBody().accept(this);
		if (context.getCondition() != null) {
			result.write(" when ");
			context.getCondition().accept(this);
		}
		context.getAttributes().accept(this);
		result.endLine();
		result.endLine();
		postpare();
	}

	@Override
	public void visit(LiterateDefinitionComment literateDefinitionComment) {
		prepare(literateDefinitionComment);
		// result.write(literateDefinitionComment.getValue());
		// result.endLine();
		postpare();
	}

	@Override
	public void visit(org.kframework.kil.Require require) {
		prepare(require);
		result.write("require \"" + require.getValue() + "\"");
		result.endLine();
		postpare();
	}

	@Override
	public void visit(BackendTerm term) {
		prepare(term);
		result.write(term.getValue());
		postpare();
	}

	@Override
	public void visit(Bracket br) {
		prepare(br);
		result.write("(");
		br.getContent().accept(this);
		result.write(")");
		postpare();
	}

	private void prepare(ASTNode astNode) {
		if (!stack.empty()) {
			if (needsParanthesis(stack.peek(), astNode)) {
				result.write("(");
			}
		}
		stack.push(astNode);
	}

	private void postpare() {
		ASTNode astNode = stack.pop();
		if (!stack.empty()) {
			if (needsParanthesis(stack.peek(), astNode)) {
				result.write(")");
			}
		}
	}

	private boolean needsParanthesis(ASTNode upper, ASTNode astNode) {
		if (!addParentheses)
			return false;
		if (astNode instanceof Rewrite) {
			if ((upper instanceof Cell) || (upper instanceof Rule)) {
				return false;
			}
			return true;
		} else if ((astNode instanceof TermCons) && (upper instanceof TermCons)) {
			TermCons termConsNext = (TermCons) astNode;
			TermCons termCons = (TermCons) upper;
			Production productionNext = termConsNext.getProduction();
			Production production = termCons.getProduction();
			if (DefinitionHelper.isPriorityWrong(production.getKLabel(), productionNext.getKLabel())) {
				return true;
			}
			return termConsNext.getContents().size() != 0;
		} else {
			return false;
		}
	}

	public java.util.Map<Production, Integer> getPriorities() {
		return priorities;
	}

	public void setPriorities(java.util.Map<Production, Integer> priorities) {
		this.priorities = priorities;
	}
}

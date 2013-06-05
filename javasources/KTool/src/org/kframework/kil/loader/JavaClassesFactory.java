package org.kframework.kil.loader;

import java.util.ArrayList;

import org.kframework.kil.ASTNode;
import org.kframework.kil.Ambiguity;
import org.kframework.kil.Attribute;
import org.kframework.kil.Attributes;
import org.kframework.kil.Bag;
import org.kframework.kil.BagItem;
import org.kframework.kil.Bracket;
import org.kframework.kil.Cast;
import org.kframework.kil.Cell;
import org.kframework.kil.Configuration;
import org.kframework.kil.Definition;
import org.kframework.kil.Empty;
import org.kframework.kil.FreezerHole;
import org.kframework.kil.Hole;
import org.kframework.kil.Import;
import org.kframework.kil.KApp;
import org.kframework.kil.KLabelConstant;
import org.kframework.kil.KList;
import org.kframework.kil.KSequence;
import org.kframework.kil.KSorts;
import org.kframework.kil.Lexical;
import org.kframework.kil.List;
import org.kframework.kil.ListItem;
import org.kframework.kil.LiterateDefinitionComment;
import org.kframework.kil.LiterateModuleComment;
import org.kframework.kil.Map;
import org.kframework.kil.MapItem;
import org.kframework.kil.Module;
import org.kframework.kil.PriorityBlock;
import org.kframework.kil.PriorityBlockExtended;
import org.kframework.kil.PriorityExtended;
import org.kframework.kil.PriorityExtendedAssoc;
import org.kframework.kil.Production;
import org.kframework.kil.Require;
import org.kframework.kil.Restrictions;
import org.kframework.kil.Rewrite;
import org.kframework.kil.Rule;
import org.kframework.kil.Sentence;
import org.kframework.kil.Set;
import org.kframework.kil.SetItem;
import org.kframework.kil.Sort;
import org.kframework.kil.StringSentence;
import org.kframework.kil.Syntax;
import org.kframework.kil.Term;
import org.kframework.kil.TermComment;
import org.kframework.kil.TermCons;
import org.kframework.kil.Terminal;
import org.kframework.kil.Token;
import org.kframework.kil.UserList;
import org.kframework.kil.Variable;
import org.kframework.utils.StringUtil;
import org.w3c.dom.Element;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

/**
 * Factory for creating KIL classes from XML nodes or ATerms.
 * Must call startConstruction/endConstruction around calls to getTerm,
 * to supply a Context.
 */
public class JavaClassesFactory {
	private static Context context = null;

	/** Set the context to use */
	public static synchronized void startConstruction(Context context) {
		assert JavaClassesFactory.context == null;
		JavaClassesFactory.context = context;
	}

	public static synchronized void endConstruction() {
		assert JavaClassesFactory.context != null;
		JavaClassesFactory.context = null;
	}

	public static ASTNode getTerm(Element element) {
		assert context != null;
		// used for a new feature - loading java classes at first step (Basic Parsing)
		if (Constants.LEXICAL.equals(element.getNodeName()))
			return new Lexical(element);
		if (Constants.RESTRICTIONS.equals(element.getNodeName()))
			return new Restrictions(element);
		if (Constants.RULE.equals(element.getNodeName()) && element.hasAttribute(Constants.VALUE_value_ATTR))
			return new StringSentence(element);
		if (Constants.CONFIG.equals(element.getNodeName()) && element.hasAttribute(Constants.VALUE_value_ATTR))
			return new StringSentence(element);
		if (Constants.CONTEXT.equals(element.getNodeName()) && element.hasAttribute(Constants.VALUE_value_ATTR))
			return new StringSentence(element);

		if (Constants.REQUIRE.equals(element.getNodeName()))
			return new Require(element);
		if (Constants.MODULE.equals(element.getNodeName()))
			return new Module(element);
		if (Constants.IMPORT.equals(element.getNodeName()))
			return new Import(element);
		if (Constants.SYNTAX.equals(element.getNodeName()))
			return new Syntax(element);

		if (Constants.PRISENT.equals(element.getNodeName()))
			return new PriorityExtended(element);
		if (Constants.PRIASSOC.equals(element.getNodeName()))
			return new PriorityExtendedAssoc(element);
		if (Constants.PRIBLOCK.equals(element.getNodeName()))
			return new PriorityBlockExtended(element);

		if (Constants.SORT.equals(element.getNodeName()))
			return new Sort(element);
		if (Constants.PRIORITY.equals(element.getNodeName()))
			return new PriorityBlock(element);
		if (Constants.PRODUCTION.equals(element.getNodeName()))
			return new Production(element);
		if (Constants.RULE.equals(element.getNodeName()))
			return new Rule(element);
		if (Constants.SENTENCE.equals(element.getNodeName()))
			return new Sentence(element);
		if (Constants.REWRITE.equals(element.getNodeName()))
			return new Rewrite(element);
		if (Constants.TERM.equals(element.getNodeName())) {
			assert context != null;
			return new TermCons(element, context);
		}
		if (Constants.BRACKET.equals(element.getNodeName()))
			return new Bracket(element);
		if (Constants.CAST.equals(element.getNodeName()))
			return new Cast(element);
		if (Constants.VAR.equals(element.getNodeName()))
			return new Variable(element);
		if (Constants.TERMINAL.equals(element.getNodeName()))
			return new Terminal(element);
		if (Constants.CONST.equals(element.getNodeName())) {
			if (element.getAttribute(Constants.SORT_sort_ATTR).equals(KSorts.KLABEL)) {
				return new KLabelConstant(element);
			} else {
				// builtin token or lexical token
				return Token.kAppOf(element);
			}
		}
		if (Constants.KAPP.equals(element.getNodeName()))
			return new KApp(element, context);
		if (KSorts.KLIST.equals(element.getNodeName()))
			return new KList(element);
		if (Constants.EMPTY.equals(element.getNodeName())) {
			if (element.getAttribute(Constants.SORT_sort_ATTR).equals(KSorts.K)) {
				return KSequence.EMPTY;
			} else if (element.getAttribute(Constants.SORT_sort_ATTR).equals(KSorts.KLIST)) {
				return KList.EMPTY;
			} else if (element.getAttribute(Constants.SORT_sort_ATTR).equals(KSorts.BAG)) {
				return Bag.EMPTY;
			} else if (element.getAttribute(Constants.SORT_sort_ATTR).equals(KSorts.LIST)) {
				return List.EMPTY;
			} else if (element.getAttribute(Constants.SORT_sort_ATTR).equals(KSorts.MAP)) {
				return Map.EMPTY;
			} else if (element.getAttribute(Constants.SORT_sort_ATTR).equals(KSorts.SET)) {
				return Set.EMPTY;
			} else {
				// user defined empty list
				return new Empty(element);
			}
		}
		if (KSorts.SET.equals(element.getNodeName()))
			return new Set(element);
		if (KSorts.SET_ITEM.equals(element.getNodeName()))
			return new SetItem(element);
		if (Constants.USERLIST.equals(element.getNodeName()))
			return new UserList(element);
		if (Constants.CONFIG.equals(element.getNodeName()))
			return new Configuration(element);
		if (Constants.CELL.equals(element.getNodeName()))
			return new Cell(element);
		if (Constants.BREAK.equals(element.getNodeName()))
			return new TermComment(element);
		if (KSorts.BAG.equals(element.getNodeName()))
			return new Bag(element);
		if (KSorts.BAG_ITEM.equals(element.getNodeName()))
			return new BagItem(element);
		if (Constants.KSEQUENCE.equals(element.getNodeName()))
			return new KSequence(element);
		if (KSorts.MAP.equals(element.getNodeName()))
			return new Map(element);
		if (KSorts.MAP_ITEM.equals(element.getNodeName()))
			return new MapItem(element);
		if (Constants.CONTEXT.equals(element.getNodeName()))
			return new org.kframework.kil.Context(element);
		if (Constants.HOLE.equals(element.getNodeName()))
			return Hole.KITEM_HOLE;
		if (Constants.FREEZERHOLE.equals(element.getNodeName()))
			return new FreezerHole(element);
		if (KSorts.LIST.equals(element.getNodeName()))
			return new List(element);
		if (KSorts.LIST_ITEM.equals(element.getNodeName()))
			return new ListItem(element);
		if (Constants.DEFINITION.equals(element.getNodeName()))
			return new Definition(element);
		if (Constants.AMB.equals(element.getNodeName()))
			return new Ambiguity(element);
		if (Constants.MODULECOMMENT.equals(element.getNodeName()))
			return new LiterateModuleComment(element);
		if (Constants.DEFCOMMENT.equals(element.getNodeName()))
			return new LiterateDefinitionComment(element);
		if (Constants.TAG.equals(element.getNodeName()))
			return new Attribute(element);
		if (Constants.ATTRIBUTES.equals(element.getNodeName()))
			return new Attributes(element);

		System.out.println(">>> " + element.getNodeName() + " <<< - unimplemented yet: org.kframework.kil.loader.JavaClassesFactory");
		return null;
	}

	public static ASTNode getTerm(ATerm atm) {
		assert context != null;

		if (atm.getType() == ATerm.APPL) {
			ATermAppl appl = (ATermAppl) atm;
			// used for a new feature - loading java classes at first step (Basic Parsing)
			// if (Constants.LEXICAL.endsWith(appl.getNodeName()))
			// return new Lexical(appl);
			// if (Constants.RESTRICTIONS.endsWith(appl.getNodeName()))
			// return new Restrictions(appl);
			// if (Constants.RULE.endsWith(appl.getNodeName()) && appl.hasAttribute(Constants.VALUE_value_ATTR))
			// return new StringSentence(appl);
			// if (Constants.RULE.endsWith(appl.getNodeName()) && appl.hasAttribute(Constants.VALUE_value_ATTR))
			// return new StringSentence(appl);
			// if (Constants.CONFIG.endsWith(appl.getNodeName()) && appl.hasAttribute(Constants.VALUE_value_ATTR))
			// return new StringSentence(appl);
			// if (Constants.CONTEXT.endsWith(appl.getNodeName()) && appl.hasAttribute(Constants.VALUE_value_ATTR))
			// return new StringSentence(appl);

			// if (Constants.REQUIRE.endsWith(appl.getNodeName()))
			// return new Require(appl);
			// if (Constants.MODULE.endsWith(appl.getNodeName()))
			// return new Module(appl);
			// if (Constants.IMPORT.endsWith(appl.getNodeName()))
			// return new Import(appl);
			// if (Constants.SYNTAX.endsWith(appl.getNodeName()))
			// return new Syntax(appl);

			// if (Constants.PRISENT.endsWith(appl.getNodeName()))
			// return new PriorityExtended(appl);
			// if (Constants.PRIASSOC.endsWith(appl.getNodeName()))
			// return new PriorityExtendedAssoc(appl);
			// if (Constants.PRIBLOCK.endsWith(appl.getNodeName()))
			// return new PriorityBlockExtended(appl);

			// if (Constants.SORT.endsWith(appl.getNodeName()))
			// return new Sort(appl);
			// if (Constants.PRIORITY.endsWith(appl.getNodeName()))
			// return new PriorityBlock(appl);
			// if (Constants.PRODUCTION.endsWith(appl.getNodeName()))
			// return new Production(appl);
			// if (Constants.RULE.endsWith(appl.getNodeName()))
			// return new Rule(appl);
			// if (Constants.REWRITE.endsWith(appl.getNodeName()))
			// return new Rewrite(appl);
			if (appl.getName().endsWith("Syn")) {
				if (appl.getName().endsWith("ListSyn") && appl.getArgument(0) instanceof ATermList) {
					ATermList list = (ATermList) appl.getArgument(0);
					TermCons head = null;
					TermCons tc = null;
					while (!list.isEmpty()) {
						TermCons ntc = new TermCons(StringUtil.getSortNameFromCons(appl.getName()), appl.getName(), context);
						ntc.setLocation(appl.getAnnotations().getFirst().toString().substring(8));
						ntc.setContents(new ArrayList<Term>());
						ntc.getContents().add((Term) JavaClassesFactory.getTerm(list.getFirst()));
						if (tc == null) {
							head = ntc;
						} else {
							tc.getContents().add(ntc);
						}
						tc = ntc;
						list = list.getNext();
					}
					if (tc != null)
						tc.getContents().add(new Empty(StringUtil.getSortNameFromCons(appl.getName())));
					else
						return new Empty(StringUtil.getSortNameFromCons(appl.getName()));
					return head;
				} else
					return new TermCons(appl, context);
			}
			if (appl.getName().endsWith("Bracket"))
				return new Bracket(appl);
			// if (Constants.CAST.endsWith(appl.getNodeName()))
			// return new Cast(appl);
			// if (Constants.VAR.endsWith(appl.getNodeName()))
			// return new Variable(appl);
			// if (Constants.TERMINAL.endsWith(appl.getNodeName()))
			// return new Terminal(appl);
			if (appl.getName().endsWith("Const")) {
				String sort = StringUtil.getSortNameFromCons(appl.getName());
				if (sort.equals(KSorts.KLABEL)) {
					return new KLabelConstant(appl);
				} else {
					// builtin token or lexical token
					return Token.kAppOf(appl);
				}
			}
			// if (Constants.KAPP.endsWith(appl.getNodeName()))
			// return new KApp(appl, null);
			// if (KSorts.KLIST.endsWith(appl.getNodeName()))
			// return new KList(appl);
			if (appl.getName().endsWith("Empty")) {
				String sort = StringUtil.getSortNameFromCons(appl.getName());
				if (sort.equals(KSorts.K)) {
					return KSequence.EMPTY;
				} else if (sort.equals(KSorts.KLIST)) {
					return KList.EMPTY;
				} else if (sort.equals(KSorts.BAG)) {
					return Bag.EMPTY;
				} else if (sort.equals(KSorts.LIST)) {
					return List.EMPTY;
				} else if (sort.equals(KSorts.MAP)) {
					return Map.EMPTY;
				} else if (sort.equals(KSorts.SET)) {
					return Set.EMPTY;
				} else {
					// user defined empty list
					return new Empty(appl);
				}
			}
			// if (KSorts.SET.endsWith(appl.getNodeName()))
			// return new Set(appl);
			// if (KSorts.SET_ITEM.endsWith(appl.getNodeName()))
			// return new SetItem(appl);
			// if (Constants.USERLIST.endsWith(appl.getName()))
			// return new UserList(appl);
			// if (Constants.CONFIG.endsWith(appl.getNodeName()))
			// return new Configuration(appl);
			// if (Constants.CELL.endsWith(appl.getNodeName()))
			// return new Cell(appl);
			// if (Constants.BREAK.endsWith(appl.getNodeName()))
			// return new TermComment(appl);
			// if (KSorts.BAG.endsWith(appl.getNodeName()))
			// return new Bag(appl);
			// if (KSorts.BAG_ITEM.endsWith(appl.getNodeName()))
			// return new BagItem(appl);
			// if (Constants.KSEQUENCE.endsWith(appl.getNodeName()))
			// return new KSequence(appl);
			// if (KSorts.MAP.endsWith(appl.getNodeName()))
			// return new Map(appl);
			// if (KSorts.MAP_ITEM.endsWith(appl.getNodeName()))
			// return new MapItem(appl);
			// if (Constants.CONTEXT.endsWith(appl.getNodeName()))
			// return new Context(appl);
			// if (Constants.HOLE.endsWith(appl.getNodeName()))
			// return Hole.KITEM_HOLE;
			// if (Constants.FREEZERHOLE.endsWith(appl.getNodeName()))
			// return new FreezerHole(appl);
			// if (KSorts.LIST.endsWith(appl.getNodeName()))
			// return new List(appl);
			// if (KSorts.LIST_ITEM.endsWith(appl.getNodeName()))
			// return new ListItem(appl);
			// if (Constants.DEFINITION.endsWith(appl.getNodeName()))
			// return new Definition(appl);
			if (Constants.AMB.equals(appl.getName()))
				return new Ambiguity(appl);
			// if (Constants.MODULECOMMENT.endsWith(appl.getNodeName()))
			// return new LiterateModuleComment(appl);
			// if (Constants.DEFCOMMENT.endsWith(appl.getNodeName()))
			// return new LiterateDefinitionComment(appl);
			// if (Constants.TAG.endsWith(appl.getNodeName()))
			// return new Attribute(appl);
			// if (Constants.ATTRIBUTES.endsWith(appl.getNodeName()))
			// return new Attributes(appl);
		}
		System.out.println(">>> " + atm + " <<< - unimplemented yet: org.kframework.kil.loader.JavaClassesFactory");
		return null;
	}
}

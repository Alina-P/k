package org.kframework.kcheck.utils;

import java.util.ArrayList;
import java.util.List;

import org.kframework.compile.utils.MetaK;
import org.kframework.kil.ASTNode;
import org.kframework.kil.KApp;
import org.kframework.kil.KLabelConstant;
import org.kframework.kil.KList;
import org.kframework.kil.KSequence;
import org.kframework.kil.Module;
import org.kframework.kil.ModuleItem;
import org.kframework.kil.Rule;
import org.kframework.kil.Sentence;
import org.kframework.kil.Term;
import org.kframework.kil.TermCons;
import org.kframework.kil.Variable;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.CopyOnWriteTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;

public class AddCircularityRules extends CopyOnWriteTransformer {

	public static final String RRULE_ATTR = "reachability-rule";
	
	private List<ASTNode> reachabilityRules;

	public AddCircularityRules(Context context, List<ASTNode> reachabilityRules) {
		super("Add circularity rules", context);
		this.reachabilityRules = reachabilityRules;
	}

	@Override
	public ASTNode transform(Module node) throws TransformerException {

        ArrayList<ModuleItem> items = new ArrayList<ModuleItem>(node.getItems());
        Module module = node.shallowCopy();
        module.setItems(items);

		
		for (ASTNode rr : reachabilityRules) {
			if (rr instanceof Sentence) {
				Sentence r = (Sentence) rr;
				
				// "parse" the reachability rules
				ReachabilityRuleKILParser parser = new ReachabilityRuleKILParser(
						context);
				r.accept(parser);

				Term newPi = parser.getPi().shallowCopy();
				Variable K = Variable.getFreshVar("K");

				// extract the content of the K cell (PGM) from LHS of  
				// the reachability rule and replace it by PGM ~> K
				ExtractCellContent extract = new ExtractCellContent(context, "k");
				newPi.accept(extract);

				List<Term> cnt = new ArrayList<Term>();
				cnt.add(extract.getContent());
				cnt.add(K);
				KSequence newContent = new KSequence(cnt);

				SetCellContent app = new SetCellContent(context, newContent, "k");
				newPi = (Term) newPi.accept(app);

				// in RHS, replace .K with K
				Term newPiPrime = parser.getPi_prime().shallowCopy();
				SetCellContent appPrime = new SetCellContent(context, K, "k");
				newPiPrime = (Term) newPiPrime.accept(appPrime);
				
				// fresh variables
				VariablesVisitor vvleft = new VariablesVisitor(context);
				newPi.accept(vvleft);
				
				VariablesVisitor vvright = new VariablesVisitor(context);
				newPiPrime.accept(vvright);
				
//				System.out.println("VL : " + vvleft.getVariables());
//				System.out.println("RL : " + vvright.getVariables());
				
				List<Term> fresh = new ArrayList<Term>();
				
				for(Variable v : vvright.getVariables()){
					if (!varInList(v, vvleft.getVariables())){
						List<Term> vlist = new ArrayList<Term>();
						vlist.add(v);
						fresh.add(new TermCons(v.getSort(), MetaK.Constants.freshCons, vlist, context));
					}
				}

				Term condition = KApp.of(KLabelConstant.ANDBOOL_KLABEL, new KList(fresh));

				Rule circRule = new Rule(newPi, newPiPrime, context);
				circRule.setCondition(condition);
				int correspondingIndex = reachabilityRules.indexOf(rr);
				circRule.addAttribute(RRULE_ATTR, correspondingIndex + "");
				
				items.add(circRule);
			}
		}

		return module;
	}
	
	public static boolean varInList(Variable v, List<Variable> vars) {
		for(Variable var : vars){
			if (v.getName().equals(var.getName())) {
				return true;
			}
		}
		return false;
	}
}

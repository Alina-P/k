package org.kframework.kcheck.utils;

import java.util.List;

import org.kframework.backend.symbolic.AddConditionToConfig;
import org.kframework.backend.symbolic.AddPathCondition;
import org.kframework.compile.utils.MetaK;
import org.kframework.kil.ASTNode;
import org.kframework.kil.Cell;
import org.kframework.kil.Cell.Ellipses;
import org.kframework.kil.KApp;
import org.kframework.kil.KLabelConstant;
import org.kframework.kil.Rewrite;
import org.kframework.kil.Rule;
import org.kframework.kil.StringBuiltin;
import org.kframework.kil.Term;
import org.kframework.kil.Variable;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.CopyOnWriteTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;

public class AddPathConditionToCircularities extends CopyOnWriteTransformer {

	public AddPathConditionToCircularities(Context context, List<ASTNode> reachabilityRules) {
		super("Add path condition to circularities", context);
	}

	@Override
	public ASTNode transform(Rule node) throws TransformerException {
		
		if(node.getAttribute(AddCircularityRules.RRULE_ATTR) != null && (node.getBody() instanceof Rewrite)) {

			// extract phi and phi'
			Term cnd = node.getCondition();
			ExtractPatternless ep = new ExtractPatternless(context, false);
			cnd = (Term) cnd.accept(ep);
			
			// separate left and right
			Rewrite ruleBody = (Rewrite) node.getBody();
			Term left = ruleBody.getLeft().shallowCopy();
			Term right = ruleBody.getRight().shallowCopy();
			
			
			// create lhs path condition cell
			Variable psi = Variable.getFreshVar("K");
            Cell leftCell = new Cell();
            leftCell.setLabel(MetaK.Constants.pathCondition);
            leftCell.setEllipses(Ellipses.NONE);
            leftCell.setContents(psi);
			left = AddConditionToConfig.addSubcellToCell((Cell)left, leftCell);

			// create rhs path condition cell
            Cell rightCell = new Cell();
            rightCell.setLabel(MetaK.Constants.pathCondition);
            rightCell.setEllipses(Ellipses.NONE);
            rightCell.setContents(KApp.of(KLabelConstant.ANDBOOL_KLABEL, psi, ep.getPhi(), ep.getPhiPrime()));
			right = AddConditionToConfig.addSubcellToCell((Cell)right, rightCell);

			// condition
			Term implication = KApp.of(KLabelConstant.BOOL_ANDBOOL_KLABEL, psi, KApp.of(KLabelConstant.NOTBOOL_KLABEL, ep.getPhi()));
			KApp unsat = StringBuiltin.kAppOf("unsat");
	        KApp checkSat = KApp.of(KLabelConstant.of("'checkSat", context), implication);
	        implication = KApp.of(KLabelConstant.KEQ_KLABEL, checkSat, unsat);
	        Term pc = KApp.of(KLabelConstant.BOOL_ANDBOOL_KLABEL, psi, ep.getPhiPrime());
			pc = AddPathCondition.checkSat(pc, context);
			
			Rule newRule = new Rule(left, right, context);
			cnd = KApp.of(KLabelConstant.ANDBOOL_KLABEL, cnd, implication, pc);
			newRule.setCondition(cnd);
			newRule.setAttributes(node.getAttributes().shallowCopy());
			return newRule;
		}
		
		return super.transform(node);
	}
}

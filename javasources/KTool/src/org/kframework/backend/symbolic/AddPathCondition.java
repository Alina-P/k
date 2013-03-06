package org.kframework.backend.symbolic;

import java.util.ArrayList;
import java.util.List;

import org.kframework.compile.utils.MetaK;
import org.kframework.kil.ASTNode;
import org.kframework.kil.Bag;
import org.kframework.kil.Cell;
import org.kframework.kil.Cell.Ellipses;
import org.kframework.kil.Constant;
import org.kframework.kil.KApp;
import org.kframework.kil.KList;
import org.kframework.kil.Rewrite;
import org.kframework.kil.Rule;
import org.kframework.kil.Term;
import org.kframework.kil.Variable;
import org.kframework.kil.visitors.BasicTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;

public class AddPathCondition extends BasicTransformer {

	public AddPathCondition() {
		super("Add Path Condition to each rule");
	}

	@Override
	public ASTNode transform(Rule node) throws TransformerException {
		if (node.getCondition() == null)
			return node;
		
		if (node.getBody() instanceof Rewrite && node.getAttribute(SymbolicBackend.SYMBOLIC) != null)
		{
			Rewrite rew = (Rewrite) node.getBody();
			
			// variable holding the formula
			Variable phi = MetaK.getFreshVar("K");
			
			// create lhs path condition cell
			Term left = rew.getLeft();
			
			// ignore non-bag and non-cell terms
			
			Cell leftCell = new Cell();
			leftCell.setLabel(MetaK.Constants.pathCondition);
			leftCell.setEllipses(Ellipses.NONE);
			leftCell.setContents(phi);

			
			
			if (left instanceof Cell) {
				AddConditionToConfig.addCellNextToKCell((Cell)left, leftCell);
			}
			else if (left instanceof Bag)
			{
				//
			}
			

			// create rhs path condition cell 
			Term right = rew.getRight();

			Cell rightCell = new Cell();
			rightCell.setLabel(MetaK.Constants.pathCondition);
			rightCell.setEllipses(Ellipses.NONE);
			List<Term> list = new ArrayList<Term>();
			list.add(phi);
			list.add(node.getCondition());
			rightCell.setContents(new KApp(Constant.ANDBOOL_KLABEL, new KList(list)));

			if (right instanceof Cell) {
				AddConditionToConfig.addCellNextToKCell((Cell)right, rightCell);
			}
			else if (right instanceof Bag)
			{
				// ?
			}
			
			// re-construct the rule
			node = node.shallowCopy();
			node.setBody(new Rewrite(left, right));
		}
		
		return node;
	}
}

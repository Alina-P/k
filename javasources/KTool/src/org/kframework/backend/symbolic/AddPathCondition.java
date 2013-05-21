package org.kframework.backend.symbolic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kframework.compile.utils.MetaK;
import org.kframework.kil.ASTNode;
import org.kframework.kil.Cell;
import org.kframework.kil.Cell.Ellipses;
import org.kframework.kil.KApp;
import org.kframework.kil.KLabelConstant;
import org.kframework.kil.KList;
import org.kframework.kil.Rewrite;
import org.kframework.kil.Rule;
import org.kframework.kil.StringBuiltin;
import org.kframework.kil.Term;
import org.kframework.kil.Variable;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.CopyOnWriteTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.utils.general.GlobalSettings;

/**
 * Add path condition cell to rules. Since this step is right after
 * configuration abstraction transformation then the path condition
 * cell must be added explicitly. Also, sets the call to the smt solver
 * in case --no-smt is set.
 *
 * @author andreiarusoaie
 */
public class AddPathCondition extends CopyOnWriteTransformer {

    public AddPathCondition(Context context) {
        super("Add Path Condition to each rule", context);
    }

    @Override
    /**
     * Construct the path condition by adding to the original 
     * path condition (phi) the side condition (SC) of the rule. 
     * If NOSMT is set, then the side condition of the rule
     * remains unchanged. Otherwise, we filter the condition
     * separating the predicates(P) from 'smtlib' translatable
     * expressions and we add as side condition of the rule
     * checkSat(SC-P) =/= unsat.
     */
    public ASTNode transform(Rule node) throws TransformerException {

        if (!node.containsAttribute(SymbolicBackend.SYMBOLIC)) {
            return node;
        }

        if (node.getCondition() == null)
            return node;

        Term condition = node.getCondition();
//        Term originalCondition = condition.shallowCopy();
        CollapseAndBoolTransformer cnft = new CollapseAndBoolTransformer(context);
        condition = (Term) node.getCondition().accept(cnft);

        ConditionTransformer ct = new ConditionTransformer(context);
        condition = (Term) node.getCondition().accept(ct);

        if (node.getBody() instanceof Rewrite) {
            Rewrite rew = (Rewrite) node.getBody();

            // variable holding the formula
            Variable phi = Variable.getFreshVar("K");

            // create lhs path condition cell
            Cell leftCell = new Cell();
            leftCell.setLabel(MetaK.Constants.pathCondition);
            leftCell.setEllipses(Ellipses.NONE);
            leftCell.setContents(phi);


            Term left = rew.getLeft();

            if (left instanceof Cell) {
                left = AddConditionToConfig.addSubcellToCell((Cell) left, leftCell);
            }


            // create rhs path condition cell
            Term right = rew.getRight();

            Cell rightCell = new Cell();
            rightCell.setLabel(MetaK.Constants.pathCondition);
            rightCell.setEllipses(Ellipses.NONE);
            Term pathCondition = phi;
            if (!ct.getFilteredTerms().isEmpty()) {
                List<Term> list = new ArrayList<Term>();
                list.add(phi);
                list.add(andBool(ct.getFilteredTerms()));
                pathCondition = new KApp(KLabelConstant.BOOL_ANDBOOL_KLABEL, new KList(list));
            }
            rightCell.setContents(pathCondition);

            if (right instanceof Cell) {
                right = AddConditionToConfig.addSubcellToCell((Cell) right, rightCell);
            }

            Term cond = condition;
            if (!GlobalSettings.NOSMT) {
                List<Term> myList = new ArrayList<Term>();
                myList.add(condition);
                myList.add(checkSat(pathCondition));
                cond = new KApp(KLabelConstant.ANDBOOL_KLABEL, new KList(myList));
            }
//            else {
//            	cond = originalCondition;
//            }

            // add transition attribute
//            List<Attribute> attrs = node.getAttributes().getContents();
            // bad practice
 //           attrs.add(new Attribute("transition", ""));

   //         Attributes atts = node.getAttributes().shallowCopy();
   //         atts.setContents(attrs);


            // re-construct the rule
            node = node.shallowCopy();
            node.setBody(new Rewrite(left, right, context));
//            node.setAttributes(atts);
            node.setCondition(cond);
        }

        return node;
    }

    private Term andBool(List<Term> filteredTerms) {

        Iterator<Term> it = filteredTerms.iterator();
        Term and = it.next();
        while (it.hasNext()) {
            List<Term> list = new ArrayList<Term>();
            list.add(and);
            list.add(it.next());
            and = new KApp(KLabelConstant.BOOL_ANDBOOL_KLABEL, new KList(list));
        }
        return and;
    }

    private Term checkSat(Term pathCondition) {
        // checkSat(pathCondition) =/=K # "unsat"(.KList)
        KApp unsat = StringBuiltin.kAppOf("unsat");
        KApp checkSat = KApp.of(KLabelConstant.of("'checkSat", context), pathCondition);
        return KApp.of(KLabelConstant.KNEQ_KLABEL, checkSat, unsat);
    }
}

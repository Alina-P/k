package org.kframework.compile.transformers;

import org.kframework.compile.utils.MetaK;
import org.kframework.kil.ASTNode;
import org.kframework.kil.Attribute;
import org.kframework.kil.Constant;
import org.kframework.kil.Empty;
import org.kframework.kil.KApp;
import org.kframework.kil.KInjectedLabel;
import org.kframework.kil.KList;
import org.kframework.kil.Module;
import org.kframework.kil.ModuleItem;
import org.kframework.kil.Production;
import org.kframework.kil.Rule;
import org.kframework.kil.Sort;
import org.kframework.kil.Term;
import org.kframework.kil.Variable;
import org.kframework.kil.visitors.CopyOnWriteTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.general.GlobalSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Transformer class for semantic equality.
 * @see CopyOnWriteTransformer
 */
public class AddSemanticEquality extends CopyOnWriteTransformer {

    public static final String EQUALITY_SORT = "EqualitySort";
    public static final Constant EQUALITY_PREDICATE = Constant.KLABEL(AddPredicates.predicate(EQUALITY_SORT));
    public static final Constant K_EQUALITY = Constant.KLABEL("'_=K_");
    public static final Constant KLIST_EQUALITY = Constant.KLABEL("'_=" + MetaK.Constants.KList + "_");

    private Map<String, String> equalities = new HashMap<String, String>();

    @Override
    public ASTNode transform(Module node) throws TransformerException {
        Module retNode = node.shallowCopy();
        retNode.setItems(new ArrayList<ModuleItem>(node.getItems()));

        Set<Production> eqProds = node.getSyntaxByTag(Attribute.EQUALITY.getKey());
        for (Production prod : eqProds)
            /*
             * operators tagged with "equality" must have the signature
             * Sort -> Sort -> Bool
             */
            if (prod.getSort().equals(Sort.BOOL))
                if (prod.getArity() == 2)
                    if (prod.getChildSort(0).equals(prod.getChildSort(1)))
                        if (!equalities.containsKey(prod.getChildSort(0)))
                            equalities.put(prod.getChildSort(0), prod.getKLabel());
                        else
                            GlobalSettings.kem.register(new KException(
                                    KException.ExceptionType.ERROR,
                                    KException.KExceptionGroup.CRITICAL,
                                    "redeclaration of equality for sort " + prod.getChildSort(0),
                                    prod.getFilename(),
                                    prod.getLocation()));
                    else
                        GlobalSettings.kem.register(new KException(
                                KException.ExceptionType.ERROR,
                                KException.KExceptionGroup.CRITICAL,
                                "arguments for equality expected to be of the same sort",
                                prod.getFilename(),
                                prod.getLocation()));
                else
                    GlobalSettings.kem.register(new KException(
                            KException.ExceptionType.ERROR,
                            KException.KExceptionGroup.CRITICAL,
                            "unexpected number of arguments for equality, expected 2",
                            prod.getFilename(),
                            prod.getLocation()));
            else
                GlobalSettings.kem.register(new KException(
                        KException.ExceptionType.ERROR,
                        KException.KExceptionGroup.CRITICAL,
                        "unexpected sort " + prod.getSort() + " for equality, expected sort " + Sort.BOOL,
                        prod.getFilename(),
                        prod.getLocation()));

        retNode.addConstant(EQUALITY_PREDICATE);

        for(Map.Entry<String, String> item : equalities.entrySet()) {
            String sort = item.getKey();
            Constant sortEq = Constant.KLABEL(item.getValue());
            if (MetaK.isComputationSort(sort)) {
                retNode.addSubsort(EQUALITY_SORT, sort);

                KList list = new KList();
                list.add(MetaK.getFreshVar(sort));
                list.add(MetaK.getFreshVar(sort));

                Term lhs = new KApp(K_EQUALITY, list);
                Term rhs = new KApp(sortEq, list);
                Rule rule = new Rule(lhs, rhs);
                rule.addAttribute(Attribute.FUNCTION);
                retNode.appendModuleItem(rule);
            }
        }

        Set<Production> prods = node.getSyntaxByTag("");
        for (Production prod : prods) {
            if (!prod.isSubsort()
                    && !prod.containsAttribute(Attribute.BRACKET.getKey())
                    && !prod.containsAttribute(Attribute.FUNCTION.getKey())
                    && !prod.containsAttribute(Attribute.PREDICATE.getKey())) {
                Variable KListVar1 = MetaK.getFreshVar(MetaK.Constants.KList);
                Variable KListVar2 = MetaK.getFreshVar(MetaK.Constants.KList);

                KList lhsList = new KList();
                lhsList.add(new KApp(Constant.KLABEL(prod.getKLabel()), KListVar1));
                lhsList.add(new KApp(Constant.KLABEL(prod.getKLabel()), KListVar2));

                KList rhsList = new KList();
                rhsList.add(new KApp(new KInjectedLabel(KListVar1), Empty.ListOfK));
                rhsList.add(new KApp(new KInjectedLabel(KListVar2), Empty.ListOfK));

                Term lhs = new KApp(K_EQUALITY, lhsList);
                Term rhs = new KApp(KLIST_EQUALITY, rhsList);
                Rule rule = new Rule(lhs, rhs);
                rule.addAttribute(Attribute.FUNCTION);
                retNode.appendModuleItem(rule);
            }
        }

        /*
        Variable var = MetaK.getFreshVar(MetaK.Constants.K);
        KList list = new KList();
        list.add(var);
        list.add(var);
        Rule rule = new Rule(new KApp(K_EQUALITY, list), Constant.TRUE);
        rule.addAttribute(Attribute.EQUALITY);
        retNode.appendModuleItem(rule);


        for (String sort : retNode.getAllSorts()) {
            if (MetaK.isComputationSort(sort) && !equalities.containsKey(sort)) {
                String symCtor = AddSymbolicK.symbolicConstructor(sort);
                Term symTerm = new KApp(Constant.KLABEL(symCtor), MetaK.getFreshVar(MetaK.Constants.KList));
                KList list = new KList();
                list.add(symTerm);
                list.add(symTerm);
                Rule rule = new Rule(new KApp(K_EQUALITY, list), Constant.TRUE);
                rule.addAttribute(Attribute.EQUALITY);
                retNode.appendModuleItem(rule);
            }
        }
        */

        /*
        Set<Production> prods = node.getSyntaxByTag("");
        for (Production prod : prods) {
            if (!prod.isSubsort()
                    && !prod.containsAttribute(Attribute.BRACKET.getKey())
                    && !prod.containsAttribute(Attribute.FUNCTION.getKey())
                    && !prod.containsAttribute(Attribute.PREDICATE.getKey())) {
                Variable KListVar1 = MetaK.getFreshVar(MetaK.Constants.KList);
                Variable KListVar2 = MetaK.getFreshVar(MetaK.Constants.KList);

                KList lhsList = new KList();
                lhsList.add(new KApp(Constant.KLABEL(prod.getKLabel()), KListVar1));
                lhsList.add(new KApp(Constant.KLABEL(prod.getKLabel()), KListVar2));

                KList rhsList = new KList();
                rhsList.add(new KApp(new KInjectedLabel(KListVar1), Empty.ListOfK));
                rhsList.add(new KApp(new KInjectedLabel(KListVar2), Empty.ListOfK));

                Term lhs = new KApp(K_EQUALITY, lhsList);
                Term rhs = new KApp(KLIST_EQUALITY, rhsList);
                Rule rule = new Rule(lhs, rhs);
                rule.addAttribute(Attribute.FUNCTION);
                retNode.appendModuleItem(rule);
            }
        }
        */

        return retNode;
    }

    public AddSemanticEquality() {
        super("Define semantic equality");
    }
}

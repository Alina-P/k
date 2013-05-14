package org.kframework.compile.transformers;

import org.kframework.compile.utils.SyntaxByTag;
import org.kframework.compile.utils.MetaK;
import org.kframework.kil.ASTNode;
import org.kframework.kil.Attribute;
import org.kframework.kil.BoolBuiltin;
import org.kframework.kil.Constant;
import org.kframework.kil.Empty;
import org.kframework.kil.IntBuiltin;
import org.kframework.kil.KApp;
import org.kframework.kil.KInjectedLabel;
import org.kframework.kil.KLabelConstant;
import org.kframework.kil.KList;
import org.kframework.kil.Module;
import org.kframework.kil.ModuleItem;
import org.kframework.kil.Production;
import org.kframework.kil.Rule;
import org.kframework.kil.Term;
import org.kframework.kil.Variable;
import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.kil.visitors.CopyOnWriteTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ResolveBinder extends CopyOnWriteTransformer {

    private static final KLabelConstant BINDER_PREDICATE
            = KLabelConstant.ofStatic(AddPredicates.predicate("Binder"));
    private static final KLabelConstant BOUNDED_PREDICATE
            = KLabelConstant.ofStatic(AddPredicates.predicate("Bound"));
    private static final KLabelConstant BOUNDING_PREDICATE
            = KLabelConstant.ofStatic(AddPredicates.predicate("Bounding"));

    private static final String REGEX
            = "\\s*(\\d+)(\\s*-\\>\\s*(\\d+))?\\s*(,?)";

    public ResolveBinder(DefinitionHelper definitionHelper) {
        super("Resolve binder", definitionHelper);
    }

    @Override
    public ASTNode transform(Module node) throws TransformerException {
        Set<Production> prods = SyntaxByTag.get(node, "binder", definitionHelper);
        if (prods.isEmpty())
            return node;

        List<ModuleItem> items = new ArrayList<ModuleItem>(node.getItems());
        node = node.shallowCopy();
        node.setItems(items);

        for (Production prod : prods) {
            String bindInfo = prod.getAttribute("binder");
            if (bindInfo == null || bindInfo.equals(""))
                bindInfo = "1->" + prod.getArity();
            Pattern p = Pattern.compile(REGEX);
            Matcher m = p.matcher(bindInfo);
            Map<Integer, Integer> bndMap = new HashMap<Integer, Integer>();

            while (m.regionStart() < m.regionEnd()) {
                if (!m.lookingAt()) {
                    System.err.println("[error:] could not parse binder attribute \"" + bindInfo.substring(m.regionStart(), m.regionEnd()) + "\"");
                    System.exit(1);
                }
                if (m.end() < m.regionEnd()) {
                    if (!m.group(4).equals(",")) {
                        System.err.println("[error:] expecting ',' at the end \"" + m.group() + "\"");
                        System.exit(1);
                    }
                } else {
                    if (!m.group(4).equals("")) {
                        System.err.println("[error:] unexpected ',' at the end \"" + m.group() + "\"");
                        System.exit(1);
                    }
                }

                int bndIdx = Integer.parseInt(m.group(1));
                if (m.group(3) == null) {
                    for (int idx = 1; idx <= prod.getArity(); idx++) {
                        if (idx != bndIdx)
                            bndMap.put(bndIdx, idx);
                    }
                } else {
                    bndMap.put(bndIdx, Integer.parseInt(m.group(3)));
                }

                m.region(m.end(), m.regionEnd());
            }

            Rule rule = new Rule(
                    KApp.of(BINDER_PREDICATE, MetaK.getTerm(prod, definitionHelper)),
                    BoolBuiltin.TRUE, definitionHelper);
            rule.addAttribute(Attribute.ANYWHERE);
            items.add(rule);

            Term klblK = KApp.of(new KInjectedLabel(KLabelConstant.ofStatic(prod.getKLabel())));

            for (int bndIdx : bndMap.keySet()) {
                KList list = new KList();
                list.getContents().add(klblK);
                list.getContents().add(IntBuiltin.kAppOf(bndIdx));
                rule = new Rule(new KApp(BOUNDED_PREDICATE, list), BoolBuiltin.TRUE, definitionHelper);
                rule.addAttribute(Attribute.ANYWHERE);
                items.add(rule);
				String bndSort = prod.getChildSort(bndIdx - 1);
				items.add(AddPredicates.getIsVariableRule(new Variable(MetaK.Constants.anyVarSymbol,bndSort), definitionHelper));
            }

            for (int bodyIdx : bndMap.values()) {
                KList list = new KList();
                list.getContents().add(klblK);
                list.getContents().add(IntBuiltin.kAppOf(bodyIdx));
                rule = new Rule(new KApp(BOUNDING_PREDICATE, list), BoolBuiltin.TRUE, definitionHelper);
                rule.addAttribute(Attribute.ANYWHERE);
                items.add(rule);
            }
/*
if (bndIdx == 0 || bndIdx > prod.getArity())  {
          System.err.println("[error:] argument index out of bounds: " + bndIdx);
          System.exit(1);
        }

if (bndMap.containsKey(bndIndex)) {
            System.err.println("[error:] " + bndIdx );
            System.exit(1);
          }
*/
        }

        return node;
    }
}

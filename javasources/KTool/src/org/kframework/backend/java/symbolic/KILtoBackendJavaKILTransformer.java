package org.kframework.backend.java.symbolic;

import com.google.common.collect.ImmutableList;

import org.kframework.backend.java.kil.BuiltinConstant;
import org.kframework.backend.java.kil.Cell;
import org.kframework.backend.java.kil.CellCollection;
import org.kframework.backend.java.kil.KItem;
import org.kframework.backend.java.kil.KLabelConstant;
import org.kframework.backend.java.kil.KLabelFreezer;
import org.kframework.backend.java.kil.Hole;
import org.kframework.backend.java.kil.KLabelInjection;
import org.kframework.backend.java.kil.KLabel;
import org.kframework.backend.java.kil.KList;
import org.kframework.backend.java.kil.KSequence;
import org.kframework.backend.java.kil.Kind;
import org.kframework.backend.java.kil.Map;
import org.kframework.backend.java.kil.Rule;
import org.kframework.backend.java.kil.Term;
import org.kframework.backend.java.kil.Variable;
import org.kframework.kil.ASTNode;
import org.kframework.kil.KSorts;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.CopyOnWriteTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: andrei
 * Date: 3/18/13
 * Time: 4:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class KILtoBackendJavaKILTransformer extends CopyOnWriteTransformer {

    public KILtoBackendJavaKILTransformer(Context context) {
        super("Transform KIL into java backend KIL", context);
    }

    @Override
    public ASTNode transform(org.kframework.kil.KApp node) throws TransformerException {
        KLabel kLabel = (KLabel) node.getLabel().accept(this);
        KList kList = (KList) node.getChild().accept(this);

        return new KItem(kLabel, kList, this.context);
    }

    @Override
    public ASTNode transform(org.kframework.kil.KLabelConstant node) throws TransformerException {
        return new KLabelConstant(node.getLabel(), this.context);
    }

    @Override
    public ASTNode transform(org.kframework.kil.FreezerLabel node) throws TransformerException {
        Term term = (Term) node.getTerm().accept(this);
        return new KLabelFreezer(term);
    }

    @Override
    public ASTNode transform(org.kframework.kil.KInjectedLabel node) throws TransformerException {
        Term term = (Term) node.getTerm().accept(this);
        return new KLabelInjection(term);
    }

    @Override
    public ASTNode transform(org.kframework.kil.Token node) throws TransformerException {
        return new BuiltinConstant(node.value(), node.tokenSort());
    }

    @Override
    public ASTNode transform(org.kframework.kil.Constant node) throws TransformerException {
        /* no support for token yet */
        throw new UnsupportedOperationException();
    }

    @Override
    public ASTNode transform(org.kframework.kil.KSequence node) throws TransformerException {
        List<org.kframework.kil.Term> flatList = new ArrayList<org.kframework.kil.Term>();
        KILtoBackendJavaKILTransformer.flattenKSequence(flatList, node.getContents());

        Variable variable = null;
        if (!flatList.isEmpty()
                && flatList.get(flatList.size() - 1) instanceof org.kframework.kil.Variable
                && flatList.get(flatList.size() - 1).getSort().equals(KSorts.KITEM)) {
            variable = (Variable) flatList.remove(flatList.size() - 1).accept(this);
        }

        ImmutableList.Builder<Term> builder = new ImmutableList.Builder<Term>();
        for (org.kframework.kil.Term term : flatList) {
            builder.add((Term) term.accept(this));
        }

        return new KSequence(builder.build(), variable);
    }

    private static void flattenKSequence(
            List<org.kframework.kil.Term> flatList,
            List<org.kframework.kil.Term> nestedList) {
        for (org.kframework.kil.Term term : nestedList) {
            if (term instanceof org.kframework.kil.KSequence) {
                org.kframework.kil.KSequence kSequence = (org.kframework.kil.KSequence) term;
                KILtoBackendJavaKILTransformer.flattenKSequence(flatList, kSequence.getContents());
            } else {
                flatList.add(term);
            }
        }
    }

    @Override
    public ASTNode transform(org.kframework.kil.KList node) throws TransformerException {
        List<org.kframework.kil.Term> flatList = new ArrayList<org.kframework.kil.Term>();
        KILtoBackendJavaKILTransformer.flattenKList(flatList, node.getContents());

        Variable variable = null;
        if (!flatList.isEmpty()
                && flatList.get(flatList.size() - 1) instanceof org.kframework.kil.Variable
                && flatList.get(flatList.size() - 1).getSort().equals(KSorts.KLIST)) {
            variable = (Variable) flatList.remove(flatList.size() - 1).accept(this);
        }

        ImmutableList.Builder<Term> builder = new ImmutableList.Builder<Term>();
        for (org.kframework.kil.Term term : flatList) {
            builder.add((Term) term.accept(this));
        }

        return new KList(builder.build(), variable);
    }

    private static void flattenKList(
            List<org.kframework.kil.Term> flatList,
            List<org.kframework.kil.Term> nestedList) {
        for (org.kframework.kil.Term term : nestedList) {
            if (term instanceof org.kframework.kil.KList) {
                org.kframework.kil.KList kList = (org.kframework.kil.KList) term;
                KILtoBackendJavaKILTransformer.flattenKList(flatList, kList.getContents());
            } else {
                flatList.add(term);
            }
        }
    }

    @Override
    public ASTNode transform(org.kframework.kil.Cell node) throws TransformerException {
        if (node.getContents() instanceof org.kframework.kil.Bag) {
            org.kframework.kil.Bag bag = (org.kframework.kil.Bag) node.getContents();

            java.util.Map<String, Cell> cells = new HashMap<String, Cell>();
            Variable variable = null;
            for (org.kframework.kil.Term term : bag.getContents()) {
                if (term instanceof org.kframework.kil.Cell) {
                    Cell cell = (Cell) term.accept(this);
                    cells.put(cell.getLabel(), cell);
                } else if (variable == null && term instanceof org.kframework.kil.Variable
                        && term.getSort().equals("Bag")) {
                    variable = (Variable) term.accept(this);
                } else {
                    throw new RuntimeException();
                }
            }

            return new Cell<CellCollection>(node.getLabel(), new CellCollection(cells, variable));
        } else {
            Term content = (Term) node.getContents().accept(this);
            if (content instanceof KItem) {
                return new Cell<KItem>(node.getLabel(), (KItem) content);
            } else if (content instanceof KSequence) {
                return new Cell<KSequence>(node.getLabel(), (KSequence) content);
            } else if (content instanceof KList) {
                return new Cell<KList>(node.getLabel(), (KList) content);
            } else if (content instanceof Map) {
                return new Cell<Map>(node.getLabel(), (Map) content);
            } else if (content instanceof Variable) {
                return new Cell<Term>(node.getLabel(), content);
            } else {
                throw new RuntimeException();
            }
        }
    }

    @Override
    public ASTNode transform(org.kframework.kil.Map node) throws TransformerException {
        java.util.Map<Term, Term> entries = new HashMap<Term, Term>(node.getContents().size());
        Variable variable = null;

        for (org.kframework.kil.Term term : node.getContents()) {
            if (term instanceof org.kframework.kil.MapItem) {
                org.kframework.kil.MapItem mapItem = (org.kframework.kil.MapItem) term;
                Term key = (Term) mapItem.getKey().accept(this);
                Term value = (Term) mapItem.getValue().accept(this);
                entries.put(key, value);
            } else if (variable == null && term instanceof org.kframework.kil.Variable
                    && term.getSort().equals("Map")) {
                variable = (Variable) term.accept(this);
            } else {
                throw new RuntimeException();
            }
        }

        return new Map(entries, variable);
    }

    @Override
    public ASTNode transform(org.kframework.kil.Variable node) throws TransformerException {
        if (node.getSort().equals("Bag")) {
            return new Variable(node.getName(), Kind.CELL_COLLECTION.toString());
        } else {
            return new Variable(node.getName(), node.getSort());
        }
    }

    @Override
    public ASTNode transform(org.kframework.kil.Rule node) throws TransformerException {
        assert node.getBody() instanceof org.kframework.kil.Rewrite;

        org.kframework.kil.Rewrite rewrite = (org.kframework.kil.Rewrite) node.getBody();
        Term leftHandSide = (Term) rewrite.getLeft().accept(this);
        //System.err.println(leftHandSide);
        //System.err.flush();
        Term rightHandSide = (Term) rewrite.getRight().accept(this);
        //System.err.println(rightHandSide);
        //System.err.flush();
        Term condition = null;
        if (node.getCondition() != null) {
            System.err.println("[error]: " + node.getCondition());
            System.err.flush();
            //TODO: handle conditions
            condition = (Term) node.getCondition().accept(this);
        }

        assert leftHandSide.getKind().equals(rightHandSide.getKind());

        return new Rule(leftHandSide, rightHandSide, condition, node.getAttributes());
    }

    @Override
    public ASTNode transform(org.kframework.kil.FreezerHole node) throws TransformerException {
        return Hole.HOLE;
    }

}

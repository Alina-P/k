package org.kframework.backend.java.kil;

import org.kframework.backend.java.indexing.IndexingPair;
import org.kframework.backend.java.symbolic.BottomUpVisitor;
import org.kframework.backend.java.symbolic.Evaluator;
import org.kframework.backend.java.symbolic.KILtoBackendJavaKILTransformer;
import org.kframework.backend.java.symbolic.Unifiable;
import org.kframework.backend.java.symbolic.Transformable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


/**
 * A K term in the internal representation of the Java Rewrite Engine.
 *
 * @author AndreiS
 */
public abstract class Term extends JavaSymbolicObject implements Transformable, Unifiable {

    protected final Kind kind;
    //protected final boolean normalized;

    protected Term(Kind kind) {
        this.kind = kind;
    }

    /**
     * Translates a term from the generic KIL representation ({@link org.kframework.kil.Term}) to
     * Java Rewrite Engine internal representation ({@link org.kframework.backend.java.kil.Term}).
     */
    public static Term of(org.kframework.kil.Term kilTerm, Definition definition) {
        KILtoBackendJavaKILTransformer transformer
                = new KILtoBackendJavaKILTransformer(definition.context());
        return transformer.transformTerm(kilTerm, definition);
    }

    /**
     * Returns a {@link Collection} view of .
     */
    public Collection<IndexingPair> getIndexingPairs() {
        final Collection<IndexingPair> indexingPairs = new ArrayList<IndexingPair>();
        accept(new BottomUpVisitor() {
            @Override
            public void visit(Cell cell) {
                if (cell.getLabel().equals("k")) {
                    indexingPairs.add(IndexingPair.getIndexingPair(cell.getContent()));
                } else if (cell.contentKind() == Kind.CELL_COLLECTION) {
                    super.visit(cell);
                }
            }
        });

        return indexingPairs;
    }

    /**
     * Returns {@code true} if a unification task between this term and another term cannot be
     * further decomposed into simpler unification tasks.
     */
    public abstract boolean isSymbolic();

    /**
     * Returns the kind of this term (Cell, CellCollection, KItem, K, KLabel, KList, or Map).
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Returns a new {@code Term} instance obtained from this term by evaluating pending
     * function and predicate operations.
     */
    public Term evaluate(TermContext context) {
        return (Term) this.accept(new Evaluator(context));
    }

    /**
     * Returns a new {@code Term} instance obtained from this term by applying substitution.
     */
    @Override
    public Term substitute(Map<Variable, ? extends Term> substitution, TermContext context) {
        return (Term) super.substitute(substitution, context);
    }

    /**
     * Returns a new {@code Term} instance obtained from this term by substituting variable with
     * term.
     */
    @Override
    public Term substitute(Variable variable, Term term, TermContext context) {
        return (Term) super.substitute(variable, term, context);
    }

}

package org.kframework.backend.java.kil;

import org.kframework.backend.java.symbolic.SymbolicConstraint;
import org.kframework.backend.java.symbolic.Transformer;
import org.kframework.backend.java.symbolic.Unifier;
import org.kframework.backend.java.symbolic.Utils;
import org.kframework.backend.java.symbolic.Visitor;
import org.kframework.kil.ASTNode;
import org.kframework.krun.api.io.FileSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;


/**
 * @author: AndreiS
 */
public class ConstrainedTerm extends Term {

    private final Term term;
    private final SymbolicConstraint lookups;
    private final SymbolicConstraint constraint;
    private final FileSystem fileSystem;

    public ConstrainedTerm(Term term, SymbolicConstraint lookups, SymbolicConstraint constraint) {
        super(term.kind);
        this.term = term;
        this.lookups = lookups;
        this.constraint = constraint;
        fileSystem = null; //TODO(AndreiS): copy from one ConstrainedTerm to the next
    }

    public ConstrainedTerm(Term term, SymbolicConstraint constraint, Definition definition) {
        this(term, new SymbolicConstraint(definition), constraint);
    }

    public ConstrainedTerm(Term term,  Definition definition) {
        this(term, new SymbolicConstraint(definition), new SymbolicConstraint(definition));
    }

    public SymbolicConstraint constraint() {
        return constraint;
    }
    public boolean implies(ConstrainedTerm constrainedTerm,  Definition definition) {
        return matchImplies(constrainedTerm, definition) != null;
    }

    /*
    public SymbolicConstraint match(ConstrainedTerm constrainedTerm, Definition definition) {
        SymbolicConstraint unificationConstraint = new SymbolicConstraint(definition);
        unificationConstraint.add(term, constrainedTerm.term);
        unificationConstraint.simplify();
        if (unificationConstraint.isFalse() || !unificationConstraint.isSubstitution()) {
            return null;
        }

        unificationConstraint.addAll(constrainedTerm.lookups);
        unificationConstraint.simplify();
        if (unificationConstraint.isFalse() || !unificationConstraint.isSubstitution()) {
            return null;
        }


    }
    */

    public SymbolicConstraint matchImplies(ConstrainedTerm constrainedTerm, Definition definition) {
        SymbolicConstraint unificationConstraint = new SymbolicConstraint(definition);
        unificationConstraint.add(term, constrainedTerm.term);
        unificationConstraint.simplify();
        if (unificationConstraint.isFalse() || !unificationConstraint.isSubstitution()) {
            return null;
        }

        SymbolicConstraint implicationConstraint = new SymbolicConstraint(definition);
        implicationConstraint.addAll(unificationConstraint);
        implicationConstraint.addAll(constrainedTerm.lookups);
        implicationConstraint.addAll(constrainedTerm.constraint);
        implicationConstraint.simplify();

        unificationConstraint.addAll(constraint);
        unificationConstraint.simplify();
        if (!unificationConstraint.implies(implicationConstraint)) {
            return null;
        }

        unificationConstraint.addAll(implicationConstraint);

        return unificationConstraint;
    }

    ///**
    // * Simplify map lookups.
    // */
    //public ConstrainedTerm simplifyLookups() {
    //    for (SymbolicConstraint.Equality equality : constraint.equalities())
    //}

    public Term term() {
        return term;
    }

    public Collection<SymbolicConstraint> unify(ConstrainedTerm constrainedTerm,  Definition definition) {
        if (!term.kind.equals(constrainedTerm.term.kind)) {
            return Collections.emptyList();
        }

        SymbolicConstraint unificationConstraint = new SymbolicConstraint(definition);
        unificationConstraint.add(term, constrainedTerm.term());
        unificationConstraint.simplify();
        if (unificationConstraint.isFalse()) {
            return Collections.emptyList();
        }

        Collection<SymbolicConstraint> constraints = new ArrayList<SymbolicConstraint>();
        for (SymbolicConstraint solution : unificationConstraint.getMultiConstraints()) {
            solution.addAll(constrainedTerm.lookups);
            solution.addAll(constrainedTerm.constraint);
            solution.addAll(constraint);

            solution.simplify();
            if (solution.isFalse()) {
                continue;
            }

            if (solution.checkUnsat()) {
                continue;
            }

            constraints.add(solution);
        }

        return constraints;
    }

    @Override
    public boolean isSymbolic() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ConstrainedTerm)) {
            return false;
        }

        ConstrainedTerm constrainedTerm = (ConstrainedTerm) object;
        return term.equals(constrainedTerm.term) && lookups.equals(constrainedTerm.lookups)
               && constraint.equals(constrainedTerm.constraint);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * Utils.HASH_PRIME + term.hashCode();
        hash = hash * Utils.HASH_PRIME + lookups.hashCode();
        hash = hash * Utils.HASH_PRIME + constraint.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return term + SymbolicConstraint.SEPARATOR + constraint;
    }

    @Override
    public ASTNode accept(Transformer transformer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(Unifier unifier, Term patten) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(Visitor visitor) {
        throw new UnsupportedOperationException();
    }

}

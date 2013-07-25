package org.kframework.backend.java.kil;

import org.kframework.backend.java.symbolic.Unifier;
import org.kframework.backend.java.symbolic.Transformer;
import org.kframework.backend.java.symbolic.Utils;
import org.kframework.backend.java.symbolic.Visitor;
import org.kframework.kil.ASTNode;

import java.util.*;

/**
 *
 * @author TraianSF
 */
public class SetUpdate extends Term {

    /** {@link org.kframework.backend.java.kil.Term} representation of the map */
    private final Term set;
    /** {@code Set} of keys to be removed from the map */
    private final Set<Term> removeSet;
    /** {@code Map} of entries to be updated in the map */
    private final Set<Term> updateSet;

    public SetUpdate(Term set, Set<Term> removeSet, Set<Term> updateSet) {
        super(Kind.KITEM);
        this.set = set;
        this.removeSet = new HashSet<Term>(removeSet);
        this.updateSet = updateSet;
    }

    public Term evaluateUpdate() {
        if (removeSet.isEmpty() && updateSet().isEmpty()) {
            return set;
        }

        if (!(set instanceof BuiltinSet)) {
            return this;
        }

        BuiltinSet builtinSet = ((BuiltinSet) set);

        Set<Term> entries = new HashSet<Term>(builtinSet.elements());
        for (Iterator<Term> iterator = removeSet.iterator(); iterator.hasNext();) {
            if (entries.remove(iterator.next())) {
                iterator.remove();
            }
        }

        if (!removeSet.isEmpty()) {
            return this;
        }

        entries.addAll(updateSet);

        if (builtinSet.hasFrame()) {
            return new BuiltinSet(entries, builtinSet.frame());
        } else {
            return new BuiltinSet(entries);
        }
    }

    public Term base() {
        return set;
    }

    public Set<Term> removeSet() {
        return Collections.unmodifiableSet(removeSet);
    }

    public Set<Term> updateSet(){
        return Collections.unmodifiableSet(updateSet);
    }

    @Override
    public boolean isSymbolic() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * Utils.HASH_PRIME + set.hashCode();
        hash = hash * Utils.HASH_PRIME + removeSet.hashCode();
        hash = hash * Utils.HASH_PRIME + updateSet.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof SetUpdate)) {
            return false;
        }

        SetUpdate mapUpdate = (SetUpdate) object;
        return set.equals(mapUpdate.set) && removeSet.equals(mapUpdate.removeSet)
               && updateSet.equals(mapUpdate.updateSet);
    }

    @Override
    public String toString() {
        String s = set.toString();
        for (Term key : removeSet) {
            s += "[" + key + " <- undef]";
        }
        for (Term key : updateSet) {
            s += "[" + key + " <- true]";
        }
        return s;
    }

    @Override
    public void accept(Unifier unifier, Term patten) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ASTNode accept(Transformer transformer) {
        return transformer.transform(this);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}

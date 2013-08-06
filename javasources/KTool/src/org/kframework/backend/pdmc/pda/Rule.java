package org.kframework.backend.pdmc.pda;

import java.util.List;
import java.util.Stack;

/**
 * @author Traian
 */
public class Rule<Control, Alphabet> {
    ConfigurationHead<Control, Alphabet> lhs;
    Configuration<Control, Alphabet> rhs;

    public Rule(ConfigurationHead<Control, Alphabet> lhs, Configuration<Control, Alphabet> rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public Configuration<Control, Alphabet> endConfiguration() {
        return rhs;
    }

    public Control endState() {
        return rhs.getHead().getState();
    }

    public Stack<Alphabet> endStack() {
        Alphabet letter = rhs.getHead().getLetter();
        if (letter == null) return Configuration.<Alphabet>emptyStack();
        Stack<Alphabet> stack = rhs.getStack();
        if (stack == null) {
            stack = new Stack<Alphabet>();
        }
        stack.push(letter);
        return stack;
    }

    public ConfigurationHead<Control, Alphabet> getHead() {
        return lhs;
    }
}

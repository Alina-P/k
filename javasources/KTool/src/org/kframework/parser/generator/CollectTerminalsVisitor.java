package org.kframework.parser.generator;

import java.util.HashSet;
import java.util.Set;

import org.kframework.kil.Terminal;
import org.kframework.kil.UserList;
import org.kframework.kil.visitors.BasicVisitor;

public class CollectTerminalsVisitor extends BasicVisitor {
	public Set<String> terminals = new HashSet<String>();

	private void addTerminal(String terminal) {
		if (terminal.equals(""))
			return;
		terminals.add(terminal);
	}

	public void visit(Terminal t) {
		addTerminal(t.getTerminal());
	}

	public void visit(UserList ul) {
		addTerminal(ul.getSeparator());
	}
}

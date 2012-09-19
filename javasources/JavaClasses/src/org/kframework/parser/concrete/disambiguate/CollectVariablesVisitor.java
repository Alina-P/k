package org.kframework.parser.concrete.disambiguate;

import java.util.ArrayList;
import java.util.HashMap;

import org.kframework.kil.Variable;
import org.kframework.kil.visitors.BasicVisitor;


public class CollectVariablesVisitor extends BasicVisitor {
	private java.util.Map<String, java.util.List<Variable>> vars = new HashMap<String, java.util.List<Variable>>();

	public java.util.Map<String, java.util.List<Variable>> getVars() {
		return vars;
	}

	public void setVars(java.util.Map<String, java.util.List<Variable>> vars) {
		this.vars = vars;
	}

	@Override
	public void visit(Variable var) {
		if (!var.getName().equals("_"))
			if (vars.containsKey(var.getName()))
				vars.get(var.getName()).add(var);
			else {
				java.util.List<Variable> varss = new ArrayList<Variable>();
				varss.add(var);
				vars.put(var.getName(), varss);
			}
	}
}

package org.kframework.krun.api;

import org.kframework.compile.utils.RuleCompilerSteps;
import org.kframework.kil.Term;
import org.kframework.kil.Variable;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.krun.SubstitutionFilter;
import org.kframework.utils.general.GlobalSettings;

import java.util.HashMap;
import java.util.Map;

public class SearchResult {
	private KRunState state;
	private Map<String, Term> substitution;
	private Map<String, Term> rawSubstitution;
	private Context context;
	private RuleCompilerSteps compilationInfo;

	public SearchResult(KRunState state, Map<String, Term> rawSubstitution, RuleCompilerSteps compilationInfo, Context context) {
		this.state = state;
		this.rawSubstitution = rawSubstitution;
		this.context = context;
		this.compilationInfo = compilationInfo;
	}

	public Map<String, Term> getSubstitution() {
		if (substitution == null) {
			substitution = new HashMap<String, Term>();
			for (Variable var : compilationInfo.getVars()) {
				Term rawValue;
				if (GlobalSettings.sortedCells) {
					Term cellFragment = compilationInfo.getCellFragment(var);
					try {
						rawValue = (Term)cellFragment.accept(new SubstitutionFilter(rawSubstitution, context));
					} catch (TransformerException e) {
						assert false; //shouldn't happen
						rawValue = null; //for static reasons
					}
				} else {
					rawValue = rawSubstitution.get(var.getName() + ":" + var.getSort());
				}
				substitution.put(var.getName() + ":" + var.getSort(), KRunState.concretize(rawValue, context));
			}
		}
		return substitution;
	}

	public KRunState getState() {
		return state;
	}
}

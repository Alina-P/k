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
	/**
	The pretty-printed substitution mapping variables explicitly named in the search pattern to
	their bindings.
	*/
	private Map<String, Term> substitution;

	/**
	The raw substitution underlying the search result. Contains all variable bindings, including
	anonymous variables, and is not modified for pretty-printing, but instead suitable for further
	rewriting.
	*/
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
          String sort;
          // The backend doesn't have sorts, so instead it matches "KItem" and
          // a predicate. This means that unless the sort of the var is "K",
          // the var in the substituion map will always have sort "KItem".
          if (var.getSort().equals("K")) {
            sort = "K";
          } else {
            sort = "KItem";
          }
					rawValue = rawSubstitution.get(var.getName() + ":" + sort);
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

package org.kframework.krun;

import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.kil.visitors.CopyOnWriteTransformer;
import org.kframework.kil.*;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.errorsystem.KException.ExceptionType;
import org.kframework.utils.errorsystem.KException.KExceptionGroup;
import org.kframework.utils.general.GlobalSettings;

import java.util.Map;

public class SubstitutionFilter extends CopyOnWriteTransformer {

	private Map<String, Term> args;

	public SubstitutionFilter(Map<String, Term> args, DefinitionHelper definitionHelper) {
		super("Plug terms into variables", definitionHelper);
		this.args = args;
	}

	@Override
	public ASTNode transform(Variable var) {
		Term t = args.get(var.getName());
		if (t == null) {
			t = args.get(var.getName() + ":" + var.getSort(definitionHelper));
		}
		if (t == null) {
			GlobalSettings.kem.register(new KException(
				ExceptionType.ERROR,
				KExceptionGroup.CRITICAL,
				"Configuration variable missing: " + var.getName(),
				var.getFilename(), var.getLocation()));
		}
		if (t instanceof BackendTerm) {
			t.setSort(var.getSort(definitionHelper));
		}
		return t;
	}
}

package org.kframework.parser.concrete.disambiguate;

import org.kframework.kil.ASTNode;
import org.kframework.kil.TermCons;
import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.kil.visitors.BasicTransformer;
import org.kframework.kil.visitors.exceptions.PriorityException;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.errorsystem.KException.ExceptionType;
import org.kframework.utils.errorsystem.KException.KExceptionGroup;

public class InclusionFilter extends BasicTransformer {
	public InclusionFilter(String localModule, DefinitionHelper definitionHelper) {
		super("Inclusion filter", definitionHelper);
		this.localModule = localModule;
	}

	String localModule = null;

	@Override
	public ASTNode transform(TermCons tc) throws TransformerException {
		String localFile = tc.getFilename();
		String consFile = tc.getProduction(definitionHelper).getFilename();
		String consModule = tc.getProduction(definitionHelper).getOwnerModuleName();
		if (!definitionHelper.isRequiredEq(consFile, localFile)) {
			String msg = "Production " + tc.getProduction(definitionHelper).toString() + " has not been imported in this file.\n";
			msg += "	Defined in module: " + consModule + " file: " + consFile;
			KException kex = new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, tc.getFilename(), tc.getLocation());
			throw new PriorityException(kex);
		}

		if (!definitionHelper.isModuleIncludedEq(localModule, consModule)) {
			String msg = "Production " + tc.getProduction(definitionHelper).toString() + " has not been imported in this module.\n";
			msg += "	Defined in module: " + consModule + " file: " + consFile;
			KException kex = new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, tc.getFilename(), tc.getLocation());
			throw new PriorityException(kex);
		}

		return super.transform(tc);
	}
}

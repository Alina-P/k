package org.kframework.parser.concrete.disambiguate;

import org.kframework.kil.*;
import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.kil.visitors.BasicHookWorker;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.errorsystem.KException.ExceptionType;
import org.kframework.utils.errorsystem.KException.KExceptionGroup;

import java.util.ArrayList;

public class TypeSystemFilter2 extends BasicHookWorker {

	private String maxSort;

	public TypeSystemFilter2(String maxSort) {
		super("Type system");
		this.maxSort = maxSort;
	}

	public TypeSystemFilter2(TypeSystemFilter2 tsf) {
		super("Type system");
		this.maxSort = tsf.maxSort;
	}

	public ASTNode transform(Term trm) throws TransformerException {
		if (!trm.getSort().equals("K") && !trm.getSort().equals(KSorts.KITEM)
                && !trm.getSort().equals ("KResult")) {
			if (!DefinitionHelper.isSubsortedEq(maxSort, trm.getSort())) {
				String msg = "Type error detected. Expected sort " + maxSort + ", but found " + trm.getSort();
				KException kex = new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, trm.getFilename(), trm.getLocation());
				throw new TransformerException(kex);
			}
        }
		return trm;
	}

	@Override
	public ASTNode transform(Ambiguity node) throws TransformerException {
		TransformerException exception = null;
		ArrayList<Term> terms = new ArrayList<Term>();
		for (Term t : node.getContents()) {
			ASTNode result = null;
			try {
				result = t.accept(this);
				terms.add((Term) result);
			} catch (TransformerException e) {
				exception = e;
			}
		}
		if (terms.isEmpty())
			throw exception;
		if (terms.size() == 1) {
			return terms.get(0);
		}
		node.setContents(terms);
		return node;
	}

	@Override
	public ASTNode transform(Bracket node) throws TransformerException {
		node.setContent((Term) node.getContent().accept(this));
		return node;
	}

	@Override
	public ASTNode transform(Rewrite node) throws TransformerException {
		Rewrite result = new Rewrite(node);
		result.setLeft((Term) node.getLeft().accept(this));
		result.setRight((Term) node.getRight().accept(this));
		return result;
	}
}

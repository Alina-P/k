package org.kframework.krun;

import org.kframework.compile.transformers.AddEmptyLists;
import org.kframework.compile.utils.MetaK;
import org.kframework.kil.*;
import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.kil.visitors.CopyOnWriteTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;

import java.util.ArrayList;
import java.util.List;

public class FlattenDisambiguationFilter extends CopyOnWriteTransformer {
	public FlattenDisambiguationFilter() {
		super("Reflatten ambiguous syntax");
	}

	@Override
	public ASTNode transform(Ambiguity amb) throws TransformerException {
		
		if (amb.getContents().get(0) instanceof TermCons) {
			TermCons t1 = (TermCons)amb.getContents().get(0);
			if (MetaK.isComputationSort(t1.getSort())) {
				if (t1.getProduction().isListDecl()) {
					Term t2 = t1.getContents().get(1);
					UserList ul = (UserList)t1.getProduction().getItems().get(0);
					if (DefinitionHelper.isSubsortedEq(ul.getSort(), t2.getSort())) {
						t1.getContents().set(1, addEmpty(t2, t1.getSort()));
					}
					if (t2 instanceof Empty) {
						t1.getContents().set(1, new ListTerminator(ul.getSeparator()));
					}
				}
				return new KApp(
                        KLabelConstant.of(t1.getProduction().getKLabel()),
                        (Term) new KList(t1.getContents()).accept(this));
			}
		} else if (amb.getContents().get(0) instanceof Empty) {
			Empty t1 = (Empty)amb.getContents().get(0);
			if (MetaK.isComputationSort(t1.getSort())) {
				return new ListTerminator(((UserList)DefinitionHelper.listConses.get(t1.getSort()).getItems().get(0)).getSeparator());
			}
		}
		return amb;
	}

	private static Term addEmpty(Term node, String sort) {
		TermCons tc = new TermCons(sort, DefinitionHelper.listConses.get(sort).getCons());
		List<Term> contents = new ArrayList<Term>();
		contents.add(node);
		contents.add(new Empty(sort));
		tc.setContents(contents);
		return tc;
	}
}

package ro.uaic.info.fmse.loader;

import ro.uaic.info.fmse.k.Production;
import ro.uaic.info.fmse.k.ProductionItem.ProductionType;
import ro.uaic.info.fmse.visitors.BasicVisitor;

public class CollectListConsesVisitor extends BasicVisitor {

	@Override
	public void visit(Production prd) {
		if (prd.getItems().size() == 1 && prd.getItems().get(0).getType() == ProductionType.USERLIST)
			DefinitionHelper.listConses.put(prd.getSort(), prd);
	}
}

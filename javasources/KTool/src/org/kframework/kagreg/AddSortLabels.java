package org.kframework.kagreg;

import java.util.ArrayList;
import java.util.List;

import org.kframework.kil.ASTNode;
import org.kframework.kil.PriorityBlock;
import org.kframework.kil.Production;
import org.kframework.kil.ProductionItem;
import org.kframework.kil.Sort;
import org.kframework.kil.Syntax;
import org.kframework.kil.Terminal;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.CopyOnWriteTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;

public class AddSortLabels extends CopyOnWriteTransformer {

	public AddSortLabels(Context context) {
		super("AddSortLabels", context);
	}
	
	@Override
	public ASTNode transform(Syntax syntax) throws TransformerException {
		List<ProductionItem> productionItems = new ArrayList<ProductionItem>();
		productionItems.add(new Terminal("L" + syntax.getSort()));
		productionItems.add(new Sort("Id"));
		productionItems.add(new Terminal(":"));
		productionItems.add(syntax.getSort());
		Production production = new Production(syntax.getSort(), productionItems);
		List<PriorityBlock> priorityBlocks = syntax.getPriorityBlocks();
		if (priorityBlocks.size() == 0) {
			System.out.println(syntax.getSort() + " empty priorityBlocks");
			PriorityBlock priorityBlock = new PriorityBlock();
			List<Production> productions = new ArrayList<Production>();
			productions.add(production);
			priorityBlock.setProductions(productions);
			priorityBlocks.add(priorityBlock);
		} else {
			priorityBlocks.get(priorityBlocks.size() - 1).getProductions().add(production);
		}
		syntax.setPriorityBlocks(priorityBlocks);
		return syntax;
	}
}

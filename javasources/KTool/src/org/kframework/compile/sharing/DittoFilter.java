package org.kframework.compile.sharing;

import org.kframework.kil.Definition;
import org.kframework.kil.Production;
import org.kframework.kil.ProductionItem;
import org.kframework.kil.UserList;
import org.kframework.kil.visitors.BasicVisitor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class DittoFilter extends BasicVisitor{
	
	Map<String, List<Production>> productions = new HashMap<String, List<Production>>();
	
	@Override
	public void visit(Production node) {
		ProductionItem ulist = node.getItems().get(0);
		if (ulist instanceof UserList)
		{
			List<Production> p;
			if (!productions.containsKey(((UserList) ulist).getSeparator()))
			{
				p = new LinkedList<Production>();
				productions.put(((UserList) ulist).getSeparator(), p);
			}
			else{
				p = productions.get(((UserList)ulist).getSeparator());
			}
			p.add(node);
		}
	}
	
	@Override
	public void visit(Definition node) {
		super.visit(node);
		
		for(List<Production> prods : productions.values())
		{
			Production max = prods.get(0);
			for (Production p : prods)
			{
				if (max.getProductionAttributes().getContents().size() < p.getProductionAttributes().getContents().size())
				{
					max = p;
				}
			}
			
			for(Production p : prods)
			{
				p.setProductionAttributes(max.getProductionAttributes());
			}
		}
	}
}

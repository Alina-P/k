package org.kframework.compile.sharing;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.kframework.kil.Definition;
import org.kframework.kil.Production;
import org.kframework.kil.ProductionItem;
import org.kframework.kil.UserList;
import org.kframework.kil.visitors.BasicVisitor;


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
				if (max.getAttributes().getContents().size() < p.getAttributes().getContents().size())
				{
					max = p;
				}
			}
			
			for(Production p : prods)
			{
				p.setAttributes(max.getAttributes());
			}
		}
	}
}

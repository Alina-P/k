package ro.uaic.info.fmse.k;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

import ro.uaic.info.fmse.loader.Constants;
import ro.uaic.info.fmse.loader.JavaClassesFactory;
import ro.uaic.info.fmse.utils.xml.XML;

public class Syntax extends ModuleItem{
	Sort sort;
	java.util.List<PriorityBlock> priorityBlocks;
	public Syntax(Element element) {
		super(element);
		
		List<Element> sorts = XML.getChildrenElementsByTagName(element, Constants.SORT);
	
		// assumption: sorts contains only one element
		sort = (Sort)JavaClassesFactory.getTerm(sorts.get(0));
		
		this.priorityBlocks = new LinkedList<PriorityBlock>();
		List<Element> priorities = XML.getChildrenElementsByTagName(element, Constants.PRIORITY);
		for (Element priority : priorities)
			priorityBlocks.add((PriorityBlock)JavaClassesFactory.getTerm(priority));
	}
	
	@Override
	public String toString() {
		String blocks = "";
		
		for (PriorityBlock pb : priorityBlocks)
		{
			blocks += pb + "\n> ";
		}
		if (blocks.length() > 2)
			blocks = blocks.substring(0, blocks.length() -3);
		
		return "  syntax " + sort + " ::= " + blocks + "\n";
	}
	
	@Override
	public String toMaude() {
		
		String contents = "";
		for (PriorityBlock pb : priorityBlocks)
		{
			for(Production p : pb.productions)
			{
				// subsort case
				if (p.items.size() == 1 && (p.items.get(0) instanceof Sort))
				{
					ProductionItem item = p.items.get(0);
					if (item instanceof Sort)
						contents += "subsort " + p.items.get(0) + " < " + sort + " .\n";
				}
				else
				{
					contents += "op " + p.getLabel() + " : " + p.toMaude() + " -> " + sort + " .\n";
				}
			}
		}
		
		return contents;
	}
}

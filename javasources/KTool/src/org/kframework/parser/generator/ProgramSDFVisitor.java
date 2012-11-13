package org.kframework.parser.generator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kframework.kil.PriorityBlock;
import org.kframework.kil.Production;
import org.kframework.kil.ProductionItem;
import org.kframework.kil.ProductionItem.ProductionType;
import org.kframework.kil.Sort;
import org.kframework.kil.Syntax;
import org.kframework.kil.Terminal;
import org.kframework.kil.visitors.BasicVisitor;
import org.kframework.utils.StringUtil;

/**
 * Collect all the syntax declarations into containers according to their function.
 * 
 * @author Radu
 * 
 */
public class ProgramSDFVisitor extends BasicVisitor {

	public List<Production> outsides = new ArrayList<Production>();
	public List<Production> constants = new ArrayList<Production>();
	public Set<String> sorts = new HashSet<String>(); // list of inserted sorts that need to avoid the priority filter
	public Set<String> startSorts = new HashSet<String>(); // list of sorts that are start symbols
	public Set<String> listSorts = new HashSet<String>(); // list of sorts declared as being list
	public Set<String> userSort = new HashSet<String>(); // list of sorts declared by the user (to be declared later as Start symbols if no declaration for Start was found)
	public StringBuilder sdf = new StringBuilder("");

	public ProgramSDFVisitor() {
	}

	public void visit(Syntax syn) {

		userSort.add(syn.getSort().getName());
		List<PriorityBlock> prilist = new ArrayList<PriorityBlock>();
		for (PriorityBlock prt : syn.getPriorityBlocks()) {
			PriorityBlock p = new PriorityBlock();
			p.setAssoc(prt.getAssoc());

			// filter the productions according to their form
			for (Production prd : prt.getProductions()) {
				startSorts.add(prd.getSort());

				if (prd.isSubsort()) {
					outsides.add(prd);
					startSorts.add(((Sort) prd.getItems().get(0)).getName());
				} else if (prd.isConstant()) {
					constants.add(prd);
				} else if (prd.getItems().get(0).getType() == ProductionType.TERMINAL && prd.getItems().get(prd.getItems().size() - 1).getType() == ProductionType.TERMINAL) {
					outsides.add(prd);
				} else if (prd.getItems().get(0).getType() == ProductionType.USERLIST) {
					outsides.add(prd);
					listSorts.add(prd.getSort());
				} else {
					p.getProductions().add(prd);
				}
			}
			if (p.getProductions().size() > 0)
				prilist.add(p);
		}

		if (prilist.size() > 0) {
			if (prilist.size() <= 1 && (syn.getPriorityBlocks().get(0).getAssoc() == null || syn.getPriorityBlocks().get(0).getAssoc().equals(""))) {
				// weird bug in SDF - if you declare only one production in a priority block, it gives parse errors
				// you need to have at least 2 productions or a block association
				PriorityBlock prt = prilist.get(0);
				for (Production p : prt.getProductions())
					outsides.add(p);
			} else {
				sdf.append("context-free priorities\n");

				for (PriorityBlock prt : prilist) {
					if (prt.getAssoc() == null || prt.getAssoc().equals(""))
						sdf.append("{\n");
					else
						sdf.append("{ " + prt.getAssoc() + ":\n");
					for (Production p : prt.getProductions()) {
						sdf.append("	");
						List<ProductionItem> items = p.getItems();
						for (int i = 0; i < items.size(); i++) {
							ProductionItem itm = items.get(i);
							if (itm.getType() == ProductionType.TERMINAL) {
								Terminal t = (Terminal) itm;
								sdf.append("\"" + t.getTerminal() + "\" ");
							} else if (itm.getType() == ProductionType.SORT) {
								Sort srt = (Sort) itm;
								// if we are on the first or last place and this sort is not a list, just print the sort
								if (i == 0 || i == items.size() - 1) {
									sdf.append(StringUtil.escapeSortName(srt.getName()) + " ");
								} else {
									// if this sort should be inserted to avoid the priority filter, then add it to the list
									sorts.add(srt.getName());
									sdf.append("InsertDz" + StringUtil.escapeSortName(srt.getName()) + " ");
								}
							}
						}
						sdf.append("-> " + StringUtil.escapeSortName(p.getSort()));
						sdf.append(SDFHelper.getSDFAttributes(p.getAttributes()) + "\n");
					}
					sdf.append("} > ");
				}
				sdf = new StringBuilder(sdf.substring(0, sdf.length() - 3) + "\n\n");
			}
		}
	}
}

package ro.uaic.info.fmse.lists;

import java.util.LinkedList;
import java.util.List;

import ro.uaic.info.fmse.k.Empty;
import ro.uaic.info.fmse.k.Production;
import ro.uaic.info.fmse.k.Sort;
import ro.uaic.info.fmse.k.Term;
import ro.uaic.info.fmse.k.TermCons;
import ro.uaic.info.fmse.k.UserList;
import ro.uaic.info.fmse.loader.Constants;
import ro.uaic.info.fmse.loader.DefinitionHelper;
import ro.uaic.info.fmse.parsing.ASTNode;
import ro.uaic.info.fmse.parsing.Modifier;
import ro.uaic.info.fmse.utils.errors.Error;

public class EmptyListsVisitor extends Modifier {

	@Override
	public ASTNode modify(ASTNode astNode) {

		// traverse
		astNode.applyToAll(this);

		if (astNode instanceof TermCons) {
			TermCons tc = (TermCons) astNode;
			Production production = DefinitionHelper.conses.get("\""
					+ tc.getCons() + "\"");
			// System.out.println("TermCons: " + tc);
			// System.out.println("Production: " + production);

			int i = 0, j = 0;
			while (!(i >= production.getItems().size() || j >= tc.getContents()
					.size())) {
				// System.out.println("Compare: ");
				// System.out.println("\tPItem: " +
				// production.getItems().get(i));
				// System.out.println("\tTItem: " + tc.getContents().get(j));
				if (production.getItems().get(i) instanceof Sort) {
					// if the sort of the j-th term is a list sort
					String psort = production.getItems().get(i).toString();
					String tsort = tc.getContents().get(j).getSort();

					if (!psort.equals(tsort)) {
						if (isListSort(psort) && subsort(tsort, psort)) {
							List<Term> genContents = new LinkedList<Term>();
							genContents.add(tc.getContents().get(j));
							genContents.add(new Empty("generated", "generated",
									psort));

							tc.getContents().set(
									j,
									new TermCons("generated", "generated",
											psort, getListCons(psort),
											genContents));
//							System.out.println("Adding cons at "
//									+ tc.getLocation());
						}

						if (!subsort(tsort, psort) && isListSort(psort)) {
							boolean avoid = false;

							if (isListSort(psort) && isListSort(tsort)) {
								UserList ps = (UserList) (DefinitionHelper.listConses
										.get(psort).getItems().get(0));
								UserList ts = (UserList) (DefinitionHelper.listConses
										.get(tsort).getItems().get(0));

								if (ps.getSeparator().equals(ts.getSeparator()))
									avoid = true;
							}

							if (!avoid)
								Error.silentReport("Cannot infer list terminator for term:"
										+ tc
										+ " at location "
										+ tc.getLocation()
										+ " in file: "
										+ tc.getFilename()
										+ "\n    Expected sort: "
										+ psort
										+ " found sort: " + tsort);
						}
					}
					j++;
				}
				i++;
			}

			// System.out.println("\n\n");
		}

		return astNode;
	}

	private String getListCons(String psort) {
		Production p = DefinitionHelper.listConses.get(psort);
		String cons = p.getAttributes().get(Constants.CONS_cons_ATTR);
		return cons.substring(1, cons.length() - 1);
	}

	private boolean subsort(String tsort, String psort) {
		return DefinitionHelper.isSubsorted(psort, tsort);
	}

	public boolean isListSort(String sort) {
		return DefinitionHelper.listConses.containsKey(sort);
	}
}

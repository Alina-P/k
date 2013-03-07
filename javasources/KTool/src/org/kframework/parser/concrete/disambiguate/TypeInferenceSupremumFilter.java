package org.kframework.parser.concrete.disambiguate;

import org.kframework.kil.*;
import org.kframework.kil.ProductionItem.ProductionType;
import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.kil.visitors.BasicTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;

import java.util.ArrayList;

public class TypeInferenceSupremumFilter extends BasicTransformer {

	public TypeInferenceSupremumFilter() {
		super("Type inference supremum");
	}

	public ASTNode transform(Ambiguity amb) throws TransformerException {

		java.util.List<Term> terms = new ArrayList<Term>(amb.getContents());
		boolean areAllListSorts = true;
		for (Term trm : amb.getContents()) {
			if (!DefinitionHelper.isListSort(trm.getSort())) {
				areAllListSorts = false; 
				break;
			}
		}

		//if all sorts are list sorts 
    //we use a more complicated algorithm.
    //
    //1) find the LUB of the sorts of all elements of all lists in the ambiguity
    //  such that LUB is one of their sorts (no picking a LUB outside of the representative sorts)
    //2)  find the term(s) in the ambiguity that has the element sort closest to LUB.
    //    if there is more than one such term we still have an ambiguity
		if (areAllListSorts){
      Term test = amb.getContents().get(0); 
      String lubElementSort = null;
      //if the Term in the Amb isn't a TermCons, punt.
      //is that even possible?
      if(test instanceof TermCons){
        TermCons tc = (TermCons) test;
        lubElementSort = tc.getContents().get(0).getSort();
      }
END:
		  for (Term trm : amb.getContents()){
        if(!(trm instanceof TermCons)) break;
        for (Term child : ((TermCons) trm).getContents()) {
          String childSort = child.getSort();
          if(DefinitionHelper.isSubsorted(childSort, lubElementSort)){
            lubElementSort = childSort;
          } 
          //if this sort is not comparable to lubElementSort we cannot resolve the
          //ambiguity, so we check to see that it is neither equal nor a subsort 
          else if(!(DefinitionHelper.isSubsorted(lubElementSort, childSort) 
                || lubElementSort.equals(childSort))){
            lubElementSort = null;
            break END;
          } 
        }
      }
      if(lubElementSort != null){
        java.util.List<String> canidates = new ArrayList<String>();
        for(Term trm : amb.getContents()){
          canidates.add(DefinitionHelper.getListElementSort(trm.getSort()));
        }
        java.util.List<String> remainingCanidates = new ArrayList<String>();
        for(String sort : canidates){
          if(DefinitionHelper.isSubsorted(sort, lubElementSort)){ 
            remainingCanidates.add(sort);
          }
        }
        String finalElementSort = findLeastSort(remainingCanidates);
        if(finalElementSort != null){
          for(Term trm : amb.getContents()){
            if(!DefinitionHelper.getListElementSort(trm.getSort()).equals(finalElementSort)){
              terms.remove(trm);
            }
          }
        }
      }
    }
		
		// choose the maximum from the list of ambiguities
		java.util.List<Term> terms2 = new ArrayList<Term>(terms);
		for (Term trm1 : terms) {
			for (Term trm2 : terms) {
				if (trm1 != trm2)
					if (termsAlike(trm1, trm2))
						if (DefinitionHelper.isSubsorted(trm2.getSort(), trm1.getSort())) {
							terms2.remove(trm1);
					}
			}
		}
		

		if (terms2.size() == 1)
		{
			return terms2.get(0).accept(this);
		}
		else if (terms2.size() > 0)
			amb.setContents(terms2);
		//if there are 0 amb left, which I believe is theoretically possible
		//we return the original ambiguity 
		return super.transform(amb);
	}

  private static String findLeastSort(java.util.List<String> sorts){
    if(sorts.size() == 0) return null;
    String leastSort = sorts.get(0);
    for(String sort : sorts){
      if(DefinitionHelper.isSubsorted(leastSort, sort)){
        leastSort = sort;
      }
    }
    return leastSort;
  }

	/**
	 * Returns true if the terms are of the same kind (Variable, TermCons...) If they are TermCons, then all the Items must be alike also.
	 * 
	 * @param trm1
	 *						First term to compare.
	 * @param trm2
	 *						Second term to compare.
	 * @return true or false weather they are alike.
	 */
	private static boolean termsAlike(Term trm1, Term trm2) {
		if (!trm1.getClass().equals(trm2.getClass()))
			return false;

		if (trm1 instanceof TermCons) {
			TermCons term1 = (TermCons) trm1;
			TermCons term2 = (TermCons) trm2;

			if (term1.getProduction().getItems().size() != term2.getProduction().getItems().size())
				return false;

			// check to see if every non terminal is subsorted-eq to the other term.
			// add 1 to left each time Sort1 < Sort2 and add 1 to right vice versa
			// if at least one of them is 0 at the end return true.
			int left = 0, right = 0;
			if (DefinitionHelper.isSubsorted(term1.getSort(), term2.getSort()))
				left++;
			else if (DefinitionHelper.isSubsorted(term2.getSort(), term1.getSort()))
				right++;
			else if (!term1.getSort().equals(term2.getSort()))
				return false;
			for (int i = 0; i < term1.getProduction().getItems().size(); i++) {
				ProductionItem itm1 = term1.getProduction().getItems().get(i);
				ProductionItem itm2 = term2.getProduction().getItems().get(i);

				if (itm1.getType() == ProductionType.TERMINAL && !itm1.equals(itm2))
					return false;
				else if (itm1.getType() == ProductionType.USERLIST) {
					if (itm2.getType() != ProductionType.USERLIST)
						return false;
					UserList ul1 = (UserList) itm1;
					UserList ul2 = (UserList) itm2;

					if (!ul1.getSeparator().equals(ul2.getSeparator()))
						return false;

					if (DefinitionHelper.isSubsorted(ul1.getSort(), ul2.getSort()))
						left++;
					else if (DefinitionHelper.isSubsorted(ul2.getSort(), ul1.getSort()))
						right++;
					else if (!ul1.getSort().equals(ul2.getSort()))
						return false;
				} else if (itm1.getType() == ProductionType.SORT) {
					if (itm2.getType() != ProductionType.SORT)
						return false;
					// only sort possible now.
					Sort srt1 = (Sort) itm1;
					Sort srt2 = (Sort) itm2;

					if (DefinitionHelper.isSubsorted(srt1.getName(), srt2.getName()))
						left++;
					else if (DefinitionHelper.isSubsorted(srt2.getName(), srt1.getName()))
						right++;
					else if (!srt1.equals(srt2))
						return false;
				}
			}
			if (right != 0 && left != 0)
				return false;
		}

		return true;
	}
}

package org.kframework.compile.transformers;

import org.kframework.compile.utils.MetaK;
import org.kframework.kil.*;
import org.kframework.kil.visitors.CopyOnWriteTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.general.GlobalSettings;

import java.util.ArrayList;

/**
 * Initially created by: Traian Florin Serbanuta
 * <p/>
 * Date: 12/19/12
 * Time: 11:40 PM
 */
public class AddSuperheatRules extends CopyOnWriteTransformer {
	java.util.List<ModuleItem> superHeats = new ArrayList<ModuleItem>();
	public AddSuperheatRules() {
		super("Add Superheat rules");
	}

	@Override
	public ASTNode transform(Module node) throws TransformerException {
		superHeats.clear();
		node = (Module) super.transform(node);
		if (!superHeats.isEmpty()) {
			node = node.shallowCopy();
			node.setItems(new ArrayList<ModuleItem>(node.getItems()));
			node.getItems().addAll(superHeats);
			node.getItems().add(new Import("K-STRICTNESS"));
		}
		return node;
	}

	@Override
	public ASTNode transform(Configuration node) throws TransformerException {
		return node;
	}

	@Override
	public ASTNode transform(Context node) throws TransformerException {
		return node;
	}

	@Override
	public ASTNode transform(Rule node) throws TransformerException {
		if (!node.containsAttribute(MetaK.Constants.heatingTag)) {
			return node;
		}
		boolean superheat = false;
		for (String heat : GlobalSettings.superheat) {
            if (node.containsAttribute(heat)) {
				superheat = true;
				break;
            }
        }
		if (!(node.getBody() instanceof Rewrite)) {
			GlobalSettings.kem.register(
					new KException(KException.ExceptionType.ERROR,
							KException.KExceptionGroup.CRITICAL,
							"Heating rules should have rewrite at the top.",
							getName(),
							node.getFilename(),
							node.getLocation())
			);
		}
		final Rewrite body = (Rewrite) node.getBody();
		if (!superheat) {
			// rule heat(redex((C[e] =>  e ~> C) ~> _:K),, _:KList)
			KSequence kSequence = new KSequence();
			kSequence.getContents().add(body);
			kSequence.add(new Variable(MetaK.Constants.anyVarSymbol,"K"));
			Term redex = new KApp(KLabelConstant.REDEX_KLABEL,kSequence);
			KList listOfK = new KList();
			listOfK.add(redex);
			listOfK.add(new Variable(MetaK.Constants.anyVarSymbol, MetaK.Constants.KList));
			Term heat = new KApp(KLabelConstant.HEAT_KLABEL, listOfK);
			Rule superHeat = node.shallowCopy();
			superHeat.setBody(heat);
			superHeats.add(superHeat);
			return node;
		}
		// add superheat rule
		// rule heat(redex(C[e] ~> RestHeat:K,,	LHeat:KList,,
		//                 (.KList => redex(e ~> C ~> RestHeat:K))),,_:KList)
		// when '_=/=K_('_inKList_(redex(e ~> C ~> RestHeat:K),,KList2KLabel LHeat:KList(.KList)),# true(.KList))
		Rule superHeat = node.shallowCopy();
		Term left = body.getLeft(); // C[e]
		Term right = body.getRight(); // e ~> C
		Variable restHeat = MetaK.getFreshVar("K");
		Variable lHeat = MetaK.getFreshVar(MetaK.Constants.KList);
		KSequence red1Seq = new KSequence();
		red1Seq.add(left); red1Seq.add(restHeat); //C[e] ~> RestHeat:K,
		KList red1List = new KList();
		red1List.add(red1Seq);red1List.add(lHeat); //C[e] ~> RestHeat:K,,	LHeat:KList
		KSequence red2Seq = new KSequence();
		red2Seq.getContents().addAll(((KSequence)right).getContents()); red2Seq.add(restHeat); // e ~> C ~> RestHeat:K
		Term red2 = new KApp(KLabelConstant.REDEX_KLABEL,red2Seq); // redex(e ~> C ~> RestHeat:K)
		Term red2rew = new Rewrite(new Empty(MetaK.Constants.KList), red2); // (.KList => redex(e ~> C ~> RestHeat:K))
		red1List.add(red2rew);
		Term red1 = new KApp(KLabelConstant.REDEX_KLABEL, red1List); // redex(C[e] ~> RestHeat:K,,	LHeat:KList,,
															   //       (.KList => redex(e ~> C ~> RestHeat:K)))
		KList heatList = new KList();
		heatList.add(red1); heatList.add(new Variable(MetaK.Constants.anyVarSymbol, MetaK.Constants.KList));
		Term heat = new KApp(KLabelConstant.HEAT_KLABEL, heatList);
		superHeat.setBody(heat);

		KList inListList = new KList();
		inListList.add(red2); inListList.add(new KApp(new KInjectedLabel(lHeat), new Empty(MetaK.Constants.KList)));
		Term inList = new KApp(KLabelConstant.of("'_inKList_"), inListList);
		KList condList = new KList();
		condList.add(inList);
		condList.add(BoolBuiltin.TRUE);
		Term cond = new KApp(KLabelConstant.KNEQ_KLABEL, condList);
		superHeat.setCondition(MetaK.incrementCondition(node.getCondition(),cond));
		superHeats.add(superHeat);

		// replace heating rule by
		// rule C[e] => heat(redex(C[e]),, heated(.KList))
		node = node.shallowCopy();
		Term red3 = new KApp(KLabelConstant.REDEX_KLABEL,left);
		KList red3List = new KList();
		red3List.add(red3);
		red3List.add(new KApp(KLabelConstant.HEATED_KLABEL, new Empty(MetaK.Constants.KList)));
		Term heat2 = new KApp(KLabelConstant.HEAT_KLABEL, red3List);
		node.setBody(new Rewrite(left, heat2));


		return node;

	}

	@Override
	public ASTNode transform(Syntax node) throws TransformerException {
		return node;
	}
}

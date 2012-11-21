package org.kframework.kil.visitors;

import org.kframework.kil.*;

public interface Visitor {
	public void visit(ASTNode node);
	public void visit(Definition node);
	public void visit(DefinitionItem node);
	// <DefinitionItems>
	public void visit(LiterateDefinitionComment node);
	public void visit(Module node);
	public void visit(Require require);
	// </DefinitionItems>
	public void visit(ModuleItem node);
	// <ModuleItems>
	public void visit(Import node);
	public void visit(LiterateModuleComment node);
	public void visit(Sentence node);
	// <Sentences>
	public void visit(StringSentence node);
	public void visit(Configuration node);
	public void visit(Context node);
	public void visit(Rule node);
	// </Sentences>
	public void visit(Syntax node);
	public void visit(PriorityExtended node);
	// <ModuleItems>
	public void visit(PriorityBlock node);
	public void visit(PriorityBlockExtended node);
	public void visit(Production node);
	public void visit(ProductionItem node);
	// <ProductionItems>
	public void visit(Sort node);
	public void visit(Terminal node);
	public void visit(UserList node);
	// </ProductionItems>
	public void visit(Term node);
	// <Terms>
	public void visit(Cell node);
	public void visit(Collection node);
	// <Collections>
	public void visit(Ambiguity node);
	public void visit(Bag node);
	public void visit(KSequence node);
	public void visit(List node);
	public void visit(ListOfK node);
	public void visit(Map node);
	public void visit(Set node);
	// </Collections>
	public void visit(CollectionItem node);
	// <CollectionItems>
	public void visit(BagItem node);
	public void visit(ListItem node);
	public void visit(MapItem node);
	public void visit(SetItem node);
	// </CollectionItems>
	public void visit(Constant node);
	public void visit(Empty node);
	public void visit(Hole node);
	public void visit(KApp node);
	public void visit(KLabel node);
	public void visit(Rewrite node);
	public void visit(TermCons node);
	public void visit(Bracket node);
	public void visit(Variable node);
	// </Terms>
	public void visit(Attributes attributes);
	public void visit(Attribute attribute);
	public void visit(TermComment node);
	// Others
	public void visit(KInjectedLabel kInjectedLabel);

	public String getName();
}

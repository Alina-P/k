package org.kframework.compile.transformers;

import org.kframework.compile.utils.GetLhsPattern;
import org.kframework.compile.utils.MetaK;
import org.kframework.kil.*;
import org.kframework.kil.Cell.Ellipses;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.BasicVisitor;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.errorsystem.KException.ExceptionType;
import org.kframework.utils.errorsystem.KException.KExceptionGroup;
import org.kframework.utils.general.GlobalSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ResolveBlockingInput extends GetLhsPattern {
	
	Map<String, String> inputCells = new HashMap<String, String>();
	java.util.List<Rule> generated = new ArrayList<Rule>();
	boolean hasInputCell;
	
	public ResolveBlockingInput(Context context) {
		super("Resolve Blocking Input", context);
	}
	
	@Override
	public ASTNode transform(Definition node) throws TransformerException {
		Configuration config = MetaK.getConfiguration(node, context);
		config.accept(new BasicVisitor(context) {
			@Override
			public void visit(Cell node) {
				String stream = node.getCellAttributes().get("stream");
				if ("stdin".equals(stream)) {
                    String delimiter = node.getCellAttributes().get("delimiters");
                    if (delimiter == null) {
                        delimiter = " \n\t\r";
                    }
					inputCells.put(node.getLabel(), delimiter);
				}
				super.visit(node);
			}

		});
		return super.transform(node);
	}
	
	@Override
	public ASTNode transform(Module node) throws TransformerException {
		ASTNode result = super.transform(node);
		if (result != node) {
			GlobalSettings.kem.register(new KException(ExceptionType.ERROR, 
					KExceptionGroup.INTERNAL, 
					"Should have obtained the same module.", 
					getName(), node.getFilename(), node.getLocation()));					
		}
		if (generated.isEmpty()) return node;
		node = node.shallowCopy();
		node.getItems().addAll(generated);
		return node;
	}
	
	@Override
	public ASTNode transform(Configuration node) throws TransformerException {
		return node;
	}
	
	@Override
	public ASTNode transform(org.kframework.kil.Context node) throws TransformerException {
		return node;
	}
	
	@Override
	public ASTNode transform(Syntax node) throws TransformerException {
		return node;
	}
	
	@Override
	public ASTNode transform(Rule node) throws TransformerException {
		hasInputCell = false;
		ASTNode result = super.transform(node);
		if (hasInputCell) {
			generated.add((Rule)result);
		}
		return node;
	}
	
	@Override
	public ASTNode transform(Cell node) throws TransformerException {
		if ((!inputCells.containsKey(node.getLabel()))) {
			return super.transform(node);
		}
		if (!(node.getEllipses() == Ellipses.RIGHT)) {
			GlobalSettings.kem.register(new KException(ExceptionType.WARNING, 
					KExceptionGroup.COMPILER, 
					"cell should have right ellipses but it doesn't." +
							System.getProperty("line.separator") + "Won't transform.", 
							getName(), node.getFilename(), node.getLocation()));
			return node;
		}
		Term contents = node.getContents();
		if (!(contents instanceof Rewrite)) {
			GlobalSettings.kem.register(new KException(ExceptionType.WARNING, 
					KExceptionGroup.COMPILER, 
					"Expecting a rewrite of a basic type variable into the empty list but got " + contents.getClass() + "." +
							System.getProperty("line.separator") + "Won't transform.", 
							getName(), contents.getFilename(), contents.getLocation()));
			return node;
		}
		Rewrite rewrite = (Rewrite) contents;
		if (!(rewrite.getLeft() instanceof ListItem)) {
			GlobalSettings.kem.register(new KException(ExceptionType.WARNING, 
					KExceptionGroup.COMPILER, 
					"Expecting a list item but got " + rewrite.getLeft().getClass() + "." +
							System.getProperty("line.separator") + "Won't transform.", 
							getName(), rewrite.getLeft().getFilename(), rewrite.getLeft().getLocation()));
			return node;			
		}
		ListItem item = (ListItem) rewrite.getLeft();
		if (!(item.getItem() instanceof Variable //&&	MetaK.isBuiltinSort(item.getItem().getSort())
				)) {
			GlobalSettings.kem.register(new KException(ExceptionType.WARNING, 
					KExceptionGroup.COMPILER, 
					"Expecting an input type variable but got " + item.getItem().getClass() + "." +
							System.getProperty("line.separator") + "Won't transform.", 
							getName(), item.getItem().getFilename(), item.getItem().getLocation()));
			return node;
		}			
		if (!(rewrite.getRight() instanceof List && ((List) rewrite.getRight()).isEmpty())) {
			GlobalSettings.kem.register(new KException(ExceptionType.WARNING, 
					KExceptionGroup.COMPILER, 
					"Expecting an empty list but got " + rewrite.getRight().getClass() + " of sort " + 
							rewrite.getRight().getSort() + "." +
							System.getProperty("line.separator") + "Won't transform.", 
							getName(), rewrite.getRight().getFilename(), rewrite.getRight().getLocation()));
			return node;						
		}
		
		hasInputCell = true;
		
		
//		  syntax List ::= "#parseInput" "(" String ")"   [cons(List1ParseSyn)]
		TermCons parseTerm = new TermCons("ListItem", "ListItem1ParseSyn", context);
		parseTerm.getContents().add(StringBuiltin.kAppOf(item.getItem().getSort()));
        parseTerm.getContents().add(StringBuiltin.kAppOf(inputCells.get(node.getLabel())));
		
//		  syntax List ::= "#buffer" "(" K ")"           [cons(List1IOBufferSyn)]
		TermCons ioBuffer = new TermCons("ListItem", "ListItem1IOBufferSyn", context);
		ioBuffer.getContents().add(new Variable(Variable.getFreshVar("K")));
		
//		ctor(List)[replaceS[emptyCt(List),parseTerm(string(Ty),nilK)],ioBuffer(mkVariable('BI,K))]
		List list = new List();
		list.getContents().add(new Rewrite(List.EMPTY, parseTerm, context));
		list.getContents().add(ioBuffer);
		
		node = node.shallowCopy();
		node.setContents(list);
		return node;
	}
}

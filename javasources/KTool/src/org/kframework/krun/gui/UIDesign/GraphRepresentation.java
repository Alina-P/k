package org.kframework.krun.gui.UIDesign;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringEscapeUtils;
import org.kframework.backend.unparser.UnparserFilter;
import org.kframework.kil.Cell;
import org.kframework.kil.Term;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.krun.ConcretizeSyntax;
import org.kframework.krun.api.KRunState;
import org.kframework.krun.api.Transition;
import org.kframework.krun.gui.Controller.RunKRunCommand;
import org.kframework.krun.gui.UIDesign.xmlEditor.XMLEditorKit;
import org.kframework.krun.gui.diff.DiffFrame;
import org.kframework.parser.concrete.disambiguate.BestFitFilter;
import org.kframework.parser.concrete.disambiguate.GetFitnessUnitTypeCheckVisitor;
import org.kframework.parser.concrete.disambiguate.TypeInferenceSupremumFilter;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.subLayout.GraphCollapser;
//use generic graph since we modify it
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GraphRepresentation extends JPanel{

	private static final long serialVersionUID = 1017336668368978842L;
	private int index = 0;
	VisualizationViewerDemo vvd; 
	GraphZoomScrollPane gzsp ;
	GraphCollapser collapser;
	JPanel commandControl;
	static ConfigurationPanel nodeInfo;
	final JTextField numberField = new JTextField(5);
	JLabel numberOfSteps;
	JButton step;
	JButton stepAll;
	JButton collapse;
	JButton expand;
	JButton exit;
	JButton compare;
	private RunKRunCommand commandProcessor;
	public static KRunState selection;

	public GraphRepresentation(RunKRunCommand command) throws Exception{
		initCommandProcessor(command);
		initGraph();
		initCollapser();
		initNodeInfoPanel();	
		initCommandControlElements();
		addZoom();
		packComponents();
	}

	public void initCommandProcessor(RunKRunCommand command){
		this.commandProcessor = command;
	}


	public void initGraph() throws Exception{
		Graph graph = null;
		try {
			graph = commandProcessor.firstStep();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}		
		vvd = new VisualizationViewerDemo(graph);
	}

	public void initCollapser(){		
		collapser = new GraphCollapser(vvd.getLayout().getGraph());
	}

	public void initNodeInfoPanel()
	{
		nodeInfo = new ConfigurationPanel();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		nodeInfo.setPreferredSize(new Dimension(screenSize.width/3,screenSize.height));
		nodeInfo.setBorder(BorderFactory.createTitledBorder("Configuration"));
	}

	public void initCommandControlElements(){
		commandControl = new JPanel();
		numberOfSteps = new JLabel("Input number of steps:");
		step = new JButton("Step");
		stepAll = new JButton("Step-all");
		collapse = new JButton("Collapse");
		expand = new JButton("Expand");
		exit = new JButton("Exit");
		compare = new JButton("Compare");
		addActionForStepButton();
		addActionForStepAllButton();
		addActionForCollapse();
		addActionForExpand();
		addActionForExit();
		addActionForCompare();
		// compare 2 selected nodes
		addCommandPanelElements();
	}

	public void addZoom(){
		gzsp = new GraphZoomScrollPane(vvd.getVv());
	}

	public void packComponents(){		
		this.setLayout(new BorderLayout());
		JPanel controls = new JPanel();    
		controls.add(commandControl);
		this.add(gzsp);
		this.add(nodeInfo,BorderLayout.EAST);
		this.add(controls, BorderLayout.SOUTH);
	}

	public void addActionForStepButton(){
		step.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				synchronized (ActionListener.class) {
					Object [] picked = vvd.getSelectedVertices().toArray();
					if(picked.length > 0){
						for (int i = 0 ; i< picked.length;i++){
							if ( !(picked[i] instanceof KRunState))
								continue;
							KRunState pick = (KRunState)picked[i];					
							try {
								// run command just for leaves
								if (vvd.getVv().getGraphLayout().getGraph().getSuccessorCount(pick) == 0){
									commandProcessor.step(pick, determineNoOfSteps());
								}
								//reset the selection
								vvd.getVv().getPickedVertexState().pick(pick, false);
							} catch (IOException e1) {
								e1.printStackTrace();
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}
						redrawGraphAndResetScroll();
					}
					else if (picked.length == 0){
						showMessageOfSelectRequirement("Step");
					}
					resetNoOfSteps();
				}
			}
		});
	}

	public void addActionForStepAllButton(){
		stepAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				synchronized (ActionListener.class) {
					Object [] picked = vvd.getSelectedVertices().toArray();
					if(picked.length > 0){
						for (int i = 0 ; i< picked.length;i++){
							if ( !(picked[i] instanceof KRunState))
								continue;
							KRunState pick = (KRunState)picked[i];						
							try {
								// run command just for leaves
								if (vvd.getVv().getGraphLayout().getGraph().getSuccessorCount(pick) == 0 || picked.length ==1 ){
									commandProcessor.step_all(determineNoOfSteps(), pick);
								}
								//reset the selection
								vvd.getVv().getPickedVertexState().pick(pick, false);

							} catch (IOException e1) {
								e1.printStackTrace();
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}
						redrawGraphAndResetScroll();
					}
					else if (picked.length == 0){
						showMessageOfSelectRequirement("Step-all");
					}
					resetNoOfSteps();
				}
			}
		});
	}
	@SuppressWarnings("unchecked")
	public void addActionForCollapse(){
		collapse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Collection<KRunState> picked = new HashSet<KRunState>(vvd.getSelectedVertices());
				if(picked.size() > 1) {
					Graph<KRunState,Transition> inGraph = vvd.getLayout().getGraph();
					Graph clusterGraph = collapser.getClusterGraph(inGraph, picked);
					Graph g = collapser.collapse(vvd.getLayout().getGraph(), clusterGraph);
					double sumx = 0;
					double sumy = 0;
					for(Object v : picked) {
						Point2D p = (Point2D)vvd.getLayout().transform(v);
						sumx += p.getX();
						sumy += p.getY();
						//break;
					}
					Point2D cp = new Point2D.Double(sumx/picked.size(), sumy/picked.size());
					vvd.getVv().getRenderContext().getParallelEdgeIndexFunction().reset();
					vvd.getLayout().setGraph(g);
					vvd.getLayout().setLocation(clusterGraph, cp);	    
					//vvd.getLayout().resetGraphPosition(g);	            	
					vvd.getVv().getPickedVertexState().clear();
					vvd.getVv().repaint();
				}
			}
		});		
	}

	@SuppressWarnings("unchecked")
	public void addActionForExpand(){
		expand.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Collection picked = new HashSet(vvd.getVv().getPickedVertexState().getPicked());
				for(Object v : picked) {
					if(v instanceof Graph) {
						Graph g = collapser.expand(vvd.getLayout().getGraph(), (Graph)v);
						vvd.getVv().getRenderContext().getParallelEdgeIndexFunction().reset();
						vvd.getLayout().setGraph(g);
						vvd.getLayout().resetGraphPosition(g);
					}
					vvd.getVv().getPickedVertexState().clear();
					vvd.getVv().repaint();
				}
			}});
	}

	public void addActionForExit(){
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
	}

	public void addActionForCompare(){
		compare.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (vvd==null){
					return;
				}
				KRunState first = null , second = null;
				Set<KRunState> picked = vvd.getSelectedVertices();
				if (picked == null || picked.size() != 2){
					showMessageOfSelectRequirement("Select two configurations to compare.");
					return;
				}
				for (KRunState krs : picked){
					if (first==null){
						first = krs;
					}
					else{ 
						second = krs;
					}
				}
				new DiffFrame(first, second, null).setVisible(true);
			}
		});
	}

	public int determineNoOfSteps(){
		if ( numberField.getText().isEmpty() ||  (Pattern.matches("[a-zA-Z]+", numberField.getText()) == true)){
			return 1;
		}
		else{
			return Integer.parseInt(numberField.getText());
		}
	}

	public void resetNoOfSteps(){
		numberField.setText("");
	}

	public void showMessageOfSelectRequirement(String method){
		JOptionPane.showMessageDialog(new JFrame("Selection needed"),"Select a vertex for the '"+method+"' method!");
	}

	public void showMessage(String message){
		JOptionPane.showMessageDialog(new JFrame("Attention"),message,"",JOptionPane.INFORMATION_MESSAGE);
	}

	public void addCommandPanelElements(){
		commandControl.add(this.step);
		commandControl.add(this.stepAll);
		//TO DO : reuse these buttons when the functionalities work
		commandControl.add(this.collapse);
		commandControl.add(this.expand);
		commandControl.add(this.numberOfSteps);
		commandControl.add(this.numberField);
		commandControl.add(this.compare);
		commandControl.add(this.exit);

	}

	public boolean findShortestPath(Object source, Object target, String rule){	
		DijkstraShortestPath<Object, Transition> shortestPath = new DijkstraShortestPath<Object, Transition>((Graph)vvd.getLayout().getGraph());
		List<Transition> path = shortestPath.getPath(source, target);
		if(path.size() < 1){
			return false;
		}
		else{
			for(Transition edge:path){
				if(edge.getLabel().equals(Transition.TransitionType.UNLABELLED) && !rule.equals("")){
					edge.setLabel(rule);
				}
			}
			return true;
		}
	}

	public void addNewGraphComponent(KRunState source, KRunState target, int depth, String rule){
		setId(target);
		vvd.addEdge(source, target, depth, rule);
		vvd.resetGraphLayout();
	}

	public void setId(KRunState vertex){
		vertex.setStateId(index);
		index++;
	}	

	public  static void displayVertexInfo(KRunState pick) {		
		Term term = pick.getResult();
		try {
			term = (Term) term.accept(new ConcretizeSyntax());
			term = (Term) term.accept(new TypeInferenceSupremumFilter());
			term = (Term) term.accept(new BestFitFilter(new GetFitnessUnitTypeCheckVisitor()));
			//as a last resort, undo concretization
			term = (Term) term.accept(new org.kframework.krun.FlattenDisambiguationFilter());
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		if (term.getClass() == Cell.class) {
			Cell generatedTop = (Cell) term;
			if (generatedTop.getLabel().equals("generatedTop")) {
				term = generatedTop.getContents();
			}
		}
		//set the color map  
		ColorVisitor cv = new ColorVisitor();
		term.accept(cv);

		UnparserFilter unparser = new UnparserFilter(true, false);
		term.accept(unparser);

		//TO-DO : create our own filter that ignores xml characters from a tag
		StringBuffer rez = new StringBuffer();
		for (String line : unparser.getResult().split("\n")){
			line= line.trim();
			if (line.startsWith("<") && line.endsWith(">"))
				rez.append(line+"\n");
			else 
				rez.append(StringEscapeUtils.escapeXml(line)+"\n");
		}
		nodeInfo.init(rez.toString());
		//nodeInfo.init(unparser.getResult());
		XMLEditorKit.collapseMemorizedTags();
	}

	public static void displayEdgeInfo(Transition pick, KRunState src, KRunState dest){
		new DiffFrame(src, dest, pick).setVisible(true);
	}

	public void redrawGraphAndResetScroll(){
		Graph newGrapg = commandProcessor.getCurrentGraph();
		Graph oldGraph = vvd.getLayout().getGraph();
		for (Object v : newGrapg.getVertices()){
			if (vvd.verifyExistingVertex(v)==null)

				oldGraph.addVertex(v);
		}
		for (Object e : newGrapg.getEdges()){
			try {
				Object source , dest ; 
				source = newGrapg.getSource(e) ;
				dest = newGrapg.getDest(e);
				if (vvd.verifyCanAddEdge(source, dest)){
					oldGraph.addEdge(e, source,dest);
				}

			}
			// should not happen
			catch (IllegalArgumentException iae){}
		}
		vvd.resetGraphLayout();
		gzsp.revalidate();
		gzsp.repaint();
		//		int x = gzsp.getX();
		//		int y = gzsp.getY();
		//		MouseWheelEvent event = new MouseWheelEvent((Component)gzsp, MouseWheelEvent.MOUSE_WHEEL, System.currentTimeMillis(),Event.SCROLL_BEGIN,x , y, 1, false, MouseWheelEvent.WHEEL_UNIT_SCROLL , 1, 1);
		//		gzsp.dispatchEvent(event);
	}

}
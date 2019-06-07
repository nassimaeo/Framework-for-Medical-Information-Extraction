package reasoning;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;


/**
 * This class is used to draw the resulted triplets as a graph.
 * 
 * @author Nassim
 *
 */

public class GraphModel extends JFrame {
	
	private static int FRAME = 100;
	private static int TRIPLET_DIAMETER = 60;
	private static int INSTANCE_DIAMETER = 30;
	private static int MAX_DISTANCE_CONCEPTS_INSTANCES = 150;
	
	// I don't know what is this!
	private static final long serialVersionUID = 1L;
	
	
	mxGraph graph;
	mxGraphComponent graphComponent;
	List<MatchedPattern> matchedPatterns;
	
	public GraphModel(List<MatchedPattern> matchedPatterns) {
		super("Graph representation of the Model [Press SPACE BAR to refresh]");
		this.matchedPatterns = matchedPatterns;
		this.initGUI();
		this.addRedrawKeyListener();
	}

	private void addRedrawKeyListener(){
		this.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) initGUI();
			}
			@Override
			public void keyReleased(KeyEvent e) {}
			@Override
			public void keyTyped(KeyEvent e) {}
		});		
	}
	
	private void initGUI() {
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//set the window size (full screen)
		this.setSize(this.getScreenDimension());
		this.setLocation(0, 0);
		setLocationRelativeTo(null);
		this.drawGraph();
		this.setVisible(true);
	}
	
	
	private void drawGraph() {
		// clear old elements 
		this.getContentPane().removeAll();
		
		// Initializing the graphs of JGraph
		graph = new mxGraph();
		graphComponent = new mxGraphComponent(graph);
		graphComponent.setPreferredSize(this.getScreenDimension());
		graphComponent.setConnectable(false);
		graph.setCellsDisconnectable(false);
		

		
		// graph style
		Map<String, Object> edjesStyle = graph.getStylesheet().getDefaultEdgeStyle();
		edjesStyle.put(mxConstants.STYLE_OPACITY, 50);
		edjesStyle.put(mxConstants.STYLE_ROUNDED, true);
		//instanceOfEdjeStyle.put(mxConstants.STYLE_EDGE, mxEdgeStyle.EntityRelation); // rectangular edges
		
		Map<String, Object> vertexesStyle = graph.getStylesheet().getDefaultVertexStyle();
		vertexesStyle.put(mxConstants.STYLE_OPACITY, 50);
		vertexesStyle.put(mxConstants.STYLE_ROUNDED, true);
		//instanceOfEdjeStyle.put(mxConstants.STYLE_EDGE, mxEdgeStyle.EntityRelation); // rectangular edges
		
		// add the JGraph in the window
		this.getContentPane().add(graphComponent);

		// drawing the graph
		graph.getModel().beginUpdate();
		Object parent = graph.getDefaultParent();
		HashMap<String, Object> elements = new HashMap<>();
		
		for (MatchedPattern matchedPattern : matchedPatterns) {

			// MetaModel Triplets
			String object = matchedPattern.getSyntacticalPattern().getObject();
			String predicate = matchedPattern.getSyntacticalPattern().getPredicate() + "_REL.";
			String subject = matchedPattern.getSyntacticalPattern().getSubject();
			
			Object vertexObject = null;
			if (!elements.containsKey(object)) {
				vertexObject = graph.insertVertex(parent, null, object, x(), y(), TRIPLET_DIAMETER,TRIPLET_DIAMETER, "strokeColor=blue;fillColor=red");
				elements.put(object, vertexObject);
			} else {
				vertexObject = elements.get(object);
			}

			Object vertexSubject = null;
			if (!elements.containsKey(subject)) {
				vertexSubject = graph.insertVertex(parent, null, subject, x(), y(), TRIPLET_DIAMETER,TRIPLET_DIAMETER, "strokeColor=blue;fillColor=red");
				elements.put(subject, vertexSubject);
			} else {
				vertexSubject = elements.get(subject);
			}
			

			// avoid printing a predicate many times
			Object edjePredicate = null;
			if (!elements.containsKey(predicate)) {
				edjePredicate = graph.insertEdge(parent, null, predicate, vertexSubject, vertexObject);
				elements.put(predicate, edjePredicate);
			} else {
				edjePredicate = elements.get(predicate); //useless
			}


			
			
			// Instances Triplets
			for (Triplet triplet : matchedPattern.getTriplets()) {
				
				String[] subjects = triplet.getSubject();
				String[] objects = triplet.getObject();
				String[] predicates = triplet.getPredicate();

				if (vertexSubject == null || vertexObject == null) throw new NullPointerException();
				
				//position instances not far from their meta model concept
				mxGeometry geo; double x, y;
				geo = graph.getCellGeometry(vertexSubject);
				x = getCloserX(geo.getCenterX());
				y = getCloserY(geo.getCenterY());
				Object S = graph.insertVertex(parent, null, String.join(",", subjects),x, y, INSTANCE_DIAMETER, INSTANCE_DIAMETER, "strokeColor=red;fillColor=yellow");
				

				
				geo = graph.getCellGeometry(vertexObject);
				x = getCloserX(geo.getCenterX());
				y = getCloserY(geo.getCenterY());
				Object O = graph.insertVertex(parent, null, String.join(",", objects),x, y, INSTANCE_DIAMETER, INSTANCE_DIAMETER, "strokeColor=red;fillColor=yellow");


				
				if (predicates != null) 
					graph.insertEdge(parent, null, String.join(",", predicates), S, O);
				else
					graph.insertEdge(parent, null, "null", S, O);

				
				graph.insertEdge(parent, null, null, S, vertexSubject, "strokeColor=gray;endArrow=open");//"instanceOf"
				graph.insertEdge(parent, null, null, O, vertexObject, "strokeColor=gray;endArrow=open" ); //"instanceOf"
			}
		}
		
		graph.getModel().endUpdate();
		
		
		// update the window to accommodate the graph
		this.update(getGraphics());
		
	}
	

	public double getCloserX(double anchor){
		return getCloser(anchor, x());
	}
	
	public double getCloserY(double anchor){
		//return anchor + 100;
		return getCloser(anchor, y());
	}

	private double getCloser(double anchor, double x){
		double result = (anchor + x) / 2;
		while (Math.abs(anchor - result) > MAX_DISTANCE_CONCEPTS_INSTANCES){
			result = (anchor + result) / 2;
		}
		return result;
	}
	
	public Dimension getScreenDimension(){
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//double width = screenSize.getWidth();
		//double height = screenSize.getHeight();
		return screenSize;
	}
	
	public double x(){
		return Math.random() * (getScreenDimension().getWidth()-FRAME);
	}
	
	public double y(){
		return Math.random() * (getScreenDimension().getHeight()-FRAME);
	}
}

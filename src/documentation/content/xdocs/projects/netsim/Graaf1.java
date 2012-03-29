/*
  First JGraph example 
  adapted by Leo Rutten
 
 $Revision: 2355 $
 $Date: 2009-08-12 12:15:37 +0200 (Wed, 12 Aug 2009) $
 $Author: lrutten $ 
  
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.UndoableEditEvent;

import org.jgraph.JGraph;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.BasicMarqueeHandler;
import org.jgraph.graph.CellMapper;
import org.jgraph.graph.CellView;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.Edge;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.GraphUndoManager;
import org.jgraph.graph.ParentMap;
import org.jgraph.graph.Port;
import org.jgraph.graph.PortView;

public class Graaf1 extends JPanel
{
	// JGraph instance
	protected JGraph graph;



	// Main Method
	public static void main(String[] args)
   {
      try
      {
		// Construct Frame
		JFrame frame = new JFrame("GraphEd");
		// Set Close Operation to Exit
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Add an Editor Panel
		frame.getContentPane().add(new Graaf1());

		frame.setSize(520, 390);
		// Show Frame
		frame.show();
      }
      catch (Exception e)
      {
         System.out.println("Exception " + e);
      }
      
	}

	//
	// Editor Panel
	//

	// Construct an Editor Panel
	public Graaf1()
   {
		// Use Border Layout
		setLayout(new BorderLayout());
		// Construct the Graph
		graph = new MyGraph(new MyModel());

		// Construct Panel
		//
		// Add the Graph as Center Component
		add(new JScrollPane(graph), BorderLayout.CENTER);

      
      DefaultGraphCell c1 = insert(new Point(50,50));
      DefaultGraphCell c2 = insert(new Point(150,50));
      DefaultGraphCell c3 = insert(new Point(250,250));
      
  	  DefaultPort ob1 = (DefaultPort) c1.getFirstChild();
	  DefaultPort ob2 = (DefaultPort) c2.getLastChild();
      DefaultPort ob3 = (DefaultPort) c3.getLastChild();
  	  System.out.println("c1 first" + ob1 );
 	  System.out.println("c2 first" + ob2 );
 	  System.out.println("c3 first" + ob3 );
 	  
 	  connect(ob1, ob2);
	  connect(ob2, ob3);
 	  connect(ob3, ob1);
	  
 	  
      System.out.println("c1 depth" + c1.getDepth() );
      
      System.out.println("getCellCount() " + getCellCount(graph));

      //System.out.println("p1 " + p1);
      //System.out.println("Graaf1() end " + this);
	}

	// Insert a new Vertex at point
	public DefaultGraphCell insert(Point point)
   {
		// Construct Vertex with no Label
		DefaultGraphCell vertex = new DefaultGraphCell();
		// Add one Floating Port
      
		DefaultPort p = new DefaultPort();
       System.out.println("p "  + p);
      
		vertex.add(p);
      
		// Snap the Point to the Grid
		point = graph.snap(new Point(point));
		// Default Size for the new Vertex
		Dimension size = new Dimension(25, 25);
		// Create a Map that holds the attributes for the Vertex
		Map map = GraphConstants.createMap();
		// Add a Bounds Attribute to the Map
		GraphConstants.setBounds(map, new Rectangle(point, size));
		// Add a Border Color Attribute to the Map
		GraphConstants.setBorderColor(map, Color.black);
		// Add a White Background
		GraphConstants.setBackground(map, Color.white);
		// Make Vertex Opaque
		GraphConstants.setOpaque(map, true);
		// Construct a Map from cells to Maps (for insert)
		Hashtable attributes = new Hashtable();
		// Associate the Vertex with its Attributes
		attributes.put(vertex, map);
		// Insert the Vertex and its Attributes (can also use model)
		graph.getGraphLayoutCache().insert(
			new Object[]
         {
           vertex
         },
			attributes,
			null,
			null,
			null);
         
      return vertex;
	}

	// Insert a new Edge between source and target
	public void connect(Port source, Port target)
   {
		// Connections that will be inserted into the Model
		ConnectionSet cs = new ConnectionSet();
		// Construct Edge with no label
		DefaultEdge edge = new DefaultEdge();
		// Create Connection between source and target using edge
		cs.connect(edge, source, target);
		// Create a Map thath holds the attributes for the edge
		Map map = GraphConstants.createMap();
		// Add a Line End Attribute
		GraphConstants.setLineEnd(map, GraphConstants.ARROW_SIMPLE);
		// Construct a Map from cells to Maps (for insert)
		Hashtable attributes = new Hashtable();
		// Associate the Edge with its Attributes
		attributes.put(edge, map);
		// Insert the Edge and its Attributes
		graph.getGraphLayoutCache().insert(
			new Object[]
         {
           edge
         },
			attributes,
			cs,
			null,
			null);
	}


	// Returns the total number of cells in a graph
	protected int getCellCount(JGraph graph)
   {
		Object[] cells = graph.getDescendants(graph.getRoots());

	   System.out.println("List of cells: ");
      for (int i=0; i<cells.length; i++)
      {
         System.out.println("  " + cells[i]);
      }
		return cells.length;
	}


	//
	// Custom Graph
	//

	// Defines a Graph that uses the Shift-Button (Instead of the Right
	// Mouse Button, which is Default) to add/remove point to/from an edge.
	public class MyGraph extends JGraph
   {
		// Construct the Graph using the Model as its Data Source
		public MyGraph(GraphModel model)
      {
			super(model);

			// Tell the Graph to Select new Cells upon Insertion
			setSelectNewCells(true);
			// Make Ports Visible by Default
			setPortsVisible(true);
			// Use the Grid (but don't make it Visible)
			setGridEnabled(true);
			// Set the Grid Size to 10 Pixel
			setGridSize(6);
			// Set the Tolerance to 2 Pixel
			setTolerance(2);
		}

		// Override Superclass Method to Return Custom EdgeView
		protected EdgeView createEdgeView(Edge e, CellMapper cm)
      {
			// Return Custom EdgeView
			return new EdgeView(e, this, cm)
         {
				// Override Superclass Method
				public boolean isAddPointEvent(MouseEvent event)
            {
					// Points are Added using Shift-Click
					return event.isShiftDown();
				}
				// Override Superclass Method
				public boolean isRemovePointEvent(MouseEvent event)
            {
					// Points are Removed using Shift-Click
					return event.isShiftDown();
				}
			};
		}
	}

	//
	// Custom Model
	//

	// A Custom Model that does not allow Self-References
	public class MyModel extends DefaultGraphModel
   {
		// Override Superclass Method
		public boolean acceptsSource(Object edge, Object port)
      {
			// Source only Valid if not Equal Target
			return (((Edge) edge).getTarget() != port);
		}
		// Override Superclass Method
		public boolean acceptsTarget(Object edge, Object port)
      {
			// Target only Valid if not Equal Source
			return (((Edge) edge).getSource() != port);
		}
	}
}


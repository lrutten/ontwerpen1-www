
/*
  J2SDK GraphLayout example adapted by Leo Rutten
  
 $Revision: 2355 $
 $Date: 2009-08-12 12:15:37 +0200 (Wed, 12 Aug 2009) $
 $Author: lrutten $ 
  
 */

import java.util.*;
import java.awt.*;
import java.applet.Applet;
import java.awt.event.*;
import javax.swing.*;


class Node
{
    double x;
    double y;

    double dx;
    double dy;

    boolean fixed;

    String lbl;
}


class Edge
{
    int from;
    int to;

    double len;
}


class GraphPanel extends Panel
    implements Runnable, MouseListener, MouseMotionListener
{    
   GraphSpring graph;
   int nnodes;
   Node nodes[] = new Node[100];

   int nedges;
   Edge edges[] = new Edge[200];

   Thread relaxer;
   boolean stress;
   boolean random;
   boolean isfixed = false;
   
   GraphPanel(GraphSpring graph)
   {
      this.graph = graph;
      addMouseListener(this);
   }

   int findNode(String lbl)
   {
      for (int i = 0 ; i < nnodes ; i++)
      {
         if (nodes[i].lbl.equals(lbl))
         {
            return i;
         }
      }
      return addNode(lbl);
   }
   
   int addNode(String lbl)
   {
      Node n = new Node();
      n.x = 10 + 380*Math.random();
      n.y = 10 + 380*Math.random();
      n.lbl = lbl;
      nodes[nnodes] = n;
      return nnodes++;
   }
   
   void addEdge(String from, String to, int len)
   {
      Edge e = new Edge();
      e.from = findNode(from);
      e.to = findNode(to);
      e.len = len;
      edges[nedges++] = e;
   }

   public void run()
   {
      System.out.println("run()");

      Thread me = Thread.currentThread();
      while (relaxer == me)
      {
         relax();
         if (random && (Math.random() < 0.03))
         {
            Node n = nodes[(int)(Math.random() * nnodes)];
            if (!n.fixed)
            {
               n.x += 100*Math.random() - 50;
               n.y += 100*Math.random() - 50;
            }
         }
         try
         {
            Thread.sleep(10);
         }
         catch (InterruptedException e) 
         {
            break;
         }
      }
   }

   synchronized void relax()
   {
      System.out.println("relax()");

      /* 
         Center the fixed node once
       */
      
      for (int i = 0 ; i < nnodes ; i++)
      {
         if (!isfixed)
         {
            Dimension d = getSize();
            System.out.println("Dim " + i + " " + " " + nnodes + " " +d);
            
            if (
               nodes[i].fixed 
               && d!=null && (d.width != 0 && d.height != 0))
            {
               isfixed = true;
               
               System.out.println("dimension " + d);
   
               d = getSize();
               nodes[i].x = d.width / 2;
               nodes[i].y = d.height / 2;
            }
         }
      }

         
      for (int i = 0 ; i < nedges ; i++)
      {
         Edge e = edges[i];
         double vx = nodes[e.to].x - nodes[e.from].x;
         double vy = nodes[e.to].y - nodes[e.from].y;
         double len = Math.sqrt(vx * vx + vy * vy);
            len = (len == 0) ? .0001 : len;
         double f = (edges[i].len - len) / (len * 3);
         double dx = f * vx;
         double dy = f * vy;

         nodes[e.to].dx += dx;
         nodes[e.to].dy += dy;
         nodes[e.from].dx += -dx;
         nodes[e.from].dy += -dy;
      }

      for (int i = 0 ; i < nnodes ; i++)
      {
         Node n1 = nodes[i];
         double dx = 0;
         double dy = 0;

         for (int j = 0 ; j < nnodes ; j++)
         {
            if (i == j)
            {
               continue;
            }
            Node n2 = nodes[j];
            double vx = n1.x - n2.x;
            double vy = n1.y - n2.y;
            double len = vx * vx + vy * vy;
            if (len == 0)
            {
               dx += Math.random();
               dy += Math.random();
            }
            else if (len < 100*100)
            {
               dx += vx / len;
               dy += vy / len;
            }
         }
         double dlen = dx * dx + dy * dy;
         if (dlen > 0)
         {
            dlen = Math.sqrt(dlen) / 2;
            n1.dx += dx / dlen;
            n1.dy += dy / dlen;
         }
      }

      Dimension d = getSize();
      System.out.println("relax dimension " + d);
      for (int i = 0 ; i < nnodes ; i++)
      {
         Node n = nodes[i];
         if (!n.fixed)
         {
            n.x += Math.max(-5, Math.min(5, n.dx));
            n.y += Math.max(-5, Math.min(5, n.dy));
         }
         if (n.x < 0)
         {
            n.x = 0;
         } 
         else if (n.x > d.width)
         {
            n.x = d.width;
         }
         if (n.y < 0)
         {
            n.y = 0;
         }
         else if (n.y > d.height)
         {
            n.y = d.height;
         }
	      n.dx /= 2;
	      n.dy /= 2;
      }
      repaint();
   }

   Node pick;
   boolean pickfixed;
   Image offscreen;
   Dimension offscreensize;
   Graphics offgraphics;

   final Color fixedColor = Color.red;
   final Color selectColor = Color.pink;
   final Color edgeColor = Color.black;
   final Color nodeColor = new Color(250, 220, 100);
   final Color stressColor = Color.darkGray;
   final Color arcColor1 = Color.black;
   final Color arcColor2 = Color.pink;
   final Color arcColor3 = Color.red;

   public void paintNode(Graphics g, Node n, FontMetrics fm)
   {
      int x = (int)n.x;
      int y = (int)n.y;
      g.setColor((n == pick) ? selectColor : (n.fixed ? fixedColor : nodeColor));
      int w = fm.stringWidth(n.lbl) + 10;
      int h = fm.getHeight() + 4;
      g.fillRect(x - w/2, y - h / 2, w, h);
      g.setColor(Color.black);
      g.drawRect(x - w/2, y - h / 2, w-1, h-1);
      g.drawString(n.lbl, x - (w-10)/2, (y - (h-4)/2) + fm.getAscent());
   }

   public synchronized void update(Graphics g)
   {
      Dimension d = getSize();
      if ((offscreen == null) || (d.width != offscreensize.width) || (d.height != offscreensize.height))
      {
         offscreen = createImage(d.width, d.height);
         offscreensize = d;
         if (offgraphics != null)
         {
            offgraphics.dispose();
         }
         offgraphics = offscreen.getGraphics();
         offgraphics.setFont(getFont());
      }

      offgraphics.setColor(getBackground());
      offgraphics.fillRect(0, 0, d.width, d.height);
      for (int i = 0 ; i < nedges ; i++)
      {
         Edge e = edges[i];
         int x1 = (int)nodes[e.from].x;
         int y1 = (int)nodes[e.from].y;
         int x2 = (int)nodes[e.to].x;
         int y2 = (int)nodes[e.to].y;
         int len = (int)Math.abs(Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)) - e.len);
         offgraphics.setColor((len < 10) ? arcColor1 : (len < 20 ? arcColor2 : arcColor3)) ;
         offgraphics.drawLine(x1, y1, x2, y2);
         if (stress)
         {
            String lbl = String.valueOf(len);
            offgraphics.setColor(stressColor);
            offgraphics.drawString(lbl, x1 + (x2-x1)/2, y1 + (y2-y1)/2);
            offgraphics.setColor(edgeColor);
         }
      }

      FontMetrics fm = offgraphics.getFontMetrics();
      for (int i = 0 ; i < nnodes ; i++)
      {
         paintNode(offgraphics, nodes[i], fm);
      }
      g.drawImage(offscreen, 0, 0, null);
   }

    //1.1 event handling
   public void mouseClicked(MouseEvent e)
   {
   }

   public void mousePressed(MouseEvent e)
   {
      addMouseMotionListener(this);
      double bestdist = Double.MAX_VALUE;
      int x = e.getX();
      int y = e.getY();
      for (int i = 0 ; i < nnodes ; i++)
      {
         Node n = nodes[i];
         double dist = (n.x - x) * (n.x - x) + (n.y - y) * (n.y - y);
         if (dist < bestdist)
         {
            pick = n;
            bestdist = dist;
         }
      }
      pickfixed = pick.fixed;
      pick.fixed = true;
      pick.x = x;
      pick.y = y;
      repaint();
      e.consume();
   }

   public void mouseReleased(MouseEvent e)
   {
      removeMouseMotionListener(this);
      if (pick != null)
      {
         pick.x = e.getX();
         pick.y = e.getY();
         pick.fixed = pickfixed;
         pick = null;
      }
      repaint();
      e.consume();
   }

   public void mouseEntered(MouseEvent e)
   {
   }

   public void mouseExited(MouseEvent e)
   {
   }

   public void mouseDragged(MouseEvent e)
   {
      pick.x = e.getX();
      pick.y = e.getY();
      repaint();
      e.consume();
   }

   public void mouseMoved(MouseEvent e)
   {
   }

   public void start()
   {
      relaxer = new Thread(this);
      relaxer.start();
   }

   public void stop()
   {
      relaxer = null;
   }
}


public class GraphSpring extends JPanel implements ActionListener, ItemListener
{
	static String edges1 = "joe-food,joe-dog,joe-tea,joe-cat,joe-table,table-plate/50,plate-food/30,food-mouse/100,food-dog/100,mouse-cat/150,table-cup/30,cup-tea/30,dog-cat/80,cup-spoon/50,plate-fork,dog-flea1,dog-flea2,flea1-flea2/20,plate-knife";
	static String center1 = "joe";

	static String edges2 = "zero-one,one-two,two-three,three-four,four-five,five-six,six-seven,seven-zero";
	static String center2 = null;
   
	static String edges3 = "zero-one,zero-two,zero-three,zero-four,zero-five,zero-six,zero-seven,zero-eight,zero-nine,one-ten,two-twenty,three-thirty,four-fourty,five-fifty,six-sixty,seven-seventy,eight-eighty,nine-ninety,ten-twenty/80,twenty-thirty/80,thirty-fourty/80,fourty-fifty/80,fifty-sixty/80,sixty-seventy/80,seventy-eighty/80,eighty-ninety/80,ninety-ten/80,one-two/30,two-three/30,three-four/30,four-five/30,five-six/30,six-seven/30,seven-eight/30,eight-nine/30,nine-one/30";
	static String center3 = null;
   
	static String edges4 = "a1-a2,a2-a3,a3-a4,a4-a5,a5-a6,b1-b2,b2-b3,b3-b4,b4-b5,b5-b6,c1-c2,c2-c3,c3-c4,c4-c5,c5-c6,x-a1,x-b1,x-c1,x-a6,x-b6,x-c6";
	static String center4 = null;
   
   GraphPanel panel;
   JPanel controlPanel;

   Button scramble = new Button("Scramble");
   Button shake    = new Button("Shake");
   Checkbox stress = new Checkbox("Stress");
   Checkbox random = new Checkbox("Random");

   GraphSpring()
   {
      init();
      start();
   }
   
   public void init()
   {
      System.out.println("init()");
      
      setLayout(new BorderLayout());

      panel = new GraphPanel(this);
      add("Center", panel);
      controlPanel = new JPanel();
      add("South", controlPanel);

      controlPanel.add(scramble); scramble.addActionListener(this);
      controlPanel.add(shake);       shake.addActionListener(this);
      controlPanel.add(stress);     stress.addItemListener(this);
      controlPanel.add(random);     random.addItemListener(this);

      String edges = edges1;
      String center = center1;



      for (StringTokenizer t = new StringTokenizer(edges, ",") ; t.hasMoreTokens() ; )
      {
         String str = t.nextToken();
         int i = str.indexOf('-');
         if (i > 0)
         {
            int len = 50;
            int j = str.indexOf('/');
            if (j > 0)
            {
               len = Integer.valueOf(str.substring(j+1)).intValue();
               str = str.substring(0, j);
            }
            panel.addEdge(str.substring(0,i), str.substring(i+1), len);
         }
      }

      /* at this point the centre node will be put in the
         centre. Using Panel within Swing provokes an error
         GetSize() returns 0,0 
       */
      
      Dimension d = getSize();
      System.out.println("dimension " + d);
   	if (center != null)
      {
         Node n = panel.nodes[panel.findNode(center)];
         n.x = d.width / 2;
         n.y = d.height / 2;
         n.fixed = true;
      }
    }

    public void destroy()
    {
        remove(panel);
        remove(controlPanel);
    }

    public void start()
    {
	    panel.start();
    }

    public void stop()
    {
	    panel.stop();
    }

    public void actionPerformed(ActionEvent e)
    {
	 Object src = e.getSource();

    if (src == scramble)
    {
 	    Dimension d = getSize();
	    for (int i = 0 ; i < panel.nnodes ; i++)
       {
		    Node n = panel.nodes[i];
		    if (!n.fixed)
          {
		       n.x = 10 + (d.width-20)*Math.random();
		       n.y = 10 + (d.height-20)*Math.random();
		    }
	    }
	    return;
	}

	if (src == shake)
   {
	    Dimension d = getSize();
	    for (int i = 0 ; i < panel.nnodes ; i++)
       {
		    Node n = panel.nodes[i];
		    if (!n.fixed)
          {
		       n.x += 80*Math.random() - 40;
		       n.y += 80*Math.random() - 40;
		    }
	    }
	}

    }

    public void itemStateChanged(ItemEvent e) 
    {
	    Object src = e.getSource();
	    boolean on = e.getStateChange() == ItemEvent.SELECTED;
	    if (src == stress) 
          panel.stress = on;
	    else if (src == random)
          panel.random = on;
    }

    public String getAppletInfo()
    {
	    return "Title: GraphLayout \nAuthor: <unknown>";
    }

    public String[][] getParameterInfo()
    {
	    String[][] info = 
       {
	       {
             "edges", "delimited string", "A comma-delimited list of all the edges.  It takes the form of 'C-N1,C-N2,C-N3,C-NX,N1-N2/M12,N2-N3/M23,N3-NX/M3X,...' where C is the name of center node (see 'center' parameter) and NX is a node attached to the center node.  For the edges connecting nodes to eachother (and not to the center node) you may (optionally) specify a length MXY separated from the edge name by a forward slash."
          },
	       {
             "center", "string", "The name of the center node."
          }
	    };
	    return info;
    }

    public static void main(String args[])
    {
		JFrame frame = new JFrame("GraphSpring");
      
		// Set Close Operation to Exit
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Add an Editor Panel
		frame.getContentPane().add(new GraphSpring());

		frame.setSize(520, 390);
		// Show Frame
		frame.show();
       
    }
}

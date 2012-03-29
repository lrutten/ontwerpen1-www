/*
    13/ 2/2004 L. Rutten
        SSF simulation example created
        WaitRoom added
 */

package be.khlim.iwt.inf;

import com.renesys.raceway.SSF.*;


class WaitRoom
{
   int counter;
   boolean stopped;
   boolean end;
   
   public WaitRoom()
   {
      counter = 0;
      stopped = false;
      end     = false;
   }

   public synchronized void markend()
   {
      System.out.println("thread " + Thread.currentThread() + "  markend() ");
      end = true;
      notifyAll();
   }
   
   public synchronized void stop()
   {
      if (!stopped)
      {
         stopped = true;
         counter++;
         System.out.println("thread " + Thread.currentThread() + "  stop() " + counter);
         
         notifyAll();
      }
      
      while (stopped)
      {
         try
         {
            wait();
         }
         catch (InterruptedException exc)
         {
         }
      }
   }
   
   public synchronized void resume()
   {
      if (stopped)
      {
         stopped = false;
         notifyAll();
      }
   }

   public synchronized boolean waitforstop()
   {
      if (!end)
      {
         while (!stopped && !end)
         {
            try
            {
               wait();
            }
            catch (InterruptedException exc)
            {
            }
         }
      }
      return end;
   }
}

class Host extends Entity
{
   private WaitRoom waitroom;
   public int rcvd;
   public inChannel  in;
   public outChannel out;
   
   public Host(WaitRoom wr)
   {
      waitroom = wr;
      rcvd = 0;
      in = new inChannel(this);
      out = new outChannel(this, 20);
      
      // receiver process
      new process(this)
      {
         public void action()
         {
            waitOn(in);
            Event[] eset = in.activeEvents();
            System.out.println("thread " + Thread.currentThread() + " " + owner().now() +" waitOn " + eset[0]);
            rcvd++;
         }
      };

      // sender process
      new process(this)
      {
         public void action()
         {
            out.write(new Event());
            
            
            waitFor(20);
            System.out.println("thread " + Thread.currentThread() + " " + owner().now() + " waitFor");
            rcvd++;

            waitroom.stop();
         }
      };
   }
}


public class TwoHost implements Runnable
{
      WaitRoom wr;
      Host host1;
      Host host2;
   
   public  TwoHost()
   {
      wr    = new WaitRoom();
      host1 = new Host(wr);
      host2 = new Host(wr);
      
      host1.out.mapto( host2.in);
      host2.out.mapto( host1.in);
   }
   
   
   public void run()
   {
      boolean stopnow = false;
      
      System.out.println("thread " + Thread.currentThread() + " run()");
      while (!stopnow)
      {
         stopnow = wr.waitforstop();

         try
         {
            // Wait for 1000 ms
            Thread.sleep(1000);
         }
         catch (InterruptedException exc)
         {
         }
         
         if (!stopnow)
         {
            wr.resume();
         }
      }
   }
   
   public void simulate()
   {
      Thread th = new Thread(this);
      th.start();
      
      host1.startAll(200);
      host1.joinAll();
    
      wr.markend();
      
      System.out.println("host1.rcvd " + host1.rcvd);
      System.out.println("host2.rcvd " + host2.rcvd);
      System.out.println("thread " + Thread.currentThread() + " simulate() end");
   }

   public static void main(String[] args)
   {
      TwoHost hosts = new TwoHost();
      hosts.simulate();
   }
}
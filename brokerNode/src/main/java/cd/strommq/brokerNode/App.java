package cd.strommq.brokerNode;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	NodeServer node=new NodeServer();
    	node.start();
    	try {
            System.in.read();
        } catch (IOException e) {
          
            e.printStackTrace();
        }
    }
}

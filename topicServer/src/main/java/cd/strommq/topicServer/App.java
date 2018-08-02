package cd.strommq.topicServer;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	NameServer server=new NameServer();
    	server.start();
    	try {
			System.in.read();
		} catch (IOException e) {
		
			e.printStackTrace();
		}
    }
}

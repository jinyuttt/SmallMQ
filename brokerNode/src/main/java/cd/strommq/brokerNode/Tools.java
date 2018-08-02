/**
 * 
 */
package cd.strommq.brokerNode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author jinyu
 *
 */
public class Tools {
    private static AtomicLong id=new AtomicLong(0);
	public static String getLocalIP() {
	  InetAddress addr = null;
	try {
		addr = InetAddress.getLocalHost();
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}  
      String ip=addr.getHostAddress().toString(); //获取本机ip  
      return ip;
	}
	public static long getID()
	{
	    return id.getAndIncrement();
	}
}

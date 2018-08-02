/**
 * 
 */
package cd.strommq.mqclient;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author jinyu
 *
 */
public class Tools {
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
     
}

/**
 * 
 */
package cd.strommq.topicServer;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
/**
 * @author jinyu
 *
 */
public class CommonTool {
	private static  AtomicLong  curID=new AtomicLong(0);
public static long  getID()
{
	return curID.getAndIncrement();
}

/**
 * 唯一ID
 * @return
 */
public static String getUUID()
{
	return UUID.randomUUID().toString();
}


}

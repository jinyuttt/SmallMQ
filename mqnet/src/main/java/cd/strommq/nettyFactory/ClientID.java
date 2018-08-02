/**    
 * 文件名：ClientID.java    
 *    
 * 版本信息：    
 * 日期：2018年8月1日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.nettyFactory;
import java.util.concurrent.atomic.AtomicLong; 
/**    
 *     
 * 项目名称：mqnet    
 * 类名称：ClientID    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年8月1日 下午11:25:44    
 * 修改人：jinyu    
 * 修改时间：2018年8月1日 下午11:25:44    
 * 修改备注：    
 * @version     
 *     
 */
public class ClientID {
    private static AtomicLong id=new AtomicLong(0);
public static long getID()
{
   return  id.getAndIncrement();
}
}

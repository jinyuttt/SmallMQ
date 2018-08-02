/**    
 * 文件名：ThreadManager.java    
 *    
 * 版本信息：    
 * 日期：2018年8月2日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.mqclient;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
/**    
 *     
 * 项目名称：mqclient    
 * 类名称：ThreadManager    
 * 类描述：   线程池创建
 * 创建人：jinyu    
 * 创建时间：2018年8月2日 上午12:18:10    
 * 修改人：jinyu    
 * 修改时间：2018年8月2日 上午12:18:10    
 * 修改备注：    
 * @version     
 *     
 */
public class ThreadManager {
    private static AtomicLong thradid=new AtomicLong(1);
public static ThreadFactory create(String name)
{
    return new ClientThreadFactroy(name+"_"+thradid.getAndIncrement());
}
}

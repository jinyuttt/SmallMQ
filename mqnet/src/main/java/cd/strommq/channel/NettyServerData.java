/**    
 * 文件名：NettyServerData.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.channel;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
/**    
 *     
 * 项目名称：mqnet    
 * 类名称：NettyServerData    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 上午4:27:25    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 上午4:27:25    
 * 修改备注：    
 * @version     
 *     
 */
public class NettyServerData {
  private  static ConcurrentHashMap<Integer,NettyChannel> map=new ConcurrentHashMap<Integer,NettyChannel>();
public static NettyRspClient  getData(int port)
{
    NettyChannel ch=map.getOrDefault(port, null);
    try {
        return ch.getData();
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return null;
    
}
public static NettyChannel getChannel(int port)
{
   return map.getOrDefault(port, null);
}
public static void addChannel(int port,NettyChannel channel)
{
    map.put(port, channel);
}
public static void removeClient()
{
    try
    {
    Iterator<Entry<Integer, NettyChannel>> iter = map.entrySet().iterator();
    while(iter.hasNext())
    {
        Entry<Integer, NettyChannel> item = iter.next();
        item.getValue().removeUnActive();
    }
    }
    catch(Exception ex)
    {
        ex.printStackTrace();
    }
}
}

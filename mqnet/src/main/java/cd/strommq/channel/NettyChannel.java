/**    
 * 文件名：NettyChannel.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.channel;

import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import io.netty.channel.Channel;

/**    
 *     
 * 项目名称：mqnet    
 * 类名称：NettyChannel    
 * 类描述：    服务端数据接收；隔离直接操作
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 上午10:08:18    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 上午10:08:18    
 * 修改备注：    
 * @version     
 *     
 */
public class NettyChannel {
private ConcurrentHashMap<String,Channel> group=null;
private LinkedBlockingQueue<NettyRspClient> queue=null;
private ConcurrentHashMap<String,NettyRspClient> mapRsp=null;
public NettyChannel()
{
    group=new ConcurrentHashMap<String,Channel>();
    queue=new LinkedBlockingQueue<NettyRspClient>();
    mapRsp=new ConcurrentHashMap<String,NettyRspClient>();
}

/**
 * 
 * @Title: addChannel   
 * @Description: 保持TCP连接的CHANEL  
 * @param ch      
 * void      
 * @throws
 */
public void addChannel(Channel ch)
{
    group.put(ch.id().asLongText(),ch);
    
}

/**
 * 
 * @Title: removeChenel   
 * @Description: 移除TCP连接的CHANEL   
 * @param ch      
 * void      
 * @throws
 */
public void removeChenel(Channel ch)
{
    group.remove(ch.id().asLongText());
}

/**
 * 
 * @Title: getData   
 * @Description: 获取数据及CHANEL
 * @return
 * @throws InterruptedException      
 * NettyData      
 * @throws
 */
public NettyRspClient getData() throws InterruptedException
{
    return queue.take();
}

/**
 * 
 * @Title: getData   
 * @Description: 超时获取
 * @param timeout
 * @return
 * @throws InterruptedException    
 * NettyRspData
 */
public NettyRspClient getData(long timeout) throws InterruptedException
{
    return queue.poll(timeout, TimeUnit.MILLISECONDS);
}

/**
 * 
 * @Title: add   
 * @Description: 数据添加 
 * @param data
 * @param client    
 * void
 */
public void add(Object data,NettyRspClient client)
{
    client.addData(data);
    try {
        queue.put(client);
    } catch (InterruptedException e) {
       
        e.printStackTrace();
    }
}

/**
 * 
 * @Title: getRsp   
 * @Description: 获取固有NettyRspClient
 * @param client
 * @return    
 * NettyRspClient
 */
public NettyRspClient getRsp(InetSocketAddress client)
{
    String key=client.getAddress().getHostAddress()+client.getPort();
    return mapRsp.getOrDefault(key, null);
}

/**
 * 
 * @Title: addRsp   
 * @Description: 
 * @param client    
 * void
 */
public void addRsp(NettyRspClient client)
{
    String key=client.client.getAddress().getHostAddress()+client.client.getPort();
    mapRsp.put(key, client);
}


/**
 * 
 * @Title: removeUnActive   
 * @Description: 移除无效的
 * void
 */
 void removeUnActive()
{
    Iterator<Entry<String, NettyRspClient>> iter = mapRsp.entrySet().iterator();
    while(iter.hasNext())
    {
       Entry<String, NettyRspClient> item = iter.next();
       if(item.getValue().getState()==NetState.UnActive)
       {
           iter.remove();
       }
    }
}
}

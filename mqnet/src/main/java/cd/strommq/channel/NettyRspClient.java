/**    
 * 文件名：NettyServer.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.channel;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

/**    
 *     
 * 项目名称：mqnet    
 * 类名称：NettyServer    
 * 类描述：    数据接收及回执
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 上午10:22:31    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 上午10:22:31    
 * 修改备注：    
 * @version     
 *     
 */
public class NettyRspClient {

/**
 * ID号，固有
 */
public long id;

/**
 * 接收的数据，一般是byte[]，除非有自己格式
 */
//public Object data;

/**
 * UDP通信回执地址，如果是TCP则为null
 */
public InetSocketAddress client;

/**
 * 回执发送的通道
 */
public Channel chanel;

private NetState state=NetState.Active;

private long readTime=System.currentTimeMillis();

private ConcurrentLinkedQueue<Object> datas=new ConcurrentLinkedQueue<Object>();
public Object getData()
{
    return datas.poll();
}
 void addData(Object data)
{
    datas.offer(data);
}
/**
 * 
 * @Title: setRsp   
 * @Description: 回执数据
 * @param data      
 * void      
 * @throws
 */
public void setRsp(byte[]data)
{
    ByteBuf buf=Unpooled.copiedBuffer(data);
    if(client!=null)
    {
       // System.out.println("udp回执："+client.getHostName()+","+client.getPort());
        chanel.writeAndFlush(new DatagramPacket(buf,client));
    }
    else
    {
        chanel.writeAndFlush(buf);
    }
   // ReferenceCountUtil.release(buf);
}

public NetState getState() {
  
    return state;
}

public void setState(NetState state)
{
    this.state=state;
}

public long lastReadTime()
{
    return readTime;
    
}

public void setReadTime()
{
    readTime=System.currentTimeMillis();
}
}

/**    
 * 文件名：FactorySocket.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.nettyFactory;

import cd.strommq.channel.NettyClient;
import cd.strommq.channel.NettyServer;
import cd.strommq.tcp.NettyTcpClient;
import cd.strommq.tcp.NettyTcpServer;
import cd.strommq.udp.NettyUdpClient;
import cd.strommq.udp.NettyUdpServer;

/**    
 *     
 * 项目名称：mqnet    
 * 类名称：FactorySocket    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 下午2:21:39    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 下午2:21:39    
 * 修改备注：    
 * @version     
 *     
 */
public class FactorySocket {
    
    /**
     * 
     * @Title: createServer   
     * @Description: 创建服务端对象   
     * @param net
     * @return      
     * NettyServer      
     * @throws
     */
public static NettyServer createServer(String net)
{
    if(net.compareToIgnoreCase("udp")==0)
    {
        return new NettyUdpServer();
    }
    else
    {
        return new NettyTcpServer();
    }
    
}

/**
 * 
 * @Title: createClient   
 * @Description: 创建客户端对象  
 * @param net
 * @return      
 * NettyClient      
 * @throws
 */
public static NettyClient createClient(String net)
{
    NettyClient client=null;
    if(net.compareToIgnoreCase("udp")==0)
    {
        client= new NettyUdpClient();
    }
    else
    {
        client= new NettyTcpClient();
    }
    client.setID(ClientID.getID());
    return client;
}
}

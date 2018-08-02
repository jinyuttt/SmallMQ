/**    
 * 文件名：MsgProxyBridge.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.brokerNode;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cd.strommq.channel.NettyRspClient;
import cd.strommq.channel.NettyServer;
import cd.strommq.nettyFactory.FactorySocket;

/**    
 *     
 * 项目名称：brokerNode    
 * 类名称：MsgProxyBridge    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 下午12:45:21    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 下午12:45:21    
 * 修改备注：    
 * @version     
 *     
 */
public class MsgProxyBridge {

    public String frontIP;
    public int  frontPort=0;
    public String frontNet="udp";
    public String backIP;
    public int backPort=0;
    public String backNet="udp";
    private NettyServer socket=null;
    private NettyServer backsocket=null;
    private ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);
    /**
     * 
     * @Title: node_sub   
     * @Description: 节点订阅    
     * void      
     * @throws
     */
    public void node_sub() {
        if(socket!=null)
        {
            socket.close();
            socket=null;
        }
       socket=FactorySocket.createServer(frontNet);
       Thread server=new Thread(new Runnable() {

        public void run() {
            socket.start(frontPort); 
        }
           
       });
      server.setDaemon(true);
      server.setName("server");
      if(!server.isAlive())
      {
          server.start();
      }
       
    }
    
    /**
     * 
     * @Title: node_pub   
     * @Description: 节点发布        
     * void      
     * @throws
     */
    public void node_pub()
    {
        if(backsocket!=null)
        {
            backsocket.close();
            backsocket=null;
        }
        backsocket=FactorySocket.createServer(backNet);
        Thread server=new Thread(new Runnable() {

         public void run() {
             System.out.println("订阅监视："+backPort);
             backsocket.start(backPort); 
         }
            
        });
       server.setDaemon(true);
       server.setName("backPub");
       if(!server.isAlive())
       {
           server.start();
       }
    }
    /**
     * 
     * @Title: node_sub_data   
     * @Description:多线程拉取数据  
     * @return
     * @throws Exception      
     * NettyData      
     * @throws
     */
    public NettyRspClient node_sub_data() throws Exception  {
        Callable<NettyRspClient> task=new Callable<NettyRspClient>() {
            public NettyRspClient call() throws Exception {
               return socket.recvice();
            }
        };
       Future<NettyRspClient> result = fixedThreadPool.submit(task);
       return result.get();
    }
  
  
    /**
     * 
     * @Title: node_pub_data   
     * @Description: 数据发布端获取数据   
     * @return
     * @throws Exception      
     * NettyData      
     * @throws
     */
   public NettyRspClient node_pub_data() throws Exception  {
       Callable<NettyRspClient> task=new Callable<NettyRspClient>() {
           public NettyRspClient call() throws Exception {
              return backsocket.recvice();
           }
       };
      Future<NettyRspClient> result = fixedThreadPool.submit(task);
      return result.get();
   }
  public void close()
  {
     if(socket!=null)
     {
         socket.close();
     }
     fixedThreadPool.shutdownNow();
  }
}

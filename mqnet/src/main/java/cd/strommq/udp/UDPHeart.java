/**    
 * 文件名：UDPHeart.java    
 *    
 * 版本信息：    
 * 日期：2018年8月2日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.udp;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import cd.strommq.channel.NetState;
import cd.strommq.channel.NetUilt;
import cd.strommq.channel.NettyRspClient;
import cd.strommq.channel.NettyServerData;

/**    
 *     
 * 项目名称：mqnet    
 * 类名称：UDPHeart    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年8月2日 上午7:08:33    
 * 修改人：jinyu    
 * 修改时间：2018年8月2日 上午7:08:33    
 * 修改备注：    
 * @version     
 *     
 */
public class UDPHeart {
    private static WeakHashMap<NettyUdpClient,String> map=new  WeakHashMap<NettyUdpClient,String>();
    private static ReentrantReadWriteLock obj_lock=new ReentrantReadWriteLock();
    public static  long maxCheck=30*1000;
    private static long minCheck=Long.MAX_VALUE;
    private static String heartClient="heartUDPClient";
    private static String heartServer="heartUDPServer";
    public static int heartClientLen=0;
    public static int heartServerLen=0;
    public static byte[] heartByteClient=null;
    public static byte[] heartByteServer=null;
    private static LinkedBlockingQueue<NettyUdpClient> queue=new LinkedBlockingQueue<NettyUdpClient>();
    private static WeakHashMap<NettyRspClient,String> mapServer=new  WeakHashMap<NettyRspClient,String>();
    private static ReentrantReadWriteLock server_lock=new ReentrantReadWriteLock();
    public static  long serverCheck=30*1000;
    public static  volatile boolean isInitServer=true;
    public static  volatile boolean isInitClient=true;
    private static ExecutorService fixedThreadPool=null;//用于数据分发
    /**
     * 
     * @Title: addClient   
     * @Description: 添加客户端  
     * @param client    
     * void
     */
    public static void addClient(NettyUdpClient client)
    {
      obj_lock.writeLock().lock();
      map.put(client, String.valueOf(System.currentTimeMillis()));
      if(minCheck>client.getHearTime())
      {
          minCheck=client.getHearTime();
      }
      if(minCheck>maxCheck)
      {
          minCheck=maxCheck;
      }
      obj_lock.writeLock().unlock();
      if(isInitClient)
      {
          startClient();
          isInitClient=false;
      }
   }
    
    /**
     * 
     * @Title: addRsp   
     * @Description: 设置返回端   
     * @param client    
     * void
     */
   public static void addRsp(NettyRspClient client)
   {
       server_lock.writeLock().lock();
       mapServer.put(client, String.valueOf(System.currentTimeMillis()));
       server_lock.writeLock().unlock();
       if(isInitServer)
       {
           startServer();
           isInitServer=false;
       }
   }
   
   /**
    * 
    * @Title: checkServer   
    * @Description: 通过比较获取状态   
    * @param ip
    * @param port
    * @return    
    * NetState
    */
   public static NetState checkClient(String ip,int port)
   {
       NetState state=NetState.Unknow;
       server_lock.readLock().lock();
       Iterator<Entry<NettyRspClient, String>> iter = mapServer.entrySet().iterator();
       while(iter.hasNext())
       {
            NettyRspClient client= iter.next().getKey();
            if(port==client.client.getPort()&&ip.equals(client.client.getAddress().getHostAddress()))
             {
                state=client.getState();
             }
       }
       server_lock.readLock().unlock();
    return state;
   }

/**
 * 
 * @Title: start   
 * @Description:   
 * void
 */
private synchronized  static void startClient()
{
    if(!isInitClient)
    {
        return;
    }
    isInitClient=false;
    queue=new LinkedBlockingQueue<NettyUdpClient>();
    heartByteClient=heartClient.getBytes(NetUilt.charset);
    heartClientLen=heartByteClient.length;
    Thread activeThread=new Thread(new Runnable() {
        @Override
        public void run() {
            if(minCheck>maxCheck)
            {
                minCheck=maxCheck;
            }
         while(true)
         {
            try {
                Thread.sleep(minCheck/2);
            } catch (InterruptedException e) {
                
                e.printStackTrace();
            }
            obj_lock.readLock().lock();
            Iterator<Entry<NettyUdpClient, String>> iter = map.entrySet().iterator();
            if(iter.hasNext())
            {
                NettyUdpClient client=iter.next().getKey();
                if(client.getState()!=NetState.UnActive)
                {
                  if(client.lastReadTime()+client.getHearTime()<System.currentTimeMillis())
                 {
                      //超过心跳时间
                     if(client.getState()==NetState.Active)
                     {
                       client.send(heartByteClient);
                       client.setState(NetState.Check);
                        try {
                        Thread.sleep(1000);//当前认为1s就可以接收数据
                        queue.put(client);
                       } catch (InterruptedException e) {
                      
                        e.printStackTrace();
                       }
                       
                     }
                     if(client.getState()==NetState.Check)
                     {
                         //说明经过minCheck/2时间后还没有轮到客户端发送1000包验证
                         //将状态改为最后验证
                         client.send(heartByteClient);
                         client.send(heartByteClient);
                         client.setState(NetState.CheckLast);
                     }
                     else if(client.getState()==NetState.CheckLast)
                     {
                         //经过了3次，已经超过了一个minCheck时间还是没有收到数据，发送3次数据；
                         //直接认为不活
                        // client.setState(NetState.UnActive);
                     }
                 }
                 else
                 {
                     //恢复状态
                     client.setState(NetState.Active);
                 }
                }
                
            }
            obj_lock.readLock().unlock();
         }
            
        }
        
    });
    activeThread.setDaemon(true);
    activeThread.setName("activeThread");
    if(!activeThread.isAlive())
    {
        activeThread.start();
    }
    
    //
    check();
}

/**
 * 
 * @Title: check   
 * @Description:逐个监测客户端对应的服务端后续状态     
 * void
 */
private synchronized  static void check()
{
    int fixNum=(int) (Runtime.getRuntime().availableProcessors()*2);
    fixedThreadPool = Executors.newFixedThreadPool(fixNum);
    Thread client=new Thread(new Runnable() {
        @Override
        public void run() {
         while(true)
         {
             NettyUdpClient client = null;
            try {
                client = queue.take();
            } catch (InterruptedException e) {
              
                e.printStackTrace();
            }
            checkClient(client);//每个客户端提交给多个线程处理
         }
            
        }
        
    });
    client.setDaemon(true);
    client.setName("ClientHeart");
    if(!client.isAlive())
    {
        client.start();
    }
    
}

/**
 * 
 * @Title: checkClient   
 * @Description: 监测每个客户端状态
 * @param client    
 * void
 */
private  static void checkClient(NettyUdpClient client)
{
    fixedThreadPool.execute(new Runnable() {

        @Override
        public void run() {
            if(client.getState()==NetState.Check)
            {
                 for(int i=0;i<1000;i++)
                 {
                     client.send(heartByteClient);
                 }
                 client.setState(NetState.Check1000);
                 //直接等待放回
                long waitTime=client.getHearTime()/1000;
                waitTime=waitTime>1?1:waitTime;
                int num=0;
                //等待一个heart周期
                while(true)
                {
                   try {
                 Thread.sleep(waitTime*1000);
                } catch (InterruptedException e) {
            
                   e.printStackTrace();
                 }
                   //
                   if(client.lastReadTime()+client.getHearTime()>System.currentTimeMillis())
                   {
                       client.setState(NetState.Active);
                       break;
                   }
                   num++;
                   if(num*waitTime*1000>client.getHearTime())
                   {
                       break;
                   }
                }
                if(client.lastReadTime()+client.getHearTime()<System.currentTimeMillis())
                {
                    client.setState(NetState.UnActive);
                }
                 
            }
            
        }
        
    });
}

private synchronized  static void startServer()
{
  
    if(!isInitServer)
    {
        return;
    }
    isInitServer=false;
    heartByteServer=heartServer.getBytes(NetUilt.charset);
    heartServerLen=heartByteServer.length;
    Thread activeThread=new Thread(new Runnable() {

        @Override
        public void run() {
         while(true)
         {
            try {
                Thread.sleep(serverCheck/2);
            } catch (InterruptedException e) {
                
                e.printStackTrace();
            }
            server_lock.readLock().lock();
            Iterator<Entry<NettyRspClient, String>> iter = mapServer.entrySet().iterator();
            if(iter.hasNext())
            {
                NettyRspClient client=iter.next().getKey();
                if(client.lastReadTime()+serverCheck<System.currentTimeMillis())
                {
                  if(client.getState()==NetState.Active)
                   {
                    //直接发送100包
                     for(int i=0;i<100;i++)
                     {
                       client.setRsp(heartByteServer);
                     }
                     client.setState(NetState.Check);
                  }
                  else if(client.getState()==NetState.Check)
                  {
                      client.setState(NetState.UnActive);
                  }
                }
                else
                {
                    client.setState(NetState.Active);
                }
            }
            server_lock.readLock().unlock();
            //
            NettyServerData.removeClient();
         }
            
        }
        
    });
    activeThread.setDaemon(true);
    activeThread.setName("activeServer");
    if(!activeThread.isAlive())
    {
        activeThread.start();
    }
}





}

/**    
 * 文件名：MQManager.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.mqclient;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import cd.strommq.channel.NettyClient;
import cd.strommq.log.LogFactory;
import cd.strommq.nettyFactory.FactorySocket;
import cd.strommq.nettyFactory.NetAddress;
import cd.strommq.nettyFactory.RequestInfo;


/**    
 *     
 * 项目名称：mqclient    
 * 类名称：MQManager    
 * 类描述：    管理订阅，数据发布，主题信息，broker信息
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 下午7:16:11    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 下午7:16:11    
 * 修改备注：    
 * @version     
 *     
 */
public class MQManager {
//主题的broker
private HashMap<String,List<String>> mapTopic=null;
//broker发布地址
private HashMap<String,NetAddress> mapPubBroker=null;
//broker订阅地址
private HashMap<String,NetAddress> mapSubBroker=null;
//有顺序的，每个broker的主题个数
private LinkedHashMap<String,Long> mapBroker=null;//broker主题个数
//每个nameServer地址
private List<NetAddress> lstNameServer=null;//nameServer地址
private NetAddress curNameServer=null;//当前客户端使用的nameServer
private byte[] req=null;//全局变量
private byte[] split=null;//主题与数据的分割
private volatile boolean isStop=false;
//IP+Port
private  HashMap<String,List<NettyModel>> subAddr=null;//订阅客户端
//IP+Port
private  HashMap<String,List<NettyModel>> pubAddr=null;//发布客户端
//主动抽取，如果判断无用的就不需要了，所以用WeakHashMap
private  WeakHashMap<NettyModel,String> mapPoll=null;//客户端
//主题+订阅,不用户安全，订阅少，同步方法
private  HashMap<String,List<MQSubscriber>> subObject=null;//订阅客户端
//数据分发线程池
private ExecutorService fixedThreadPool=null;//用于数据分发
//数据接收线程池
private ExecutorService cachePool=null;//用于数据接收

private LinkedBlockingQueue<PubModel> pubDataQueue=null;//发布数据
private ReentrantReadWriteLock obj_lock=new ReentrantReadWriteLock();//控制地址变化
private LinkedTransferQueue<NettyModel> clientQueue=null;//新增客户端队列
private LinkedBlockingQueue<NettyModel> recviceQueue=null;//数据接收队列
private ConcurrentLinkedQueue<SubscriberParam> failSubscriber=null;//订阅失败的信息，自动订阅
//订阅者接收数据会遍历
private ReentrantReadWriteLock subscriber_lock=new ReentrantReadWriteLock();//订阅者同步
private Semaphore semaphore=new Semaphore(1);
private static class Singleton {
    private static MQManager singleton = new MQManager();
}
public static  MQManager getInstance()
{
    return Singleton.singleton;
}
private MQManager() 
{
    try {
        semaphore.acquire();
    } catch (InterruptedException e) {
       
        e.printStackTrace();
    }//先消耗掉许可，必须请求一次数据在释放
    start();
}
/**
 * 解析地址字符串
 * @param addr
 * @return
 */
public NetAddress convertAddr(String addr)
{
    NetAddress address=new NetAddress();
    StringBuffer buf=new StringBuffer();
    if(addr.isEmpty())
    {
        return null;
    }
    buf.append(addr);
    int index=buf.indexOf(":");
    address.netType=buf.substring(0, index);
    buf.delete(0, index+3);
    index=buf.indexOf(":");
    address.ip=buf.substring(0, index);
    address.port=Integer.valueOf(buf.substring(index+1));
    return address;
}
/**
 * 解析地址字符串
 * @param addr
 * @return
 */
public String convertAddr(String ip,int port,String net)
{
    StringBuffer buf=new StringBuffer();
    buf.append(net);
    buf.append("://");
    buf.append(ip);
    buf.append(":");
    buf.append(port);
    return buf.toString().toLowerCase();
}

/**
 * 
 * @Title: start   
 * @Description:开始处理       
 * void      
 */
public void start()
{
    //
    mapTopic=new HashMap<String,List<String>>();//主题信息 topic+broker
    mapPubBroker=new HashMap<String,NetAddress>();//broker发布地址
    mapSubBroker=new HashMap<String,NetAddress>();//broker订阅地址
    mapBroker=new LinkedHashMap<String,Long>();//每个broker主题个数s
    lstNameServer=new ArrayList<NetAddress>();//topic地址
    subAddr=new HashMap<String,List<NettyModel>>();//订阅客户端，IP+Port为Key
    pubAddr=new HashMap<String,List<NettyModel>>();//发布客户端,IP+Port为Key
    pubDataQueue=new LinkedBlockingQueue<PubModel>();//客户端发布的数据
    mapPoll=new WeakHashMap<NettyModel,String> ();//所有需要抽取数据的客户端通信
    clientQueue=new LinkedTransferQueue<NettyModel>();//所有新增客户端，进行处理拦截，并且提交到接收数据的队列
    recviceQueue=new LinkedBlockingQueue<NettyModel>();//数据接收队列
    subObject=new HashMap<String,List<MQSubscriber>>();//所有订阅端订阅对象
    failSubscriber=new ConcurrentLinkedQueue<SubscriberParam>();//订阅失败的对象，会再次订阅
    //
    int fixNum=(int) (Runtime.getRuntime().availableProcessors()*1.5);
    fixedThreadPool = Executors.newFixedThreadPool(fixNum,ThreadManager.create("fixPool"));
    cachePool=Executors.newCachedThreadPool(ThreadManager.create("cachePool"));
    split=" ".getBytes(ClientUtil.charset);
    readConfig();
    if(ClientConfig.nameServer==null||ClientConfig.nameServer.isEmpty())
    {
        NetAddress tmp=new NetAddress();
        tmp.ip=Tools.getLocalIP();
        tmp.port=30000;
        tmp.netType="tcp";
        lstNameServer.add(tmp);
    }
    else
    {
        for(String addr:ClientConfig.nameServer)
        {
            NetAddress cur=convertAddr(addr);
            if(cur!=null)
            {
                lstNameServer.add(cur);
            }
        }
    }
    //
    this.startRequest();
    this.startPubThread();
    this.checkClient();
    this.startPoll();
    this.recvice();
    this.startRec();
}
public void readConfig()
{
    
}


/**
 * 
 * @Title: startRec   
 * @Description: 新客户端提交       
 * void
 */
private  void startRec()
{
   //订阅端未必有数据回来
    Thread clientThread=new Thread(new Runnable() {
        @Override
        public void run() {
            while(!isStop)
            {
               NettyModel client=null;
                try
                {
                   client = clientQueue.take();
                   //有新的客户端，交给数据接收方法接收
                   if(client!=null)
                   {
                     clientRec(client);
                   }
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    break;
                }
            }
        }
    });
    clientThread.setDaemon(true);
    clientThread.setName("clientThread");
    if(!clientThread.isAlive())
    {
        clientThread.start();
    }
  
}

/**
 * 
 * @Title: clientRec   
 * @Description:新客户端接收数据
 * @param client    
 * void
 */
private void clientRec(NettyModel client)
{
    //新增的客户端接收数据
    cachePool.execute(new Runnable(){
       @Override
       public void run()  {
           client.get().setRecviceTimeOut(5000);//设置5秒超时接收;
           //如果新客户端有数据就一直接收，没有则提交给客户端队列，由数据接收线程处理
           try {
               Thread.sleep(500);
           } catch (InterruptedException e1) {
               e1.printStackTrace();
           }
            while(!client.get().isEmpty())
            {
               //重复获取数据，占用线程
             byte[]rec=client.get().recvice();//阻塞
             if(rec!=null&&rec.length>0)
              {
               //收到收到数据
               byte[] data=new byte[rec.length-1];
               System.arraycopy(rec, 1, data, 0, data.length);
               byte flage=rec[0];//0是数据
               if(flage==1)
               {
                   //说明是注册返回
                   String rsp=new String(data);
                   rsp=rsp.replaceAll("\\s", " ");
                   System.out.println("收到订阅回执："+rsp);
                   //再次确认
                   if(rsp.startsWith("regRsp"))
                   {
                       //
                       int index=0;
                       StringBuffer buf=new StringBuffer();
                       buf.append(rsp);
                       buf.delete(0, 7);
                       index=buf.indexOf(" ");
                       String topicName=buf.substring(0, index);
                       buf.delete(0, index+1);
                       index=buf.indexOf(" ");
                       String group=buf.substring(0, index);
                       buf.delete(0, index+1);
                       String id=buf.toString();
                       //
                       client.rspResult(topicName, group, id);
                   } 
                 
               }
               else
               {
                 
                   processSubscriber(data,client);
               }
           }
           else
           {
               //没有取到数据，退出循环
               break;
           }
            }
           //执行完返回,提交到数据接收队列
            System.out.println("提交给数据接收队列");
            try {
               recviceQueue.put(client);
           } catch (InterruptedException e) {
              
               e.printStackTrace();
           }
        }
       
 }   
    );
}

/**
 * 
 * @Title: recvice   
 * @Description: 接收数据       
 * void
 */
private void recvice()
{
    Thread recData=new Thread(new Runnable() {
        @Override
        public void run() {
            int num=0;
            while(!isStop)
            {
                NettyModel client=null;
                if(num%100==0)
                {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                       
                        e.printStackTrace();
                    }
                }
            try {
                 client= recviceQueue.take();
                 if(client!=null&&!client.get().isEmpty())
                 {
                     //有数据才提交到线程中取数据
                     processData(client);
                 } 
                 else
                 {
                     recviceQueue.add(client);
                     num++;
                 }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
              //还没有回来的客户端，就暂停一段时间
             if(recviceQueue.isEmpty())
              {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                   
                    e.printStackTrace();
                }
              }
            }
        }
    });
    recData.setDaemon(true);
    recData.setName("recClientData");
    if(!recData.isAlive())
    {
        recData.start();
    }
}

/**
 * 
 * @Title: processData   
 * @Description: 提交到线程中循环处理客户端接收数据  
 * @param client    
 * void
 */
private void processData(NettyModel client)
{
  
    cachePool.execute(new Runnable() {
        @Override
        public void run() {
            System.out.println("提交线程cachePool接收数据");
            while(!client.get().isEmpty())
            {
                //重复获取数据
               byte[]rec=client.get().recvice();//阻塞
               if(rec!=null&&rec.length>0)
                {
                //
                byte[] data=new byte[rec.length-1];
                System.arraycopy(rec, 1, data, 0, data.length);
                byte flage=rec[0];
                if(flage==1)
                {
                    //说明是注册返回
                    String rsp=new String(data);
                    rsp=rsp.replaceAll("\\s", " ");
                    //再次确认
                    if(rsp.startsWith("regRsp"))
                    {
                        //
                        int index=0;
                        StringBuffer buf=new StringBuffer();
                        buf.append(rsp);
                        buf.delete(0, 7);
                        index=buf.indexOf(" ");
                        String topicName=buf.substring(0, index);
                        buf.delete(0, index+1);
                        index=buf.indexOf(" ");
                        String group=buf.substring(0, index);
                        buf.delete(0, index+1);
                        String id=buf.toString();
                        //
                        client.rspResult(topicName, group, id);
                        System.out.println("收到订阅回执："+rsp);
                    } 
                    
                }
                else
                {
                    //说明是数据了
                    //解析数据
                    System.out.println("解析数据");
                    processSubscriber(data,client);
                }
        }
        else
         {
                break;
         }
            }
            //执行完成
            try {
                recviceQueue.put(client);
            } catch (InterruptedException e) {
             
                e.printStackTrace();
            }
        
        }
    });
}


/**
 * 
 * @Title: processSubscriber   
 * @Description: 处理订阅的数据
 * @param data
 * @param client    
 * void
 */
private void processSubscriber(byte[]data,NettyModel client)
{
    SubscriberModel model= Analysis(data);
    model.brokerName=client.getBroker();
    subscriber_lock.readLock().lock();
    try
    {
        List<MQSubscriber> subscriberObj = subObject.getOrDefault(model.topicName, null);
        if(subscriberObj!=null)
        {
            int size=subscriberObj.size();
            for(int i=0;i<size;i++)
            {
                MQSubscriber sub=subscriberObj.get(i);
                if(sub.isDestroy())
                {
                    subscriberObj.remove(i);
                    size--;
                    sub=null;
                    continue;
                }
                else
                {
                   sub.addData(model);
                }
            }
        }
    }
    catch(Exception ex)
    {
        
    }
    subscriber_lock.readLock().unlock();
}

/**
 * 
 * @Title: Analysis   
 * @Description: 分析数据，进入数据不能含标记
 * @param data
 * @return
 * @throws     
 * SubModel
 */
public SubscriberModel Analysis(byte[]data)
{
    SubscriberModel model=new SubscriberModel();
    String topicName="";
    byte[] topic=null;
    if(data.length>ClientConfig.topicNameMaxLen)
    {
        topic=new byte[ClientConfig.topicNameMaxLen];
    }
    else
    {
        topic=data;
    }
    //第一个标记位剔除
    System.arraycopy(data,0, topic, 0, topic.length);
    String tmp=new String(topic);
    int index=tmp.indexOf(" ");
    topicName=tmp.substring(0,index+1);//计算出来topicName,包括空格;
    int num=topicName.getBytes(ClientUtil.charset).length;//topicName长度
    byte[] buf=new byte[data.length-num];//真实数据
    System.arraycopy(data, num, buf, 0, buf.length);//拷贝
    model.topicName=topicName.trim();
    model.data=buf;
    return model;
}

/**
 * 
 * @Title: startPoll   
 * @Description: poll类型发送获取数据         
 * void      
 * @throws
 */
private void startPoll()
{
    fixedThreadPool.execute(new Runnable() {

        @Override
        public void run() {
            
            while(!isStop)
            {
              try
              {
                  Thread.sleep(1000);//1s
                 StringBuffer buf=new StringBuffer();//
                 HashMap<String,NettyModel> mapM=new HashMap<String,NettyModel>();
                 //带监测的客户端
                Iterator<Entry<NettyModel, String>> kv = mapPoll.entrySet().iterator();
                while(kv.hasNext())
                {
                   Entry<NettyModel, String> item = kv.next();
                   NettyModel clientNetty=item.getKey();
                   //发送获取数据的request
                   NettyClient client=clientNetty.get();
                   if(client!=null)
                   {
                       Map<String,String> map=clientNetty.getSubPollInfo();
                       //
                       Iterator<Entry<String, String>> iter = map.entrySet().iterator();
                       while(iter.hasNext())
                       {
                           //topic+group,id
                           Entry<String, String> itemsub = iter.next();
                          // if()
                          // String v=itemsub.getValue();
                           if(itemsub.getValue()==null)
                           {
                               //还没有返回注册信息的客户端
                               mapM.put(itemsub.getKey(), clientNetty);
                           }
                           else
                           {
                               //发送数据获取,开头是字节1
                               String v=itemsub.getValue();
                               buf.setLength(0);
                               buf.append(itemsub.getKey());//主题+组
                               buf.append(" ");
                               buf.append(v);//订阅ID
                               buf.append(" ");
                               buf.append("100");
                               byte[]reqPoll=buf.toString().getBytes(ClientUtil.charset);
                               byte[]tmp=new byte[reqPoll.length+1];
                               tmp[0]=1;
                               System.arraycopy(reqPoll, 0, tmp, 1, reqPoll.length);
                               client.send(tmp);
                           }
                       }
                   }
                   
                }
                //处理完成后监测客户端
                if(!mapM.isEmpty())
                {
                    //主题+group,client
                    Iterator<Entry<String, NettyModel>> iter = mapM.entrySet().iterator();
                    while(iter.hasNext())
                    {
                        Entry<String, NettyModel> item = iter.next();
                        String key=item.getKey();
                        NettyModel nettyClient=item.getValue();//保持客户端信息
                        if(nettyClient==null)
                        {
                            //说明已经验证到该客户端没有订阅的broker
                            continue;
                        }
                        NettyClient client = nettyClient.get();//通信客户端
                        Map<String,String> info=nettyClient.getSubInfo();//订阅信息
                        if(client!=null)
                        {
                            String v=info.getOrDefault(key, null);
                            if(v!=null&&v.startsWith("."))
                            {
                                //说明注册还没有返回
                                //取出注册时间
                                int index=v.indexOf(",");
                                String time=v.substring(1, index);
                                long reTime=Long.valueOf(time);
                                if(reTime+ClientConfig.checkClientTime<System.currentTimeMillis())
                                {
                                    //说有问题，重新注册
                                    String brokerName=v.substring(index+1);
                                    if(mapSubBroker.containsKey(brokerName))
                                    {
                                        byte[] reginfo=nettyClient.getRegInfo(key);
                                        client.send(reginfo);
                                    }
                                    else
                                    {
                                        //说明已经没有broker了，无法注册，清除客户端
                                        client.close();
                                        client=null;
                                        try
                                        {
                                           nettyClient.close();
                                        }
                                        catch(Exception ex)
                                        {
                                           
                                        }
                                        nettyClient=null;
                                        iter.remove();
                                    }
                                }
                                
                            }
                        }
                    }
                }
               }
                catch(Exception ex)
                 {
                
                 }
            }
            
        }
        
    });
    
}


/**
 * 
 * @Title: addPoll   
 * @Description: 添加抽取的客户端  
 * @param e
 * @throws     
 * void
 */
private void addPoll(NettyModel e)
{
    if(!mapPoll.containsKey(e))
    {
        mapPoll.put(e, null);
    }
}
/**
 * 
 * @Title: startRequest   
 * @Description: 开始请求信息  
 * void      
 * @throws
 */
private void startRequest()
{
    Thread req=new Thread(new Runnable() {
        @Override
        public void run() {
           
            while(!isStop)
            {
                request();
                semaphore.release();//释放信号量，让订阅执行
                //监测订阅失败的数据
                if(!failSubscriber.isEmpty())
                {
                    int num=failSubscriber.size();//先获取一次数据量;这里与性能无关，只要不是上万的
                    while(!failSubscriber.isEmpty())
                    {
                     SubscriberParam subscriber = failSubscriber.poll();
                     if(subscriber!=null)
                     {
                         if(subscriber.subscriber==null||subscriber.subscriber.isDestroy())
                         {
                             subscriber.subscriber=null;
                             continue;
                         }
                        //再次调用订阅，理论上有了这个机制，不需要用信号量等待初始化时请求主题信息了
                        //但是按照我们一般的思路，还是保留信号量，等待初始化
                        addSubscriber(subscriber.topic,subscriber.group,subscriber.isPush,subscriber.isCopy,subscriber.subscriber);
                        num--;
                     }
                     if(num<=0)
                     {
                         break;//上面的订阅还是可能失败，信息会重新回到failSubscriber,会造成死循环
                     }
                    }
                }
                
                try {
                    Thread.sleep(ClientConfig.reqTopicTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
    });
    req.setDaemon(true);
    req.setName("reqTopic");
    if(!req.isAlive())
    req.start();
}

/**
 * 
 * @Title: startPubThread   
 * @Description: 开始发布数据 
 * void      
 * @throws
 */
private void startPubThread()
{
    Thread send=new Thread(new Runnable() {
        @Override
        public void run() {
           int num=0;
            while(!isStop)
            {
              
                try {
                    PubModel data  = pubDataQueue.take();
                    if(data==null)
                    {
                        continue;
                    }
                    //把每包数据提交到不同线程去发送
                    fixedThreadPool.execute(new Runnable() {

                        @Override
                        public void run() {
                            pubData(data);
                        }
                        
                    });
                    
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                if(num++%1000==0)
                {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                }
            }
        }
        
    });
    send.setDaemon(true);
    send.setName("pubData");
    if(!send.isAlive())
        send.start();
}

/**
 * 
 * @Title: checkClient   
 * @Description: 监测连接客户端，不使用的移除    
 * void      
 * @throws
 */
private void checkClient()
{
    Thread checkThread=new Thread(new Runnable() {

        @Override
        public void run() {
           while(!isStop)
           {
               int size=0;
               try {
                Thread.sleep(60*1000);
            } catch (InterruptedException e) {
              
                e.printStackTrace();
            }
               obj_lock.writeLock().lock();
               try
               {
               if(!pubAddr.isEmpty())
               {
                   Iterator<Entry<String, List<NettyModel>>> iter = pubAddr.entrySet().iterator();
                   while(iter.hasNext())
                   {
                       Entry<String, List<NettyModel>> item = iter.next();
                       List<NettyModel> lst=item.getValue();
                       if(!lst.isEmpty())
                       {
                            size=lst.size();
                            for(int i=0;i<size;i++)
                            {
                                //发布地址5分钟没有使用的删除
                                if(lst.get(i).lastUseTime()+ClientConfig.checkClientTime<System.currentTimeMillis())
                                {
                                    NettyModel rem= lst.remove(i);
                                    rem.get().close();
                                    size--;
                                }
                            }
                       }
                       //已经没有该发布地址了；
                       if(lst.isEmpty())
                       {
                            iter.remove();
                            continue;
                       }
                      boolean isFind=false;
                      Iterator<Entry<String, NetAddress>> mapPub = mapPubBroker.entrySet().iterator();
                      if(mapPub.hasNext())
                      {
                           NetAddress cur= mapPub.next().getValue();
                           if((cur.ip+cur.port).equals(item.getKey()))
                           {
                               isFind=true;
                               break;
                           }
                      }
                      if(!isFind)
                      {
                          LogFactory.getInstance().addDebug("没有发发布地址了，删除："+item.getKey());
                          iter.remove();
                          if(!lst.isEmpty())
                          {
                               size=lst.size();
                               for(int i=0;i<size;i++)
                               {
                                   try
                                   {
                                     lst.get(i).get().close();
                                   }
                                   catch(Exception ex)
                                   {
                                       ex.printStackTrace();
                                   }
                               }
                               lst.clear();
                          }
                      }
                      
                   }
               }
               //
              if(!subAddr.isEmpty())
              {
                  boolean isFind=false;
                  Iterator<Entry<String, List<NettyModel>>> iter = subAddr.entrySet().iterator();
                  while(iter.hasNext())
                  {
                      Entry<String, List<NettyModel>> item = iter.next();
                      Iterator<Entry<String, NetAddress>> mapPub = mapSubBroker.entrySet().iterator();
                      if(mapPub.hasNext())
                      {
                           NetAddress cur= mapPub.next().getValue();
                           String key=cur.ip+cur.port;
                           if(key.equals(item.getKey()))
                           {
                               isFind=true;
                               break;
                           }
                      }
                      if(!isFind)
                      {
                          List<NettyModel> lst=item.getValue();
                          LogFactory.getInstance().addDebug("没有订阅地址了，删除："+item.getKey());
                          iter.remove();
                          if(!lst.isEmpty())
                              {
                                   size=lst.size();
                                   for(int i=0;i<size;i++)
                                   {
                                       try
                                       {
                                         lst.get(i).get().close();
                                       }
                                       catch(Exception ex)
                                       {
                                           ex.printStackTrace();
                                       }
                                   }
                                   lst.clear();
                              }
                          
                      }
                  }
              }
               }
               catch(Exception ex)
               {
                   ex.printStackTrace();
               }
              obj_lock.writeLock().unlock();
           }
        }
        
    });
    checkThread.setDaemon(true);
    checkThread.setName("checkClient");
    if(!checkThread.isAlive())
    {
        checkThread.start();
    }
}

/**
 * 
 * @Title: pubData   
 * @Description: 发布数据         
 * void      
 * @throws
 */
private void pubData(PubModel data)
{
    //与订阅不同的地方：订阅只要有该主题的每一个broker都订阅，发布是寻找一个broker发布
    List<String> lst=mapTopic.getOrDefault(data.topicName, null);
    String curBroker="";
    if(lst==null||lst.isEmpty())
    {
        //找一个broker;
        String first="";
        if(mapBroker.isEmpty())
        {
            //还没有主题信息；
            //找发布地址
            if(!mapPubBroker.isEmpty())
            {
                //获取任意发布地址
                curBroker=(String) mapPubBroker.keySet().toArray()[0];
            }
        }
        else
        {
        Iterator<Entry<String, Long>> iter = mapBroker.entrySet().iterator();
         while (iter.hasNext()) { 
           Entry<String, Long> item = iter.next();
           if(!mapPubBroker.containsKey(item.getKey()))
           {
               //信息请求同步问题
               continue;
           }
           if(first.isEmpty())
           {
               first=item.getKey();
           }
           if(item.getValue()%ClientConfig.findBroker!=0)
           {
               curBroker=item.getKey();
               break;
           }
           } 
           if(curBroker.isEmpty())
           {
            curBroker=first;//第一个
           }
        }
    }
    else
    {
        int size=lst.size();
        for(int i=0;i<size;i++)
        {
            //找到一个即可
            if(mapPubBroker.containsKey(lst.get(i)))
            {
                curBroker=lst.get(i);
                break;
            }
        }
    }
    //与订阅不同的地方：订阅只要有该主题的每一个broker都订阅，发布是寻找一个broker发布
    NetAddress addr=mapPubBroker.getOrDefault(curBroker, null);
    if(addr!=null)
    {
        List<NettyModel> lstAddr=pubAddr.getOrDefault(addr.ip+addr.port, null);
        if(lstAddr==null)
        {
            //还没有发布的通信对象
            lstAddr=new ArrayList<NettyModel>();
            subAddr.put(addr.ip+addr.port, lstAddr);
        }
        if(lstAddr.isEmpty())
        {
             NettyModel e=new NettyModel();
             lstAddr.add(e);
             lstAddr.get(0);
             NettyClient client = FactorySocket.createClient(addr.netType);
             e.set(client);
             client.connect(addr.ip, addr.port);
             client.send(data.data);
           
        }
        else
        {
            NettyModel e=lstAddr.get(0);
            NettyClient client=e.get();
            if(client==null)
            {
                 client = FactorySocket.createClient(addr.netType);
                 client.connect(addr.ip, addr.port);
                
            }
            client.send(data.data);
        }
    }
    else
    {
        //发布不成功，重新返回数据队列
        try {
            pubDataQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
      
    
}


/**
 * 
 * @Title: request   
 * @Description: 请求信息     
 * void      
 * @throws
 */
private  void request()
{
    byte[] rec=null;
    req="request".getBytes(ClientUtil.charset);
    if(lstNameServer!=null)
    {
        if(curNameServer==null)
        {
            for(NetAddress srv:lstNameServer)
            {
                NettyClient client = FactorySocket.createClient(srv.netType);
                client.connect(srv.ip, srv.port);
                client.setRecviceTimeOut(3000);
                client.send(req);
                rec=client.recvice();
                client.close();
                if(rec==null||rec.length==0)
                {
                    //视为错误
                    continue;
                }
                else
                {
                    curNameServer=srv;
                    String rsp=new String(rec);
                    System.out.println(rsp);
                }
            }
        }
        else
        {
            NettyClient client = FactorySocket.createClient(curNameServer.netType);
            client.connect(curNameServer.ip, curNameServer.port);
            client.setRecviceTimeOut(3000);
            client.send(req);
            rec=client.recvice();
            client.close();
            if(rec==null||rec.length==0)
            {
                //视为错误
                curNameServer=null;
            }
            else
            {
                
                String rsp=new String(rec);
                System.out.println(rsp);
            }
        }
    }
    //
    if(rec!=null&&rec.length>0)
    {
        //处理地址；
       
        String json=new String(rec);
        System.out.println(rec);
        obj_lock.writeLock().lock();
        try
        {
        RequestInfo info=JSONSerializable.Deserialize(json, RequestInfo.class);
        mapPubBroker=info.mapPub;
        mapSubBroker=info.mapSub;
        mapTopic=info.mapTopic;
        //
        Iterator<Entry<String, List<String>>> iter = mapTopic.entrySet().iterator();
        mapBroker.clear();
        while(iter.hasNext())
        {
            //
            Entry<String, List<String>> item = iter.next();
            for(String str:item.getValue())
            {
                Long num=mapBroker.getOrDefault(str, null);
                if(num==null)
                {
                    mapBroker.put(str, 1L);
                }
                else
                {
                    num=num+1;
                }
            }
           
        }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        obj_lock.writeLock().unlock();
    }
}

/**
 * 
 * @Title: addPub   
 * @Description:发布数据
 * @param topicName
 * @param data      
 * void      
 * @throws
 */
public void addPub(String topicName,byte[]data)
{
    PubModel pub=new PubModel();
    byte[] topic=topicName.getBytes(ClientUtil.charset);
    byte[]tmp=new byte[topic.length+data.length+split.length];
    ByteBuffer buf=ByteBuffer.wrap(tmp);
    buf.put(topic);
    buf.put(split);
    buf.put(data);
    pub.topicName=topicName;
    pub.data=tmp;
    try {
        pubDataQueue.put(pub);
    } catch (InterruptedException e) {
     
        e.printStackTrace();
    }
}

/**
 * 
 * @Title: addSubscriber   
 * @Description: 添加订阅,订阅使用同步方法没有性能问题
 * @param topic
 * @param group
 * @param isPush
 * @param isCopy      
 * void      
 * @throws
 */
public synchronized void addSubscriber(String topic,String group,boolean isPush,boolean isCopy,MQSubscriber subscriber)
{
 
    if(group==null||group.isEmpty())
    {
        group=ClientConfig.group;
    }
    try {
        semaphore.acquire();//阻塞这里
    } catch (InterruptedException e1) {
        e1.printStackTrace();
    } 
  semaphore.release();//只要阻塞完成就释放;
  StringBuffer buf=new StringBuffer();
  buf.append(topic);
  buf.append(" ");
  buf.append(group);
  buf.append(" ");
  buf.append(isPush);
  buf.append(" ");
  buf.append(isCopy);
  byte[]data=buf.toString().getBytes(ClientUtil.charset);
  byte[] reg=new byte[data.length+1];
  ByteBuffer bufs=ByteBuffer.wrap(reg);
  bufs.put((byte) 0);
  bufs.put(data);
  obj_lock.readLock().lock();
  boolean isSucess=false;//订阅主题成功
  try
  {
  List<String> lst = mapTopic.getOrDefault(topic, null);
  List<String> lstBroker=new ArrayList<String>(10);
  String curBroker="";
  if(lst==null||lst.isEmpty())
  {
      //还没有该主题注册,找到一个broker即可
      System.out.println("正在订阅");
      String first="";
      if(mapBroker.isEmpty())
      {
          //还没有一个broker上有主题信息，则查看是否有broker,
          if(!this.mapSubBroker.isEmpty())
          {
               //获取任意一个即可
              curBroker=(String) mapSubBroker.keySet().toArray()[0];
          }
      }
      else
      {
       Iterator<Entry<String, Long>> iter = mapBroker.entrySet().iterator(); 
         while (iter.hasNext()) { 
         Entry<String, Long> item = iter.next();
         if(first.isEmpty())
         {
             first=item.getKey();
         }
         if(item.getValue()%ClientConfig.findBroker!=0)
         {
             curBroker=item.getKey();
             break;
         }
      } 
      if(curBroker.isEmpty())
       {
          curBroker=first;//第一个
       }
      }
      //
      if(curBroker!=null&&!curBroker.isEmpty())
      {
         lstBroker.add(curBroker);
      }
      
  }
  else
  {
      //找到有地址的broker
      int size=lst.size();
      for(int i=0;i<size;i++)
      {
          if(mapSubBroker.containsKey(lst.get(i)))
          {
               curBroker=lst.get(i);
               if(curBroker!=null&&!curBroker.isEmpty())
               {
                   lstBroker.add(curBroker);
               }
          }
      }
  }
  //与发布不同的地方：订阅只要有该主题的每一个broker都订阅，发布是寻找一个broker发布
  if(lstBroker.isEmpty())
  {
      //broker没有找一个
      SubscriberParam e=new SubscriberParam();
      e.group=group;
      e.isCopy=isCopy;
      e.isPush=isPush;
      e.topic=topic;
      e.subscriber=subscriber;
      this.failSubscriber.add(e);
      obj_lock.readLock().lock();//记得释放
      return;
  }
   
   int subSize=lstBroker.size();
   for(int i=0;i<subSize;i++)
    {
     //获取每个broker的订阅地址
   NetAddress addr=mapSubBroker.getOrDefault(lstBroker.get(i), null);
   if(addr!=null)
   {
       isSucess=true;
     // 主题 组名称 组是否是push 复制还是轮训
      List<NettyModel> lstAddr=subAddr.getOrDefault(addr.ip+addr.port, null);
      if(lstAddr==null)
      {
          //客户端还没有订阅过该broker,此时以IP+Port为key
          lstAddr=new ArrayList<NettyModel>();
          subAddr.put(addr.ip+addr.port, lstAddr);
      }
      if(lstAddr.isEmpty())
      {
           //创建通信客户端，订阅主题
           NettyModel e=new NettyModel();
           lstAddr.add(e);
           NettyClient client = FactorySocket.createClient(addr.netType);
           e.set(client);
           client.connect(addr.ip, addr.port);
           client.send(reg);
           clientQueue.transfer(e);//新增
           if(!isPush)
           {
               e.add(topic, group,lstBroker.get(i),data);
               this.addPoll(e);
           }
      }
      else
      {
          //否则任意选择一个通信客户端订阅，理论上现在也只有一个
          //后期会考虑不同通信协议TCP,UDP
          NettyModel e=lstAddr.get(0);
          NettyClient client=e.get();
          if(client==null)
          {
              //理论上不会出现这样的情况，开始没有线程同步
              //现在修改成了同步方法应该不会有这个分支
              //但是也不删除此分支，暂留
               client = FactorySocket.createClient(addr.netType);
               client.connect(addr.ip, addr.port);
               e.set(client);
               clientQueue.transfer(e);//新增1
              
          }
          client.send(reg);
          if(!isPush)
          {
              e.add(topic, group,lstBroker.get(i),data);
              this.addPoll(e);
          }
      }
    }
  }
   //if(
  } 
  catch(Exception ex)
  {
      ex.printStackTrace();
  }
  obj_lock.readLock().unlock();
  if(!isSucess)
  {
      //逻辑上还是失败了，订阅地址同步原因
      SubscriberParam e=new SubscriberParam();
      e.group=group;
      e.isCopy=isCopy;
      e.isPush=isPush;
      e.topic=topic;
      e.subscriber=subscriber;
      this.failSubscriber.add(e);
      return;
  }
  //
  subscriber_lock.writeLock().lock();
   List<MQSubscriber> lstSub = subObject.getOrDefault(topic, null);
   if(lstSub==null)
   {
       lstSub=new ArrayList<MQSubscriber>();
       subObject.put(topic, lstSub);
   }
   lstSub.add(subscriber);
  subscriber_lock.writeLock().unlock();
}


/**
 * 关闭，回收资源
 * @Title: close   
 * @Description: 关闭，回收资源      
 * void
 */
public void close()
{
    this.isStop=true;
    this.cachePool.shutdownNow();
    this.fixedThreadPool.shutdownNow();
    this.clientQueue.clear();
    this.lstNameServer.clear();
    this.mapBroker.clear();
    this.mapPoll.clear();
    this.mapPubBroker.clear();
    this.mapSubBroker.clear();
    this.mapTopic.clear();
    this.pubAddr.clear();
    this.pubDataQueue.clear();
    this.recviceQueue.clear();
    this.subAddr.clear();
    this.subObject.clear();
    
}
}

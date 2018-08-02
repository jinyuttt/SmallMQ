/**
 * 
 */
package cd.strommq.brokerNode;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cd.strommq.channel.NetState;
import cd.strommq.channel.NettyClient;
import cd.strommq.channel.NettyRspClient;
import cd.strommq.log.LogFactory;
import cd.strommq.nettyFactory.FactorySocket;
import cd.strommq.nettyFactory.NetAddress;


/**
 * @author jinyu
 *
 */
public class NodeServer {
	MsgProxyBridge  proxy=new MsgProxyBridge();//代理
	private boolean isStop=false;//是否停止
	private Thread recData=null;//接收发布的数据
	private List<NetAddress> nameAddress=null;//nameServer地址
	private TopicQueue writeQueue=null;//接收数据的写队列
	private ReadQueue readQueue=null;//需要数据复制的读队列
	private HashMap<String,String> mapTopic=null;//本节点所有主题
	private SubscriberQueue subscriberQueue=null;//本节点所有订阅者信息
	private CacheLocal cache=null;//内存中的缓存
	private DataDiskLocal dataSave=null; //落盘
	private HashMap<NetAddress,NettyClient> allClient=null;//本节点所有客户端对象
	private ExecutorService fixedThreadPool=null;//用于数据分发
	
	/**
	 * 有订阅组会复制
	 */
	private HashMap<String,String> mapCopy=null;
	public NodeServer()
	{
	    mapTopic=new HashMap<String,String>();
	    writeQueue=new TopicQueue(mapTopic);
	    readQueue=new ReadQueue();
	    allClient=new HashMap<NetAddress,NettyClient>();
	}
	
	/**
	 * 
	 * @Title: start   
	 * @Description: 启动服务        
	 * void      
	 * @throws
	 */
public void start()
{
    int fixNum=(int) (Runtime.getRuntime().availableProcessors()*1.5);
    fixedThreadPool = Executors.newFixedThreadPool(fixNum);
    proxy.frontIP=NodeConfig.PubIP;
	proxy.frontPort=NodeConfig.PubPort;
	proxy.backIP=NodeConfig.SubIP;
	proxy.backPort=NodeConfig.SubPort;
	//
	nameAddress=new ArrayList<NetAddress>();
	allClient=new HashMap<NetAddress,NettyClient>();
	if(NodeConfig.TopicServer==null)
	{
		NodeConfig.TopicServer=new ArrayList<String>();
		NodeConfig.TopicServer.add("tcp://*:30000");
	}
	if(NodeConfig.TopicServer!=null)
	{
		//
		List<String> lst=NodeConfig.TopicServer;
		for(String addr:lst)
		{
			nameAddress.add(convertAddr(addr));
		}
	}
	startSubscribe();//接收订阅信息
	LogFactory.getInstance().addInfo("启动数据订阅");
	startRec();//接收数据
	LogFactory.getInstance().addInfo("启动数据接收");
	startReg();//向nameServer注册
	LogFactory.getInstance().addInfo("启动节点信息注册");
	startHeart();//开启心跳
	LogFactory.getInstance().addInfo("启动节点与NameServer心跳");
	startTimeTopic();//新增主题注册及时上报
	LogFactory.getInstance().addInfo("启动节点与NameServer主题信息上报");
	startReportTopic();//定时上报主题信息
	LogFactory.getInstance().addInfo("启动节点与NameServer新增主题信息上报");
	startDistribution();//推送数据
	LogFactory.getInstance().addInfo("启动数据向订阅端推送");
	startCache();
	LogFactory.getInstance().addInfo("启动本节点缓存管理");
	System.out.println("本节点8步骤完成");
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
 * 接收发布的数据
 */
private void startRec()
{
	recData=new Thread(new Runnable() {
		public void run() {
		    proxy.node_sub();
			while(!isStop)
			{
			    NettyRspClient client;
                try {
                    client = proxy.node_sub_data();
                    writeQueue.addQueue((byte[])client.getData());
                } catch (Exception e) {
                    e.printStackTrace();
                }
				
			}
			
		}
		
	});
	recData.setDaemon(true);
	recData.setName("nodeRec");
	if(!recData.isAlive())
	{
		recData.start();
	}
}


/**
 * 向nameServer定时注册broker信息
 */
private void startReg()
{
	Thread regNode=new Thread(new Runnable() {
		public void run() {
		    List<NetAddress> lst=new ArrayList<NetAddress>();
			while(!isStop)
			{
					StringBuffer buf=new StringBuffer();
					String ip="*";
					buf.append("mqregister");
					buf.append(" ");
					buf.append(NodeConfig.LocalName);
					buf.append(" pub ");
					ip=NodeConfig.PubIP;
					if(ip.equals("*"))
					{
						ip=Tools.getLocalIP();
					}
					buf.append(convertAddr(ip,NodeConfig.PubPort,NodeConfig.PubNet));
					buf.append(" sub ");
					ip=NodeConfig.SubIP;
					if(ip.equals("*"))
					{
						ip=Tools.getLocalIP();
					}
					buf.append(convertAddr(ip,NodeConfig.SubPort,NodeConfig.SubNet));
					if(NodeConfig.heartLive!=0)
					{
						buf.append(" heart ");
						buf.append(NodeConfig.heartLive);
					}
					byte[] data=buf.toString().getBytes(NodeConfig.charset);
					byte[]rec=null;
					try
					{
					if(lst.isEmpty())
					{
					  for(NetAddress ser:nameAddress)
					  {
					     NettyClient client = allClient.getOrDefault(ser, null);
					     if(client==null)
					     {
					         if(ser.ip.equals("*"))
	                         {
	                             //主要是测试阶段
	                             ser.ip=Tools.getLocalIP();
	                         }
					         client=FactorySocket.createClient(ser.netType);
					         allClient.put(ser, client);
					         client.resetConnect();
					         client.setRecviceTimeOut(2000);
					         client.connect(ser.ip, ser.port);
					     }
					     if(data!=null)
					     {
					         client.send(data);
					     }
					     else
					     {
					         continue;
					     }
					    rec=client.recvice();
						if(rec!=null&&rec.length>0)
						{
							String result=new String(rec);
							if(!result.equals("recviced"))
							{
								NodeConfig.LocalName=result;
							}
						}
						else
						{
							//注册失败，继续注册；
							lst.add(ser);
						}
					  }
					 //第一次全部注册成功;退出
					 if(lst.isEmpty())
					 {
						 break;
					 }
					}
					else
					{
						int size=lst.size();
						for(int i=0;i<size;i++)
						{
							NetAddress ser = lst.get(i);
							NettyClient client = allClient.getOrDefault(ser, null);
	                         if(client==null)
	                         {
	                             client=FactorySocket.createClient(ser.netType);
	                             allClient.put(ser, client);
	                             client.connect(ser.ip, ser.port);
	                             client.resetConnect();
	                             client.setRecviceTimeOut(2000);
	                         }
	                        System.out.println("发送注册");
	                        client.send(data);
	                        rec=client.recvice();
							if(rec!=null&&rec.length>0)
							{
								//注册成功一个移除一个
								String result=new String(rec);
								if(!result.equals("recviced"))
								{
									NodeConfig.LocalName=result;
								}
								lst.remove(i);
								size--;
							}
							
						}
						if(lst.isEmpty())
						{
							break;
						}
					}
					}
					catch(Exception ex)
					{
					    ex.printStackTrace();
					}
					try {
	                    Thread.sleep(10000);
	                } catch (InterruptedException e) {
	                    e.printStackTrace();
	                }
				}
		}
		
	});
	regNode.setDaemon(true);
	regNode.setName("regNode");
	if(!regNode.isAlive())
	{
		regNode.start();
	}
}


/**
 * 与nameServer的心跳包
 */
private void startHeart()
{
	Thread heartNode=new Thread(new Runnable() {
		public void run() {
			while(!isStop)
			{
				//
			    System.out.println("启动心跳包，时间(s)："+NodeConfig.heartTime/1000);
			    try {
                    Thread.sleep(NodeConfig.heartTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
					StringBuffer buf=new StringBuffer();
					buf.append("heart");
					buf.append(" ");
					buf.append(NodeConfig.LocalName);
					byte[] data=buf.toString().getBytes(NodeConfig.charset);
					for(NetAddress ser:nameAddress)
					 {
					    try
					    {
						 if(ser.ip.equals("*"))
						 {
							 //主要是测试阶段
							 ser.ip=Tools.getLocalIP();
						 }
						 NettyClient client = allClient.getOrDefault(ser, null);
                         if(client==null)
                         {
                             client=FactorySocket.createClient(ser.netType);
                             allClient.put(ser, client);
                             client.connect(ser.ip, ser.port);
                             client.resetConnect();
                             client.setRecviceTimeOut(2000);
                         }
                         if(data!=null)
                        client.send(data);
					    }
					    catch(Exception ex)
					    {
					        ex.printStackTrace();
					    }
					 }
				}
		}
		
	});
	heartNode.setDaemon(true);
	heartNode.setName("heartNode");
	if(!heartNode.isAlive())
	{
		heartNode.start();
	}
}


/**
 * 新增主题注册
 */
private void startTimeTopic()
{
	Thread topicNode=new Thread(new Runnable() {
		public void run() {
			while(!isStop)
			{
				  // mqtoptic topicname brokername rqueue wqueue
			       System.out.println("启动新增主题上报");
				    String name = null;
					try {
						name = writeQueue.getAddTopic();
					} catch (InterruptedException e1) {
						
						e1.printStackTrace();
					}
					  System.out.println("新增主题上报："+name);
					StringBuffer buf=new StringBuffer();
					byte[] data=null;
					buf.append("mqtoptic");
					buf.append(" ");
					buf.append(name);
					buf.append(" ");
					buf.append(NodeConfig.LocalName);
				    buf.append(" ");
					buf.append(writeQueue.getSize());
					buf.append(" ");
					buf.append("2");
					data=buf.toString().getBytes(NodeConfig.charset);
					for(NetAddress ser:nameAddress)
					 {
					    NettyClient client = allClient.getOrDefault(ser, null);
                        if(client==null)
                        {
                            client=FactorySocket.createClient(ser.netType);
                            allClient.put(ser, client);
                            client.connect(ser.ip, ser.port);
                            client.resetConnect();
                            client.setRecviceTimeOut(2000);
                        }
                       client.send(data);
					 }
					//监测该主题有没有复制的组
					if(subscriberQueue.isCopy(name))
					{
					    mapCopy.put(name, null);
					}
					
				}
			
		}
		
	});
	topicNode.setDaemon(true);
	topicNode.setName("topicNode");
	if(!topicNode.isAlive())
	{
		topicNode.start();
	}
}


/**
 * 
 * @Title: startReportTopic   
 * @Description: 定时上报主题        
 * void      
 * @throws
 */
private void startReportTopic()
{

	Thread reportNode=new Thread(new Runnable() {
		public void run() {
			while(!isStop)
			{
				  // mqtoptic topicname brokername rqueue wqueue
			    System.out.println("启动定时上报主题信息，上报时间(s)："+NodeConfig.heartTime/1000);
			    try {
                    Thread.sleep(NodeConfig.heartTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
					StringBuffer buf=new StringBuffer();
					byte[] data=null;
					for(NetAddress ser:nameAddress)
					 {

					    NettyClient client = allClient.getOrDefault(ser, null);
					    if(ser.ip.equals("*"))
                         {
                           //主要是测试阶段
                           ser.ip=Tools.getLocalIP();
                         }
                        if(client==null)
                        {
                            client=FactorySocket.createClient(ser.netType);
                            allClient.put(ser, client);
                            client.connect(ser.ip, ser.port);
                            client.resetConnect();
                            client.setRecviceTimeOut(2000);
                        }
                      try
                      {
						for(String name:mapTopic.keySet())
						{
							buf.append("mqtoptic");
							buf.append(" ");
							buf.append(name);
							buf.append(" ");
							buf.append(NodeConfig.LocalName);
							buf.append(" ");
							buf.append(writeQueue.getSize());
							buf.append(" ");
							buf.append("2");
							data=buf.toString().getBytes(NodeConfig.charset);
							buf.setLength(0);
							
							 client.send(data);
							 try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                             
                                e.printStackTrace();
                            }
						}
                      }
                      catch(Exception ex)
                      {
                          ex.printStackTrace();
                      }
					
					 }
					
				}
			
		}
		
	});
	reportNode.setDaemon(true);
	reportNode.setName("reportTopic");
	if(!reportNode.isAlive())
	{
		reportNode.start();
	}
}

/**
 * 即是订阅也是获取数据的端口
 */
private void startSubscribe()
{
	Thread subscribe=new Thread(new Runnable() {
		public void run() {
		    subscriberQueue=new SubscriberQueue();
			proxy.node_pub();
			try {
                Thread.sleep(2000);
            } catch (InterruptedException e1) {
              
                e1.printStackTrace();
            }
			while(!isStop)
			{
			    byte[] data=null;
			    NettyRspClient result=null;
			try {
                 result = proxy.node_pub_data();
                data=(byte[]) result.getData();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            ByteBuffer req=ByteBuffer.wrap(data);
	    	//req.flip();
	    	byte head=req.get();
	    	byte[] dst=new byte[data.length-1];
	    	req.get(dst);
            String topic=new String(dst).replaceAll("\\s", " ");
	    	if(head==0)
	    	{
	    	    System.out.println("收到注册："+topic);
				//注册
	    		registerTopic(topic,result);
	    		//回执
	    		// result.setRsp(new byte[] {1},rsp);
	    		 //test=result;
	    	}
	    	else if(head==1)
	    	{
	    		//获取数据
	    	    System.out.println("收到数据传输请求");
	    	    HandOut(topic);
	    	}
	    	
			}
		}
		
		/**
		 * 
		 * @Title: registerTopic   
		 * @Description: 订阅主题  
		 * @param topic
		 * @param chanel
		 * @throws
		 * @return      
		 * String
		 */
		private String registerTopic(String topic,NettyRspClient chanel)
		{
			//通过订阅接收的信息 都是字符串，都用空格隔离；
			//主题 组名称 组是否是push 复制还是轮训
		    StringBuffer buf=new StringBuffer();
			String[]info=topic.split(" ");
			if(info.length==4)
			{
				String topicName=info[0];
				String groupName=info[1];
				boolean isPush=Boolean.valueOf(info[2]);
				boolean isCopy=Boolean.valueOf(info[3]);
				Subscriber subscriber=new Subscriber();
				if(chanel.client!=null)
				{
				    subscriber.address=chanel.client.getAddress().getHostAddress();
				    subscriber.port=chanel.client.getPort();
				    subscriber.rspClient=chanel;
				}
				//subscriber.chanel=chanel.chanel;
				subscriber.isCopy=isCopy;
				subscriber.isPush=isPush;
				subscriber.id=Tools.getID();
				if(subscriberQueue.addSubscriber(topicName, groupName, subscriber))
				{
				    //监测复制组
				    if(subscriberQueue.isCopy(topicName))
				    {
				        mapCopy.put(topicName, null);
				    }
				    buf.append("regRsp");
		            buf.append(" ");
		            buf.append(topicName);
	                buf.append(" ");
	                buf.append(groupName);
	                buf.append(" ");
	                buf.append(subscriber.id);
	                //订阅主题后，查看本broker有没有主题，没有就自动创建
	                if(!mapTopic.containsKey(topicName))
	                {
	                    writeQueue.addTopic(topicName);
	                    mapTopic.put(topicName, null);//注意2者顺序，不然不能加入
	                }
	                subscriber.setRsp(new byte[] {1},buf.toString());
				}
				//
				//subscriber.chanel(new byte[] {1},"heart");
				//chanel.writeAndFlush(new DatagramPacket(buf,client));
			}
			
			return buf.toString();
			
		}
		
		/**
		 * 主动获取的数据
		 * @param topic
		 */
		public void HandOut(String topic)
		{
			//获取主题数据
			//主题名称+组名称+订阅者ID+个数
			String[]req=topic.split(" ");
			Long subscriberId=Long.valueOf(req[2]);
			int len=Integer.valueOf(req[3]);
			String topicName=req[0];
			String groupName=req[1];
			if(req.length==4)
			{
			    System.out.println("传输请求格式正确");
			 if(!mapTopic.containsKey(topicName))
			 {
			    System.out.println("不存在传输请求主题："+topicName);
				return;
			 }
			else
			{
				//有该主题了
				//找到订阅主题的订阅者
				HandTopic hand=subscriberQueue.getSubscriber(groupName, subscriberId);
				if(hand.isPush||hand.Subscriber==null)
				{
					//该组不是主动获取则不应该
				    System.out.println("该组不是主动获取则不应该");
					return;
				}
				else
				{
					//找到该主题数据
				    boolean isRead=false;
				    if(hand.isCopy)
				    {
				    	//查找读取队列
				        Subscriber sub=null;
				        List<byte[]> lst=null;
				        if(readQueue.isexist(topicName))
				        {
				            //有数据，则匹配订阅者返回
				             sub=(Subscriber) hand.Subscriber;
				            //找到数据
				             lst=readQueue.getData(topicName, subscriberId, len);
				           
				        }
				        else
				        {
				            //查看写入队列，可能还没有监测分发
				             sub=(Subscriber) hand.Subscriber;
	                         lst=writeQueue.getData(topicName, len);
	                        //同时要放入读队列
	                        readQueue.addData(topicName, lst);
	                        isRead=true;
				        }
				        sendData(sub,lst,topicName,isRead);
				    }
				    else
				    {
				    	//只会在写入队列中
				        Subscriber sub=(Subscriber) hand.Subscriber;
				        List<byte[]> lst=writeQueue.getData(topicName, len);
				        sendData(sub,lst,topicName,false);
				    }
				}
			}
			}
		}
		
		/*
		 * 找到注册信息及数据
		 * 包括内部缓存，当前文件数据不读取
		 */
		private void sendData(Subscriber sub,List<byte[]> data,String topic,boolean isread)
		{
		  //如果数据空，看缓存
		  //  NettyRspClient rsp=new NettyRspClient();
		  //  rsp.chanel=(Channel) sub.chanel;
//		    if(!sub.address.isEmpty())
//		    {
//		      rsp.client=new InetSocketAddress(sub.address,sub.port);
//		    }
		    System.out.println("传输进入回执流程");
		    if(data==null||data.isEmpty())
		    {
		        //缓存
		        //dataSave文件落盘
                if(cache!=null)
                {
                     data=cache.getData(topic, 100);
                     if(data!=null&&isread)
                     {
                         readQueue.addData(topic, data);
                     }
                }
		    }
		    if(data==null||data.isEmpty())
            {
		        return;
            }
		    else
		    {
		        //回执数据请求
		       // rsp.setRsp(new byte[0],data);
		         System.out.println("传输正在回执:"+data.size());
		         sub.setRsp(new byte[] {0},data);
		         System.out.println("传输回复："+sub.rspClient.client.getPort());
		    }
		}
		
	});
	subscribe.setDaemon(true);
	subscribe.setName("subscribe");
	subscribe.start();
}

/**
 * 
 * @Title: startDistribution   
 * @Description: 分发推送的数据
 * void      
 * @throws
 */
private void startDistribution()
{
    Thread  distribution=new Thread(new Runnable() {
        
        /**
         * 
         * @Title: send   
         * @Description: 发送数据  
         * @param topicName    
         * void
         */
         private void send(String topicName)
         {
             //查看推送的订阅者
             //System.out.println("正在分发："+topicName);
             List<String> group=subscriberQueue.getGroup(topicName);//获取订阅该主题的组
             List<byte[]>lst=writeQueue.getData(topicName, 100);//获取数据
             if(lst==null||lst.isEmpty())
             {
                 return;//没有数据
             }
             if(group!=null)
             {
                 int size=group.size();
                 boolean isPoll=false;
                 boolean isCopy=false;
                 for(int j=0;j<size;j++)
                 {
                     HandTopic hand= subscriberQueue.getSubscriber(topicName, group.get(j));
                     if(hand.isPush)
                     {
                         //当前是推送；
                         if(hand.Subscriber==null)
                         {
                             cache.addData(topicName, lst);
                             continue;
                         }
                         try
                         {
                            System.out.println("分发数据："+topicName);
                           sendData(hand.Subscriber,lst,topicName);
                         }
                         catch(Exception ex)
                         {
                             ex.printStackTrace();
                         }
                     }
                     else
                     {
                         isPoll=true;
                         isCopy=hand.isCopy;
                     }
                 }
                 //该主题中含有获取以及复制时
                 if(isCopy&&isPoll)
                 {
                     //有抽取，并且复制时，放入读取队列
                     readQueue.addData(topicName, lst);
                 }
                 else if(isPoll)
                 {
                     //仅仅是客户端单一抽取，则放回写入队列
                     writeQueue.addData(topicName, lst);
                 }
             }
             else
             {
                 //说明没有订阅者，则进入缓存
                 cache.addData(topicName, lst);
             }
             
         
         }
        
         public void run() {
               
           while(!isStop)
           {
               
               writeQueue.sumTopic();
               String[] topicAll=new String[mapTopic.size()];
               mapTopic.keySet().toArray(topicAll);
               if(topicAll.length==0|| writeQueue.getSize()==0)
               {
                   try {
                    Thread.sleep(5000);//5s
                    continue;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
               }
               else
               {
                   try {
                       Thread.sleep(1000);//5s
                   } catch (InterruptedException e) {
                       e.printStackTrace();
                   }
               }
               final CyclicBarrier barrier=new CyclicBarrier(topicAll.length+1);
               for(int i=0;i<topicAll.length;i++)
               {
                   String topicName=topicAll[i];
                   fixedThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        send(topicName);
                        try {
                            barrier.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (BrokenBarrierException e) {
                            e.printStackTrace();
                        }
                    }
                       
                   });                  
               }
               //
               subscriberQueue.checkPushSubscribers();
               try {
                   //等待每个主题完成
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
           }
            
        }
        
        /**
         * 
         * @Title: sendData   
         * @Description:发送数据  
         * @param subs 订阅者
         * @param data 数据
         * @param topicName      主题
         * void      
         * @throws
         */
        private void sendData(Object subs,List<byte[]> data,String topicName)
        {
            //如果数据空，看缓存
           // NettyRspClient rsp=new NettyRspClient();
            Subscriber[] subscribers=null;
            if(subs instanceof Subscriber)
            {
                subscribers=new Subscriber[] {(Subscriber) subs};
            }
            else
            {
                subscribers=(Subscriber[]) subs;
            }
            if(data==null||data.isEmpty())
            {
                //缓存
                //dataSave文件落盘
                if(cache!=null)
                {
                     data=cache.getData(topicName, 100);
                }
            }
            if(data==null||data.isEmpty())
            {
                return;
            }
            Subscriber sub=null;
            for(int i=0;i<subscribers.length;i++)
            {
                //
                sub=subscribers[i];
                if(sub.rspClient.getState()==NetState.UnActive)
                {
                   continue;
                }
                System.out.println("分发数据");
                //rsp.chanel=sub.chanel;
                sub.setRsp(new byte[]{0}, data);
               // rsp.setRsp(new byte[] {0},data);
            }
           
            
        
        }
    
    });
    distribution.setDaemon(true);
    distribution.setName("distribution");
    if(!distribution.isAlive())
    {
        distribution.start();
    }
}

/**
 * 
 * @Title: startCache   
 * @Description: 管理缓存及内存      
 * void      
 * @throws
 */
private void startCache()
{
    if(cache==null)
    {
        cache=new CacheLocal();
    }
    Thread  cacheMem=new Thread(new Runnable() {

        public void run() {
          
           while(!isStop)
           {
               try {
                Thread.sleep(NodeConfig.cacheCheckTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
              
               if(cache.getmemSize()>NodeConfig.maxCacheMem||NodeConfig.maxCacheSize>cache.getSize())
               {
                   String[] keys=cache.getTopic();
                   try
                   {
                   for(int i=0;i<keys.length;i++)
                   {
                       //
                       List<byte[]> lst=cache.getData(keys[i], 100);
                       if(NodeConfig.cacheSave)
                       {
                           //落盘
                          if(dataSave==null)
                          {
                              dataSave=new DataDiskLocal();
                          }
                          dataSave.add(keys[i], lst);
                          dataSave.deleteFile(keys[i], NodeConfig.cacheFileTime);
                       }
                   }
                   }
                   catch(Exception ex)
                   {
                       ex.printStackTrace();
                   }
               }
           }
        }
       
    });
    cacheMem.setDaemon(true);
    cacheMem.setName("cacheMem");
    cacheMem.start();
}


}

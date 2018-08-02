/**
 * 
 */
package cd.strommq.topicServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import cd.strommq.channel.NettyRspClient;
import cd.strommq.channel.NettyServer;
import cd.strommq.log.LogFactory;
import cd.strommq.model.BrokerAddress;
import cd.strommq.model.BrokerCommand;
import cd.strommq.model.BrokerData;
import cd.strommq.model.BrokerLiveInfo;
import cd.strommq.model.Command;
import cd.strommq.model.QueueData;
import cd.strommq.model.TopicData;
import cd.strommq.nettyFactory.FactorySocket;
import cd.strommq.nettyFactory.NetAddress;
import cd.strommq.nettyFactory.RequestInfo;

/**
 * @author jinyu
 *
 */
public class NameServer {
private  NettyServer registerServer=null;
private Thread  serverThread=null;
private TopicData serverData=null;
private  volatile boolean isStop=false;
private Thread checkLive=null;
/**
 * 开启服务
 * @return
 */
public boolean  start()
{
   
	if(registerServer==null)
	{
		registerServer=FactorySocket.createServer(ServerConfig.srvNet);
		serverData=new TopicData();
	}
	startListener();
	// registerServer.srvIP=ServerConfig.srvIP;
	 //registerServer.srvport=ServerConfig.srvPort;
	serverThread=new Thread(new Runnable() {
		
		//接收3类信息：
		//1.broker注册，服务列表
		//2.broker 主题信息
		//3.心跳信息，broker更新时间
		//4.客户端请求信息（返回主题信息）
		//5.外部指令信息 
		public void run() {
		    try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
              
                e.printStackTrace();
            }
		    //先读取信息
		    try
		    {
		    FileBroker fr=new FileBroker();
		    String[] lines=fr.read();
		    if(lines!=null)
		    {
		        fr.delete();//先删除文件
		        for(int i=0;i<lines.length;i++)
		        {
		            registerBroker(lines[i]);
		        }
		    }
		    }
		    catch(Exception ex)
		    {
		        ex.printStackTrace();
		    }
			while(!isStop)
			{
			    NettyRspClient recData=registerServer.recvice();
			    String result=processData((byte[])recData.getData());
			    byte[] data=result.getBytes(ServerConfig.charset);
			    recData.setRsp(data);
			}
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
		 * 注册
		 * @param registerInfo
		 */
		private String registerBroker(String registerInfo)
		{
			StringBuffer buf=new StringBuffer();
			   //取出信息
			buf.append(registerInfo);
			buf.delete(0, 11);
			//取出名称
			int index=buf.indexOf(" ");
			String name=buf.substring(0, index);
			buf.delete(0, index+1);//去除名称及空格
			String type1=buf.substring(0, 3);
			buf.delete(0, 4);
			index=buf.indexOf(" ");//准备取出地址
			String addr1=buf.substring(0,index);
			buf.delete(0, index+1);
			//String type2=buf.substring(0, 3);
			buf.delete(0, 4);
			String tmp=buf.toString();
			String addr2="";
			if(tmp.contains("heart"))
			{
				//注册另外的心跳监测时间
				index=buf.indexOf(" ");//准备取出地址
				addr2=buf.substring(0,index);
				//
				buf.delete(0, index+5);
				serverData.brokerHeartTable.put(name, Long.valueOf(buf.toString().trim()));
				
			}
			else
			{
			    addr2=buf.toString();
			}
			//
			if(name.equals("name"))
			{
				//说明是默认名称，配置没有名称，则由nameServer控制名字，ID
				name="broker"+CommonTool.getID();
			}
			//地址信息
			BrokerAddress addr=new BrokerAddress();
			if(type1.trim().equals("pub"))
			{
				addr.pub=addr1.trim();
				addr.sub=addr2.trim();
			}
			else
			{
				addr.sub=addr1.trim();
				addr.pub=addr2.trim();
			}
			//
			BrokerData regBroker=serverData.brokerAddrTable.getOrDefault(name, null);
			//
			if(regBroker==null)
			{
				//说明还没有
				regBroker=new BrokerData();
				regBroker.brokerAddrs=addr;
				serverData.brokerAddrTable.put(name, regBroker);
			}
			else
			{
				//比较地址，可能是相同名称
				if(addr.pub.equals(regBroker.brokerAddrs.pub)||addr.sub.equals(regBroker.brokerAddrs.sub))
				{
					//有一个地址相同则是更新相同了
					regBroker.brokerAddrs=addr;
				}
				else
				{
					//名称重复了，强制修改名称
					name="broker"+CommonTool.getID();
					regBroker=new BrokerData();
					regBroker.brokerAddrs=addr;
					serverData.brokerAddrTable.put(name, regBroker);
				}
			}
			StringBuffer msg=new StringBuffer();
			msg.append("注册broker,");
			msg.append("broker名称：");
			msg.append(name);
			LogFactory.getInstance().addInfo(msg.toString());
			System.out.println(msg.toString());
			return name;
		}
		
		/**
		 * topic信息更新
		 * @param topicInfo
		 */
		private void topicUpdate(String topicInfo)
		{

			StringBuffer buf=new StringBuffer();
			   //取出信息mqtoptic test1 broker0 2 2
			buf.append(topicInfo);
			buf.delete(0, 9);
			int index=buf.indexOf(" ");
			String topicName=buf.substring(0, index);
			buf.delete(0, index+1);
			index=buf.indexOf(" ");
			String brokerName=buf.substring(0, index);
			buf.delete(0, index+1);
			index=buf.indexOf(" ");
			String rQueueNum=buf.substring(0, index);
			buf.delete(0, index+1);
			String wQueueNum=buf.toString().trim();
			//index=buf.indexOf(" ");
			//String wQueueNum=buf.substring(0, index);
			List<QueueData> lst=serverData.topicQueueTable.getOrDefault(topicName, null);
			if(lst==null)
			{
				lst=new ArrayList<QueueData>(10);
				serverData.topicQueueTable.put(topicName, lst);
				QueueData e=new QueueData();
				e.brokerName=brokerName;
				e.readQueueNums=Integer.valueOf(rQueueNum);
				e.writeQueueNums=Integer.valueOf(wQueueNum);
				lst.add(e);
			}
			else
			{
				QueueData e=new QueueData();
				e.brokerName=brokerName;
				e.readQueueNums=Integer.valueOf(rQueueNum);
				e.writeQueueNums=Integer.valueOf(wQueueNum);
				int size=lst.size();
				for(int i=0;i<size;i++)
				{
					QueueData cur=lst.get(i);
					if(cur.brokerName.equals(e.brokerName))
					{
						lst.remove(i);
						break;
					}
				
				}
				lst.add(e);
			}
			
		
		}
		
		/**
		 * 更新心跳数据
		 * @param heartInfo
		 */
		private void heartUpdate(String heartInfo)
		{
			StringBuffer buf=new StringBuffer();
			   //取出信息
			buf.append(heartInfo);
			buf.delete(0, 7);
			String brokerName=buf.toString().trim();
			BrokerLiveInfo blive=serverData.brokerLiveTable.getOrDefault(brokerName, null);
			if(blive==null)
			{
				blive=new BrokerLiveInfo();
				blive.setTime();
				serverData.brokerLiveTable.put(brokerName, blive);
			}
			else
			{
				blive.setTime();
			}
		}
		
		/**
		 * 客户端获取信息
		 * @param quest
		 * @return
		 */
		private String responceClient(String quest)
		{
		    RequestInfo info=new RequestInfo();
		    Iterator<Entry<String, List<QueueData>>> kv = serverData.topicQueueTable.entrySet().iterator();
		     while(kv.hasNext())
             {
		        Entry<String, List<QueueData>> item = kv.next();
		        List<QueueData> lst=item.getValue();
		        List<String> broker=new ArrayList<String>();
		        info.mapTopic.put(item.getKey(), broker);
		        int size=lst.size();
                for(int i=0;i<size;i++)
                {
                  QueueData cur=lst.get(i);
                  broker.add(cur.brokerName);
                 }
             }
		     //
		     Iterator<Entry<String, BrokerData>> addr = serverData.brokerAddrTable.entrySet().iterator();
		     while(addr.hasNext())
             {
		         Entry<String, BrokerData> item = addr.next();
		         info.mapPub.put(item.getKey(), convertAddr(item.getValue().brokerAddrs.pub));
		         info.mapSub.put(item.getKey(), convertAddr(item.getValue().brokerAddrs.sub));
		         info.mapRPC.put(item.getKey(), convertAddr(item.getValue().brokerAddrs.rpc));
             }
		     return JSONSerializable.Serialize(info);
			//2组
			//StringBuffer buf=new StringBuffer();
			//buf.append("topic:");
//			Iterator<Entry<String, List<QueueData>>> kv = serverData.topicQueueTable.entrySet().iterator();
//			buf.append("{");
//			while(kv.hasNext())
//			{
//				buf.append("name:");
//				buf.append(kv.next().getKey());
//				buf.append(",");
//				buf.append("queue:");
//				List<QueueData> lst=kv.next().getValue();
//				int size=lst.size();
//				for(int i=0;i<size;i++)
//				{
//					QueueData cur=lst.get(i);
//					buf.append(cur.brokerName);
//					buf.append(",");
//					buf.append(cur.readQueueNums);
//					buf.append(",");
//					buf.append(cur.writeQueueNums);
//					buf.append(";");
//				}
//				buf.append(",");
//			}
//			buf.append("}");
//			buf.append(",");
//			buf.append("broker:");
//			buf.append("{");
//			Iterator<Entry<String, BrokerData>> addr = serverData.brokerAddrTable.entrySet().iterator();
//			while(addr.hasNext())
//			{
//				buf.append("name:");
//				buf.append(addr.next().getKey());
//				buf.append(",");
//				buf.append("address:");
//				buf.append("pub ");
//				buf.append(addr.next().getValue().brokerAddrs.pub);
//				buf.append(" sub ");
//				buf.append(addr.next().getValue().brokerAddrs.sub);
//				buf.append(" rpc ");
//				buf.append(addr.next().getValue().brokerAddrs.rpc);
//				buf.append(";");
//			}
//			return buf.toString();
		}
		
		/**
		 * 处理指令信息
		 * 指令只针对broker操作topic
		 * @param commond
		 */
		private String exeCommond(String commond)
		{
			LogFactory.getInstance().addInfo("收到指令:"+commond);
			StringBuffer buf=new StringBuffer();
			buf.append(commond);
			buf.delete(0, 7);
			//
			int index=buf.indexOf(" ");
			String cmdName=buf.substring(0, index);
			buf.delete(0, index);
			index=buf.indexOf(" ");
			String brokerName=buf.substring(0, index);
			buf.delete(0, index);
			index=buf.indexOf(" ");
			String topicName="";
			if(index>0)
			{
			  topicName=buf.substring(0, index);
			  buf.delete(0, index);
			}
			else
			{
				topicName=buf.toString();
			}
			BrokerCommand cmd=new BrokerCommand();
			cmd.cmd=Command.valueOf(cmdName);
			cmd.topicName=topicName;
			//
			BrokerCommand lastCmd=serverData.brokerCommondTable.getOrDefault(brokerName, null);
			if(lastCmd!=null)
			{
				if(lastCmd.cmd==Command.updatetopic)
				{
					if(lastCmd.createTime+24*60*60*10000>System.currentTimeMillis())
					{
						return "fail";
					}
				}
			}
			if(lastCmd==null||lastCmd.createTime+ServerConfig.updateCommandTime<System.currentTimeMillis())
			{
				//可以更新了
				serverData.brokerCommondTable.put(brokerName, cmd);
				//
				List<QueueData> lst=serverData.topicQueueTable.getOrDefault(topicName, null);
				switch(cmd.cmd)
				{
				case deltopic:
					 if(lst!=null)
					 {
						 int size=lst.size();
						 for(int i=0;i<size;i++)
						 {
							 if(lst.get(i).brokerName.equals(brokerName))
							 {
								 lst.remove(i);
								 break;
							 }
						 }
						 
					 }
					 break;
				case createtopic:
					 if(lst!=null)
					 {
						 int size=lst.size();
						 boolean isFind=false;
						 for(int i=0;i<size;i++)
						 {
							 if(lst.get(i).brokerName.equals(brokerName))
							 {
								 isFind=true;
								 break;
							 }
						 }
						 //
						 if(!isFind)
						 {
					       QueueData cur=new QueueData();
					       cur.brokerName=brokerName;
					       cur.readQueueNums=0;
					       cur.readQueueNums=0;
					       lst.add(cur);
						 }
					 }
					 else
					 {
						 //完成新建
						 lst=new ArrayList<QueueData>();
						 QueueData cur=new QueueData();
					       cur.brokerName=brokerName;
					       cur.readQueueNums=0;
					       cur.readQueueNums=0;
					       lst.add(cur);
					       serverData.topicQueueTable.put(topicName, lst);
					 }
				case updatetopic:
					//更新，删除后等待其它broker创建
					//该broker将在一天内不能创建topic
					 if(lst!=null)
					 {
						 int size=lst.size();
						 for(int i=0;i<size;i++)
						 {
							 if(lst.get(i).brokerName.equals(brokerName))
							 {
								 lst.remove(i);
								 break;
							 }
						 }
						 
					 }
				default:
					break;
					
				}
				serverData.brokerCommondTable.put(brokerName, cmd);
				return "sucess";
			}
			else
			{
				return "fail";
			}
			
			
			
		}
		
		/**
		 * 分类处理数据
		 * @param data
		 * @return
		 */
		private String processData(byte[]data)
		{
			String result="";
			String rec=new String(data).toLowerCase().trim();
			String request=rec.replaceAll("\\s", " ");//处理成一个空格
			//1.broker注册 mqregister name pub xxxx sub xxxx heart .....
			if(request.startsWith("mqregister"))
			{
				result=registerBroker(request);
				//
			//	String line=request.replaceFirst(" name ", " "+result+" ");
//				try
//				{
//				FileBroker fr=new FileBroker();
//				fr.write(line);
//				}
//				catch(Exception ex)
//				{
//				    ex.printStackTrace();
//				}
			}
			//2.topic信息  mqtoptic topicname brokername rqueue wqueue
			else if(request.startsWith("mqtoptic"))
			{
				topicUpdate(request);
			}
			//3. broker心跳  heart brokername
			else if(request.startsWith("heart"))
			{
				heartUpdate(request);
			}
			//4.客户端请求 request
			else if(request.startsWith("request"))
			{
				 result=responceClient(request);
				
			}
			//5.指令   command cmdname brokername topic xxxx
			else if(request.startsWith("command"))
			{
				result=exeCommond(request);
			}
			return result;
		}
		}
	);
	serverThread.setDaemon(true);
	serverThread.setName("nameServer");
	if(!serverThread.isAlive())
	{
	   serverThread.start();
	}
	startCheck();
	return true;
	
}


/**
 * 
 * @Title: startListener   
 * @Description: 启动服务监听         
 * void      
 * @throws
 */
public void  startListener()
{
   Thread listener=new Thread(new Runnable() {
        
        //接收3类信息：
        //1.broker注册，服务列表
        //2.broker 主题信息
        //3.心跳信息，broker更新时间
        //4.客户端请求信息（返回主题信息）
        //5.外部指令信息 
        public void run() {
            while(!isStop)
            {
                LogFactory.getInstance().addInfo("启动nameServer监听");
                System.out.println("监听端口："+ServerConfig.srvPort);
                registerServer.start(ServerConfig.srvPort);
            }
        }});
   listener.setDaemon(true);
   listener.setName("recData");
   if(!listener.isAlive())
   {
       listener.start();
   }
}

/**
 * 验证存活情况
 */
private void startCheck()
{
	checkLive=new Thread(new Runnable() {
      private void print()
      {
    	//2组
			StringBuffer buf=new StringBuffer();
			buf.append("topic:");
			Iterator<Entry<String, List<QueueData>>> kv = serverData.topicQueueTable.entrySet().iterator();
			buf.append("{");
			while(kv.hasNext())
			{
				buf.append("name:");
				buf.append(kv.next().getKey());
				buf.append(",");
				buf.append("queue:");
				List<QueueData> lst=kv.next().getValue();
				int size=lst.size();
				for(int i=0;i<size;i++)
				{
					QueueData cur=lst.get(i);
					buf.append(cur.brokerName);
					buf.append(",");
					buf.append(cur.readQueueNums);
					buf.append(",");
					buf.append(cur.writeQueueNums);
					buf.append(";");
				}
				buf.append(",");
			}
			buf.append("}");
			buf.append(",");
			buf.append("broker:");
			buf.append("{");
			Iterator<Entry<String, BrokerData>> addr = serverData.brokerAddrTable.entrySet().iterator();
			while(addr.hasNext())
			{
				buf.append("name:");
				buf.append(addr.next().getKey());
				buf.append(",");
				buf.append("address:");
				buf.append("pub ");
				buf.append(addr.next().getValue().brokerAddrs.pub);
				buf.append(" sub ");
				buf.append(addr.next().getValue().brokerAddrs.sub);
				buf.append(" rpc ");
				buf.append(addr.next().getValue().brokerAddrs.rpc);
				buf.append(";");
			}
			LogFactory.getInstance().addInfo(buf.toString());
      }
		public void run() {
			List<String> lstBroker=new ArrayList<String>();
			int count=0;
		while(!isStop)
		{
			//1分钟
			try {
				Thread.sleep(60*1000);
				count++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(serverData==null)
			{
				continue;
			}
			Iterator<Entry<String, BrokerLiveInfo>> kv = serverData.brokerLiveTable.entrySet().iterator();
			while (kv.hasNext()) {
				Entry<String, BrokerLiveInfo> item = kv.next();
				Long time=serverData.brokerHeartTable.getOrDefault(item.getKey(), null);
				if(time==null)
				{
					time=ServerConfig.checkLiveTime;
				}
				if(item.getValue().getLastUpdate()+time<System.currentTimeMillis())
				{
					//如果已经超时则移除
					//移除地址
					serverData.brokerAddrTable.remove(item.getKey());
					kv.remove();
					lstBroker.add(item.getKey());
				}
				
				
			}
			//如果移除broker;主题下的信息也移除
			if(!lstBroker.isEmpty())
			{
				Iterator<Entry<String, List<QueueData>>> queue = serverData.topicQueueTable.entrySet().iterator();
			    while(queue.hasNext())
			    {
			    	Entry<String, List<QueueData>> item = queue.next();
			    	if(!item.getValue().isEmpty())
			    	{
			    		int size=item.getValue().size();
			    		for(int i=0;i<size;i++)
			    		{
			    			if(lstBroker.contains(item.getValue().get(i).brokerName))
			    			{
			    				item.getValue().remove(i);
			    				size--;
			    			}
			    		}
			    	}
			    }
			    lstBroker.clear();
			}
			//处理完成后查看是否打印
			if(count%30==0)
			{
				print();
				count=0;
			}
			
		}
			
		}
		
	}) ;
	checkLive.setDaemon(true);
	checkLive.setName("nameServerCheck");
	if(!checkLive.isAlive())
	{
	  checkLive.start();
	}
}

/**
 * 关闭
 */
public void close()
{
	this.isStop=true;
	this.registerServer.close();
}
}

/**    
 * 文件名：NettyModel.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.mqclient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import cd.strommq.channel.NettyClient;

/**    
 *     
 * 项目名称：mqclient    
 * 类名称：NettyModel    
 * 类描述：   保持客户端订阅
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 下午7:50:55    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 下午7:50:55    
 * 修改备注：    
 * @version     
 *     
 */
public class NettyModel {
  private ReentrantReadWriteLock obj_lock=new ReentrantReadWriteLock();//控制订阅主题
    
    /**
     * 客户端
     */
  private   NettyClient client=null;
  
  /**
   * 使用时间
   */
  private long time=System.currentTimeMillis();
  
  
  /**
   * 主题信息（topicName+Group）+subid
   */
  private HashMap<String,String> mapTopic=new HashMap<String,String>();
  
  /**
   * topic+group
   */
  private HashMap<String,byte[]> mapRegInfo=new HashMap<String,byte[]>();
  
  /**
   * broker,一个客户端只能订阅一个broker
   */
  private String brokerName;
  
  /**
   * 订阅成功加入的组
   */
  private HashMap<String,String> subGroup=new HashMap<String,String>();
  
  /**
   * 订阅的主题
   */
  private HashMap<String,String> subTopic=new HashMap<String,String>();
  
  /**
    * 订阅的主题及组
   */
  private HashMap<String,String> pollTopic=new HashMap<String,String>();
  
  /**
   * 
   * @Title: get   
   * @Description: 返回客户端   
   * @return
   * @throws     
   * NettyClient
   */
  public NettyClient get()
  {
      time=System.currentTimeMillis();
      return this.client;
  }
  
  /**
   * 
   * @Title: set   
   * @Description:设置通信客户端
   * @param client
   * @throws     
   * void
   */
  public void set(NettyClient client)
  {
      this.client=client;
  }
  
  /**
   * 
   * @Title: lastUseTime   
   * @Description: 获取最后使用时间 
   * @return
   * @throws     
   * long
   */
  public long lastUseTime()
  {
      return time;
  }
  
  /**
   * 
   * @Title: getSubInfo   
   * @Description: 获取订阅者信息
   * @return
   * @throws     
   * List<String>
   */
  public Map<String,String> getSubInfo()
  {
      //防止外部使用修改，可以单独处理
      HashMap<String,String> map=new HashMap<String,String>();
      obj_lock.readLock().lock();
      map.putAll(mapTopic);
      obj_lock.readLock().unlock();
      return map;
  }
  
  /**
   * 
   * @Title: getSubPollInfo   
   * @Description: 获取订阅POLL类型信息
   * @return
   * @throws     
   * List<String>
   */
  public Map<String,String> getSubPollInfo()
  {
      //防止外部使用修改，可以单独处理
      HashMap<String,String> map=new HashMap<String,String>();
      obj_lock.readLock().lock();
      map.putAll(this.pollTopic);
      obj_lock.readLock().unlock();
      return map;
  }
  
  /**
   * 
   * @Title: getRegInfo   
   * @Description: 返回原始订阅信息  
   * @param key
   * @return
   * @throws     
   * byte[]
   */
  public byte[] getRegInfo(String key)
  {
      return this.mapRegInfo.getOrDefault(key, null);
  }
  
  /**
   * 
   * @Title: getBroker   
   * @Description: 返回 
   * @return
   * @throws     
   * String
   */
  public String getBroker()
  {
      return this.brokerName;
  }
  /**
   * 
   * @Title: rspResult   
   * @Description: 接收回执   
   * @param topic
   * @param group
   * @param id
   * @throws     
   * void
   */
  public void  rspResult(String topic,String group,String id)
  {
      String key=topic+" "+group;//主题+组
     if(this.mapTopic.containsKey(key))
     {
         //poll类型注册包含的
         this.mapTopic.put(key, id);
         pollTopic.put(key, id);
     }
     else if(this.subTopic.containsKey(topic))
     {
         //组名称已经被修改
         Iterator<String> iter = this.mapTopic.keySet().iterator();
         while(iter.hasNext())
         {
             String key1=iter.next();
             if(key1.contains(topic))
             {
                 //说明就是该主题；
                 mapTopic.remove(key1);
                 mapTopic.put(key, id);
                if(this.pollTopic.containsKey(key1))
                {
                    //修改
                    this.pollTopic.put(key,id);
                    this.pollTopic.remove(key1);
                }
                mapRegInfo.remove(key1);
                break;
             }
         }
     }
      this.subTopic.put(topic, null);
      this.subGroup.put(group, null);
      mapRegInfo.remove(key);//已经订阅成功的不再保持
  }
  /**
   * 
   * @Title: add   
   * @Description: 加入主题及poll类型组 ，准备再次注册
   * @param topic
   * @param group      
   * void      
   */
  public void add(String topic,String group,String brokerName,byte[]reginfo)
  {
      String key=topic+" "+group;//主题+组
      String value="."+System.currentTimeMillis()+","+brokerName;
       obj_lock.writeLock().lock();
      if(!this.mapTopic.containsKey(key))
      {
          this.mapTopic.put(key, value);
          this.mapRegInfo.put(key, reginfo);
          this.brokerName=brokerName;
      }
      subTopic.put(topic, null);
      subGroup.put(group, null);
      pollTopic.put(key, null);//作为poll类型的主题
      obj_lock.writeLock().unlock();
  }
  
  /**
   * 
   * @Title: close   
   * @Description: 关闭清理
   * @throws     
   * void
   */
  public void close()
  {
      this.mapRegInfo.clear();
      this.mapTopic.clear();
      this.client.close();
      this.client=null;
      this.mapRegInfo=null;
      this.mapTopic=null;
  }
}

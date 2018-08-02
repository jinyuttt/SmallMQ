/**    
 * 文件名：MQSub.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.mqclient;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
/**    
 *     
 * 项目名称：mqclient    
 * 类名称：MQSub    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 下午7:15:03    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 下午7:15:03    
 * 修改备注：    
 * @version     
 *     
 */
public class MQSubscriber {
   private LinkedBlockingQueue<SubscriberModel> queue=new  LinkedBlockingQueue<SubscriberModel>();
   private HashMap<String,String> topicName=new HashMap<String,String>();
   private AtomicInteger size=new AtomicInteger(0);
   private volatile boolean isclose=false;
 
   /**
   * 
   * @Title: Subscriber   
   * @Description: 订阅数据  
   * @param topic 主题
   * @param group 组名称
   * @param isPush 是否服务端推送
   * @param isCopy 数据是发给每个订阅者还是轮训
   * @return
   * @throws     
   * boolean
   */
 public boolean Subscriber(String topic,String group,boolean isPush,boolean isCopy)
{
     if(isclose)
     {
         return false;
     }
    if(topicName.containsKey(topic))
    {
        return true;
    }
    MQManager.getInstance().addSubscriber(topic, group, isPush, isCopy,this);
    topicName.put(topic, null);
    return true;
    
}
 
 /**
  * 
  * @Title: addData   
  * @Description: 后端返回数据使用，不能对外
  * @param data
  * @throws     
  * void
  */
 void addData(SubscriberModel data)
{
     if(this.isclose)
     {
        return;
     }
    try {
        queue.put(data);
        size.incrementAndGet();
    } catch (InterruptedException e) {
       
        e.printStackTrace();
    }
}

 /**
  * 
  * @Title: getData   
  * @Description: 同步阻塞获取数据
  * @return
  * @throws     
  * SubscriberModel
  */
public SubscriberModel getData()
{
    try {
        size.decrementAndGet();
        SubscriberModel data= queue.take();
        size.decrementAndGet();
        return data;
    } catch (Exception e) {
       
        e.printStackTrace();
    }
    return null;
}

/**
 * 
 * @Title: getData   
 * @Description: 超时获取数据 
 * @param timeOut
 * @return
 * @throws     
 * SubscriberModel
 */
public SubscriberModel getData(long timeOut)
{
    try {
        return queue.poll(timeOut, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      
        e.printStackTrace();
    }
    return null;
}

/**
 * 
 * @Title: clear   
 * @Description: 清除订阅的数据
 * void
 */
public void clear()
{
    queue.clear();
    size.set(0);
}

/**
 * 
 * @Title: destroy   
 * @Description: 销毁对象，不再接受数据及订阅    
 * void
 */
public void destroy()
{
    isclose=true;
    this.clear();
    this.queue=null;
    this.topicName.clear();
    this.topicName=null;
}

/**
 * 
 * @Title: isDestroy   
 * @Description: 获取销毁状态
 * @return    
 * boolean
 */
 boolean isDestroy()
{
    return this.isclose;
}
}

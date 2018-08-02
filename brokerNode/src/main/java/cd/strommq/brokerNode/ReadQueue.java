/**
 * 
 */
package cd.strommq.brokerNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author jinyu
 *
 */
public class ReadQueue {
	
/**
 * 数据
 */
 private HashMap<String,List<byte[]>> queue=new HashMap<String,List<byte[]>>();
 
 /**
  * 订阅者取出的数据索引
  */
 private HashMap<Long,Long> subIndex=new HashMap<Long,Long>();
 
 /**
  * 每个主题数据的长度
  */
 private HashMap<String,Long> dataLen=new HashMap<String,Long>();
 
 /**
  * 
  */
 private ReentrantReadWriteLock obj_lock=new ReentrantReadWriteLock();
 
 /**
  * 
  * @Title: addData   
  * @Description: 添加需要复制的数据
  * @param topicName
  * @param data      
  * void      
  * @throws
  */
 public  void addData(String topicName,byte[]data)
	{
	   obj_lock.writeLock().lock();
	   List<byte[]> lst=queue.getOrDefault(topicName, null);
	   Long len=dataLen.getOrDefault(topicName, null);
	   if(lst==null)
	   {
		   lst=new ArrayList<byte[]>(1000);
		   len=Long.valueOf(0);
		   queue.put(topicName, lst);
		   dataLen.put(topicName, len);
	   }
	   lst.add(data);
	   len=len+1L;
	   obj_lock.writeLock().unlock();
	}
 
 /**
  * 
  * @Title: addData   
  * @Description: 添加一组数据   
  * @param topicName
  * @param data      
  * void      
  * @throws
  */
 public  void addData(String topicName,List<byte[]>data)
 {
     if(data==null||data.isEmpty())
     {
         return;
     }
    obj_lock.writeLock().lock();
    List<byte[]> lst=queue.getOrDefault(topicName, null);
    Long len=dataLen.getOrDefault(topicName, null);
    if(lst==null)
    {
        lst=new ArrayList<byte[]>(1000);
        len=Long.valueOf(0);
        queue.put(topicName, lst);
        dataLen.put(topicName, len);
    }
    lst.addAll(data);
    len=len+data.size();
    obj_lock.writeLock().unlock();
 }
 /**   
  * @Title: getData   
  * @Description: 获取需要分发的数据
  * @param topicName 主题
  * @param subscriberid 订阅者ID
  * @param len 取出的长度
  * @return      
  * List<byte[]>      
  * @throws   
  */
  public  List<byte[]> getData(String topicName,long subscriberid,int len)
  {
	  obj_lock.readLock().lock();
	  List<byte[]> result=null;
	  List<byte[]> lst=queue.getOrDefault(topicName, null);//数据
	  Long Len=dataLen.getOrDefault(topicName, null);//数据的长度
	  Long sLen=subIndex.getOrDefault(subscriberid, null);//已经取出的数据
	  if(lst!=null)
	  {
		 int dLen=(int) (Len==null?0:Len);
		 int dSLen=(int) (sLen==null?0:sLen);
		 if(dLen<len+1)
		 {
		     result= lst;
		     subIndex.put(subscriberid,(long) dLen);
		 }
		 else
		 {
		     result=lst.subList(dSLen, dSLen+len);
		     subIndex.put(subscriberid,(long) dSLen+len);
		 }
	  }
	return result;
  }
  
  /**
   * 
   * @Title: isexist   
   * @Description: 监测是否存在主题数据
   * @param topicName
   * @return      
   * boolean      
   * @throws
   */
  public boolean  isexist(String topicName)
  {
      return queue.containsKey(topicName);
  }
}

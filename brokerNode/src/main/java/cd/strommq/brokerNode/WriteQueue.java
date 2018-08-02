/**
 * 
 */
package cd.strommq.brokerNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author jinyu
 *
 */
public class WriteQueue {
    private ReentrantReadWriteLock obj_lock=new ReentrantReadWriteLock();
	private	byte[] topicTmp=new byte[NodeConfig.topicNameMaxLen];
    private	HashMap<String,LinkedList<byte[]>> mapTopic=new HashMap<String,LinkedList<byte[]>>();
    private HashMap<String,Long> queueSize=new HashMap<String,Long>();
    public volatile int num=0;
    public volatile long sum=0;
    private TopicQueue parent=null;
    WriteQueue(TopicQueue queue)
    {
    	this.parent=queue;
    }
    
    /**
     * 
     * @Title: addData   
     * @Description: 保持数据   
     * @param data
     * @return  是否需要添加队列
     * boolean      
     * @throws
     */
public  boolean addData(byte[]data)
{
	if(data==null||data.length==0)
	{
		return false;
	}
	String topicName="";
	String tmp="";
	obj_lock.writeLock().lock();
	if(data.length>NodeConfig.topicNameMaxLen)
	{
		 System.arraycopy(data, 0, topicTmp, 0, topicTmp.length);
		 tmp=new String(topicTmp);
	}
	else
	{
		tmp=new String(data);
	}
	int index= tmp.indexOf(" ");
	topicName=tmp.substring(0,index);
	LinkedList<byte[]> queue= mapTopic.getOrDefault(topicName, null);
	Long len= queueSize.put(topicName, 1L);
	if(queue==null)
	{
	    queue=new LinkedList<byte[]>();
	    len=0L;
	    mapTopic.put(topicName, queue);
	    queueSize.put(topicName, len);
	    this.parent.addTopic(topicName);
	}
	queue.addLast(data);
	len=len+1L;
	num++;
	sum+=data.length;
	obj_lock.writeLock().unlock();
	if(NodeConfig.maxQueueLen<sum||NodeConfig.maxQueueSize<num)
	{
		return true;
	}
	return false;
}

/**
 * 
 * @Title: addData   
 * @Description: 返回数据
 * @param topicName
 * @param data      
 * void      
 * @throws
 */
public void addData(String topicName,List<byte[]>data)
{
    if(data==null||data.isEmpty())
    {
        return;
    }
    else
    {
        obj_lock.writeLock().lock();
        LinkedList<byte[]> queue= mapTopic.getOrDefault(topicName, null);
        Long len= queueSize.put(topicName, 1L);
        if(queue==null)
        {
            queue=new LinkedList<byte[]>();
            len=0L;
            mapTopic.put(topicName, queue);
            queueSize.put(topicName, len);
            this.parent.addTopic(topicName);
        }
        queue.addAll(data);
        len=len+1L;
        num+=data.size();
        sum+=data.size()*50;//内存不精确计算了，每包算50字节
        obj_lock.writeLock().unlock();
    }
}
/**
 * 
 * @Title: getTopic   
 * @Description: 获取数据主题
 * @return      
 * Map<String,String>      
 * @throws
 */
public Map<String, String> getTopic()
{
    obj_lock.readLock().lock();
    String[] keys=new String[this.mapTopic.size()];
	mapTopic.keySet().toArray(keys);
	HashMap<String,String> map=new HashMap<String,String>(keys.length);
	for(int i=0;i<keys.length;i++)
	{
	    map.put(keys[i], null);
	}
	obj_lock.readLock().unlock();
    return map;
}

/**
 * 
 * @Title: getData   
 * @Description: 只要取出就删除 
 * @param topic
 * @param len
 * @return      
 * List<String>      
 * @throws
 */
public List<byte[]> getData(String topic,int len)
{
    List<byte[]> lstData=null;
    obj_lock.readLock().lock();
    LinkedList<byte[]> lst = mapTopic.getOrDefault(topic, null);
    if(lst!=null)
    {
        if(len==0||len==-1)
        {
            queueSize.remove(topic);
            mapTopic.remove(topic);
            lstData=lst;
            num=num-lst.size();
            sum=sum-lst.size()*50;
        }
        else
        {
            lstData=new ArrayList<byte[]>(len);
            long dLen=queueSize.getOrDefault(topic, null);
            if(dLen<len+1)
            {
                mapTopic.remove(topic);
                queueSize.remove(topic);
                lstData=lst;
                num=num-lst.size();
                sum=sum-lst.size()*50;
            }
            else
            {
                queueSize.put(topic, dLen-len);
                for(int i=0;i<len;i++)
                {
                    byte[] cur=lst.remove();
                    sum=sum-cur.length;
                    lstData.add(cur);
                }
                num=num-len;
               
            }
        }
    }
   
    obj_lock.readLock().unlock();
    return lstData;
}
}

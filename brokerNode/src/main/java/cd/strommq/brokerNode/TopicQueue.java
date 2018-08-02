/**
 * 
 */
package cd.strommq.brokerNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author jinyu
 *
 */
public class TopicQueue {
private List<WriteQueue> lstQueue=null;
private volatile int index=0;//写入索引
private volatile int  readIndex=0;//读取索引；
private volatile int size=2;//至少2个替换
private Map<String, String> allTopic=null;
private LinkedBlockingQueue<String> topicAdd=null;
public TopicQueue(Map<String,String> topic)
{
	 this.allTopic=topic;
	 this.topicAdd=new LinkedBlockingQueue<String>();
	 lstQueue=new ArrayList<WriteQueue>(10);
     lstQueue.add(new WriteQueue(this));
     lstQueue.add(new WriteQueue(this));
}

/**
 * 添加数据
 * @Title: addQueue   
 * @Description:添加数据
 * @param data      
 * void      
 * @throws
 */
public void addQueue(byte[]data)
{
	if(lstQueue.get(index++%size).addData(data))
	{
		//满足扩展条件
		 lstQueue.add(new WriteQueue(this));
		 this.size+=1;
	}
}

/**
 * 
 * @Title: addData   
 * @Description:返回数据  
 * @param topicName
 * @param data      
 * void      
 * @throws
 */
public void addData(String topicName,List<byte[]> data)
{
    lstQueue.get(index++%size).addData(topicName,data);
    
}
/**
 * 
 * @Title: getData   
 * @Description: 获取数据   
 * @param topic
 * @param len
 * @return      
 * List<byte[]>      
 * @throws
 */
public List<byte[]> getData(String topic,int len)
{
    List<byte[]> result=new LinkedList<byte[]>();
    int r=readIndex;
    int dLen=len;
    while(true)
    {
        List<byte[]> lst=lstQueue.get(r).getData(topic, dLen);
        if(lst!=null)
        {
         result.addAll(lst);
         dLen=dLen-lst.size();
        }
        if(dLen<=0)
        {
            break;
        }
        r++;
       if(r==size)
       {
           r=0;
       }
       if(r==readIndex)
       {
           break;
       }
    }
    readIndex=r;
    return result;
}
/**
 * 
 * @Title: addTopic   
 * @Description: 新增主题   
 * @param topic      
 * void      
 * @throws
 */
public void addTopic(String topic)
{
	if(!this.allTopic.containsKey(topic))
	{
		topicAdd.add(topic);
	}
	
}

/**
 * 
 * @Title: getAddTopic   
 * @Description: 获取新增主题
 * @return
 * @throws InterruptedException      
 * String      
 * @throws
 */
public String getAddTopic() throws InterruptedException
{
	return topicAdd.take();
}
public int getSize()
{
	return this.size;
}

/**
 * 
 * @Title: sumTopic   
 * @Description: 整理
 * void      
 * @throws
 */
public void sumTopic()
{
	for(int i=0;i<size;i++)
	{
		allTopic.putAll(lstQueue.get(i).getTopic());
	}
}
}

/**
 * 
 */
package cd.strommq.brokerNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import cd.strommq.channel.NetState;
/**
 * @author jinyu
 *
 */
public class SubscriberQueue {
//订阅主题下的订阅组
private HashMap<String,List<String>> subGroup=null;
//订阅组下的订阅者
private HashMap<String,SubscribersGroup> groupSubscribers=null;

private ReentrantReadWriteLock obj_lock=new ReentrantReadWriteLock();

public SubscriberQueue()
{
	subGroup=new HashMap<String,List<String>>();
	groupSubscribers=new HashMap<String,SubscribersGroup>();
}

/**
 * 添加订阅者
 * @param topicName 主题
 * @param groupName 分组
 * @param s 
 */
public boolean addSubscriber(String topicName,String groupName,Subscriber s)
{
    boolean isSucess=false;
	obj_lock.writeLock().lock();
	//添加订阅组信息
	List<String> lst=subGroup.getOrDefault(topicName, null);
	if(lst==null)
	{
		lst=new ArrayList<String>(20);
		subGroup.put(topicName, lst);
	}
	if(!lst.contains(groupName))
	{
		lst.add(groupName);
	}
	//添加订阅信息
	SubscribersGroup group = groupSubscribers.getOrDefault(groupName, null);
	if(group==null)
	{
		group=new SubscribersGroup();
		groupSubscribers.put(groupName, group);
	}
	isSucess=group.add(s);
	obj_lock.writeLock().unlock();
    return isSucess;
}

/**
 * 获取订阅
 * @param topicName
 * @param groupName
 * @return
 */
public HandTopic getSubscriber(String topicName,String groupName)
{
	obj_lock.readLock().lock();
	List<String> lst=subGroup.getOrDefault(topicName, null);
	if(lst==null)
	{
		return null;
	}
	//添加订阅信息
	SubscribersGroup group = groupSubscribers.getOrDefault(groupName, null);
	if(group==null)
	{
		return null;
	}
	Object sub=group.getSubscribers();
	HandTopic handOut=new HandTopic();
	handOut.isCopy=group.isCopy();
	handOut.isPush=group.isPush();
	handOut.Subscriber=sub;
	obj_lock.readLock().unlock();
	return handOut;
}

/**
 * 
 * @Title: getGroup   
 * @Description: 获取主题的所有组
 * @param topic
 * @return      
 * List<String>      
 * @throws
 */
public List<String> getGroup(String topic)
{
     List<String> lst=null;
     obj_lock.readLock().lock();
     lst=subGroup.getOrDefault(topic, null);
     obj_lock.readLock().unlock();
     return lst;
    
}

/**
 * 
 * @Title: getSubscriber   
 * @Description: 订阅者ID  
 * @param subscriberID
 * @return      
 * HandTopic      
 * @throws
 */
public HandTopic getSubscriber(String groupName, long subscriberID)
{
    obj_lock.readLock().lock();
    //订阅信息
    SubscribersGroup group = groupSubscribers.getOrDefault(groupName, null);
    if(group==null)
    {
        return null;
    }
    Object sub=group.getSubscriberByID(subscriberID);
    HandTopic handOut=new HandTopic();
    handOut.isCopy=group.isCopy();
    handOut.isPush=group.isPush();
    handOut.Subscriber=sub;
    obj_lock.readLock().unlock();
    return handOut;
}

/**
 * 
 * @Title: isCopy   
 * @Description: 
 * @param topic
 * @return      
 * boolean      
 * @throws
 */
public boolean isCopy(String topicName)
{
    boolean isexist=false;
    obj_lock.readLock().lock();
  //添加订阅组信息
    List<String> lst=subGroup.getOrDefault(topicName, null);
    if(lst!=null)
    {
        int size=lst.size();
       for(int i=0;i<size;i++)
       {
           SubscribersGroup group = groupSubscribers.getOrDefault(lst.get(i), null);
           if(group.isCopy())
           {
               isexist=true;
               break;
           }
       }
    }
    obj_lock.readLock().unlock();
    return isexist;
   
}

/**
 * 
 * @Title: getPushSubscribers   
 * @Description: 获取所有PUSH类型的订阅者
 * @return    
 * List<Subscriber>
 */
public List<Subscriber> getPushSubscribers()
{
    List<Subscriber> lst=new ArrayList<Subscriber>(20);
    obj_lock.readLock().lock();
    Iterator<SubscribersGroup> iter = groupSubscribers.values().iterator();
    if(iter.hasNext())
    {
        SubscribersGroup suber=iter.next();
        if(suber.isPush())
        {
            lst.addAll(suber.getAll());
        }
    }
    obj_lock.readLock().unlock();
    return lst;
}

/**
 * 
 * @Title: checkPushSubscribers   
 * @Description: 监测无用PUSH     
 * void
 */
public void checkPushSubscribers()
{
   
    obj_lock.writeLock().lock();
    Iterator<SubscribersGroup> iter = groupSubscribers.values().iterator();
    if(iter.hasNext())
    {
        SubscribersGroup suber=iter.next();
        if(suber.isPush())
        {
           List<Subscriber> lst = suber.getAll();
           for(Subscriber s:lst)
           {
               if(s.rspClient.getState()==NetState.UnActive)
               {
                   suber.remove(s.id);
                   System.out.println("过期，移除订阅者："+s.id);
               }
           }
           
        }
    }
    obj_lock.writeLock().unlock();
   
}
/**
 * 
 * @Title: getAll   
 * @Description: 获取所有订阅者，测试用
 * @return    
 * List<Subscriber>
 */
public List<Subscriber> getAll()
{
    List<String> lst=new ArrayList<String>();
    List<Subscriber> lstAll=new ArrayList<Subscriber>();
    Iterator<List<String>> iter = subGroup.values().iterator();
    while(iter.hasNext())
    {
        lst.addAll(iter.next());
    }
    //
    for(String gropu:lst)
    {
        SubscribersGroup group=groupSubscribers.getOrDefault(gropu, null);
        if(group!=null)
        {
            lstAll.addAll(group.getAll());
        }
    }
    return lstAll;
}
} 

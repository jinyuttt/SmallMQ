/**
 * 
 */
package cd.strommq.brokerNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jinyu
 *
 */
public class SubscribersGroup {
private HashMap<Long,Subscriber> mapSub=null;
private List<Long> lst=null;
private volatile boolean isCopy=true;
private volatile boolean isPush=false;
private int index=0;
private AtomicInteger size=new AtomicInteger(0);
public SubscribersGroup()
{
    mapSub=new HashMap<Long,Subscriber>(30);
    lst=new ArrayList<Long>(30);
}
public boolean isPush()
{
	return this.isPush;
}
public boolean isCopy()
{
	return this.isCopy;
}
public boolean add(Subscriber subscriber)
{
	if(!mapSub.containsValue(subscriber))
	{
	    mapSub.put(subscriber.id, subscriber);
	    lst.add(subscriber.id);
		size.incrementAndGet();
		this.isCopy=subscriber.isCopy;
		this.isPush=subscriber.isPush;
		return true;
	}
    return false;
}


/**
 * 
 * @Title: getSubscribers   
 * @Description: 返回应该数据分发的订阅者
 * @return      
 * Object      
 * @throws
 */
public Object getSubscribers()
{
	//如果是复制则返回全部，否则返回一个
	if(isCopy)
	{
	    //返回所有
	    Subscriber[] all=new Subscriber[this.size.get()];
		 this.mapSub.values().toArray(all);
		 return all;
	}
	else
	{
		if(mapSub.isEmpty())
		{
			return null;
		}
		else
		{
			return mapSub.get(lst.get((index++)%size.get()));
		}
		
	}
}

/**
 * 
 * @Title: getAll   
 * @Description: 返回所有的订阅者，测试用
 * @return    
 * List<Subscriber>
 */
public List<Subscriber> getAll()
{
    List<Subscriber> lst=new ArrayList<Subscriber>();
     for(Subscriber s:mapSub.values())
     {
         lst.add(s);
     }
     return lst;
}


/**
 * 
 * @Title: getSubscriberByID   
 * @Description: 根据ID查找订阅者
 * @param id
 * @return      
 * Subscriber      
 * @throws
 */

public Subscriber getSubscriberByID(Long id)
{
    return mapSub.getOrDefault(id, null);
}

/**
 * 
 * @Title: remove   
 * @Description: 
 * @param id    
 * void
 */
public void remove(long id)
{
    mapSub.remove(id);
}
}

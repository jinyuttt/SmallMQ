/**    
 * 文件名：CacheLocal.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.brokerNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
/**    
 *     
 * 项目名称：brokerNode    
 * 类名称：CacheLocal    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 上午2:11:40    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 上午2:11:40    
 * 修改备注：    
 * @version     
 *     
 */
public class CacheLocal {
    private HashMap<String,LinkedList<byte[]>> cache=new  HashMap<String,LinkedList<byte[]>>();
    private ReentrantReadWriteLock obj_lock=new ReentrantReadWriteLock();
    private long memSum=0;//内存
    private int num=0;//包
    
    /**
     * 
     * @Title: addData   
     * @Description: 保持数据
     * @param topic
     * @param data      
     * void      
     * @throws
     */
public void addData(String topic,List<byte[]> data)
{
    obj_lock.writeLock().lock();
    LinkedList<byte[]> lst= cache.getOrDefault(topic, null);
    if(lst==null)
    {
        lst=new LinkedList<byte[]>();
        cache.put(topic, lst);
    }
    num=num+1;
    memSum=memSum+1;
    lst.addAll(data);
    obj_lock.writeLock().unlock();
}

/**
 * 
 * @Title: getData   
 * @Description: 保持数据 
 * @param topic
 * @param len
 * @return      
 * List<byte[]>      
 * @throws
 */
public List<byte[]> getData(String topic,int len)
{
    LinkedList<byte[]> lst= cache.getOrDefault(topic, null);
    if(lst==null)
    {
       return null;
    }
    else
    {
        int size=lst.size();
       List<byte[]> result=new ArrayList<byte[]>(len);
        if(size<=len)
        {
            obj_lock.writeLock().lock();
            lst=cache.remove(topic);
            obj_lock.writeLock().unlock();
            num=num-size;
            for(int i=0;i<len;i++)
            {
                byte[]cur=lst.remove();
                memSum=memSum-cur.length;
                result.add(cur);
                
            }
            return result;
        }
        else
        {
            obj_lock.readLock().lock();
            num=num-len;
            for(int i=0;i<len;i++)
            {
                byte[]cur=lst.remove();
                memSum=memSum-cur.length;
                result.add(cur);
            }
            obj_lock.readLock().unlock();
            return result;
        }
    }
}

public long getmemSize()
{
    return this.memSum;
}

public int getSize()
{
    return this.num;
}
public String[] getTopic()
{
    String[] keys=null;
    obj_lock.readLock().lock();
    keys=new String[cache.size()];
    cache.keySet().toArray(keys);
    obj_lock.readLock().unlock();
    return keys;
}
public boolean isexist(String topic)
{
    return cache.containsKey(topic);
}
}

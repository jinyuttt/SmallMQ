/**    
 * 文件名：ThreadFactroy.java    
 *    
 * 版本信息：    
 * 日期：2018年8月1日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.mqclient;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**    
 *     
 * 项目名称：mqclient    
 * 类名称：ThreadFactroy    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年8月1日 下午11:52:34    
 * 修改人：jinyu    
 * 修改时间：2018年8月1日 下午11:52:34    
 * 修改备注：    
 * @version     
 *     
 */
public class ClientThreadFactroy implements  ThreadFactory {
   public String name;
   public AtomicLong thradid=new AtomicLong(1);
  public  ClientThreadFactroy(String name)
  {
      this.name=name;
  }
    @Override
    public Thread newThread(Runnable arg) {
        Thread thread = new Thread(arg);
        thread.setName(name+"_"+thradid.getAndIncrement());  //对新创建的线程做一些操作
        thread.setDaemon(true);
        return thread;
    }

}

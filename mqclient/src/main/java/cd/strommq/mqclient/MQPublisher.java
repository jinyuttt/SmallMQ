/**    
 * 文件名：MQPublish.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.mqclient;

/**    
 *     
 * 项目名称：mqclient    
 * 类名称：MQPublish    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 下午7:14:52    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 下午7:14:52    
 * 修改备注：    
 * @version     
 *     
 */
public class MQPublisher {
public void  publish(String topicName,byte[]data)
{
    MQManager.getInstance().addPub(topicName, data);
}
}

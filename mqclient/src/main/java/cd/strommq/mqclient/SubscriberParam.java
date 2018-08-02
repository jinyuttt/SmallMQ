/**    
 * 文件名：SubscriberParam.java    
 *    
 * 版本信息：    
 * 日期：2018年7月31日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.mqclient;

/**    
 *     
 * 项目名称：mqclient    
 * 类名称：SubscriberParam    
 * 类描述：    订阅失败者信息
 * 创建人：jinyu    
 * 创建时间：2018年7月31日 下午2:08:40    
 * 修改人：jinyu    
 * 修改时间：2018年7月31日 下午2:08:40    
 * 修改备注：    
 * @version     
 *     
 */
public class SubscriberParam {
    String topic;String group;boolean isPush;boolean isCopy;MQSubscriber subscriber;
}

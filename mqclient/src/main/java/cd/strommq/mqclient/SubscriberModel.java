/**    
 * 文件名：SubModel.java    
 *    
 * 版本信息：    
 * 日期：2018年7月30日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.mqclient;

/**    
 *     
 * 项目名称：mqclient    
 * 类名称：SubModel    
 * 类描述：    订阅返回数据
 * 创建人：jinyu    
 * 创建时间：2018年7月30日 下午9:23:58    
 * 修改人：jinyu    
 * 修改时间：2018年7月30日 下午9:23:58    
 * 修改备注：    
 * @version     
 *     
 */
public class SubscriberModel {
public String topicName;
public byte[] data=null;
public String brokerName;
}

/**    
 * 文件名：ClientConfig.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.mqclient;

import java.util.List;

/**    
 *     
 * 项目名称：mqclient    
 * 类名称：ClientConfig    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 下午7:16:37    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 下午7:16:37    
 * 修改备注：    
 * @version     
 *     
 */
public class ClientConfig {
public static List<String> nameServer;
public static long reqTopicTime=10*6000;//10s
public static int findBroker=5;
public static String group="group1";
public static boolean isPush=false;
public static boolean isCopy=true;
public static long checkClientTime=5*60*6000;//5miu
public static int pollSize=100;//每次抽取的包数
public static long regTopicTime=10*1000;//注册主题完成时间长度，默认10s
public static int topicNameMaxLen=500;//topic最大程度
}

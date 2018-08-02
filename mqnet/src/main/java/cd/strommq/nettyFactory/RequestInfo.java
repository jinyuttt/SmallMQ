/**    
 * 文件名：RequestInfo.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.nettyFactory;

import java.util.HashMap;
import java.util.List;

/**    
 *     
 * 项目名称：brokerNode    
 * 类名称：RequestInfo    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 下午11:32:07    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 下午11:32:07    
 * 修改备注：    
 * @version     
 *     
 */
public class RequestInfo {
public HashMap<String,List<String>> mapTopic=new HashMap<String,List<String>>();
public HashMap<String,NetAddress> mapSub=new HashMap<String,NetAddress>();
public HashMap<String,NetAddress> mapPub=new HashMap<String,NetAddress>();
public HashMap<String,NetAddress> mapRPC=new HashMap<String,NetAddress>();
}

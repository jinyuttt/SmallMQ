/**    
 * 文件名：JSONSer.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.topicServer;

import com.alibaba.fastjson.JSON;

/**    
 *     
 * 项目名称：topicServer    
 * 类名称：JSONSer    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 下午11:38:01    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 下午11:38:01    
 * 修改备注：    
 * @version     
 *     
 */
public class JSONSerializable {
public static <T> String Serialize(T obj)
{
   return JSON.toJSONString(obj);
}
public static <T> T Deserialize(String json,Class<T>clazz)
{
   T obj= JSON.parseObject(json, clazz);
   return obj;
}
}

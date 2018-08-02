/**    
 * 文件名：NetState.java    
 *    
 * 版本信息：    
 * 日期：2018年8月2日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.channel;

/**    
 *     
 * 项目名称：mqnet    
 * 类名称：NetState    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年8月2日 上午7:31:20    
 * 修改人：jinyu    
 * 修改时间：2018年8月2日 上午7:31:20    
 * 修改备注：    
 * @version     
 *     
 */
public enum NetState {
    Active,
    Check,
    Check1000,
    CheckLast,//程序逻辑需要
    UnActive,
    Unknow//比较使用的
}

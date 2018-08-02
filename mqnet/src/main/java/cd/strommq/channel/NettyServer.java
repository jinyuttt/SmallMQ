/**    
 * 文件名：NettyServer.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.channel;

/**    
 *     
 * 项目名称：mqnet    
 * 类名称：NettyServer    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 下午12:35:56    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 下午12:35:56    
 * 修改备注：    
 * @version     
 *     
 */
public interface NettyServer {
public boolean start(int port);
public NettyRspClient recvice();
public void close();
}

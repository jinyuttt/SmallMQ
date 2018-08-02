/**    
 * 文件名：NettyClient.java    
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
 * 类名称：NettyClient    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 下午12:07:36    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 下午12:07:36    
 * 修改备注：    
 * @version     
 *     
 */
public interface NettyClient {
 /**
  * 
  * @Title: connect   
  * @Description: 连接   
  * @param host
  * @param port
  * @return
  * @throws     
  * boolean
  */
public boolean connect(String host,int port);

/**
 * 
 * @Title: send   
 * @Description: 发送数据   
 * @param data
 * @return
 * @throws     
 * int
 */
public int send(byte[]data);

/**
 * 
 * @Title: sendUDP   
 * @Description: 发送数据，专门为UDP等非连接准备   
 * @param host
 * @param port
 * @param data
 * @return
 * @throws     
 * int
 */
public int sendUDP(String host,int port,byte[]data);

/**
 * 
 * @Title: recvice   
 * @Description: 接收数据
 * @return
 * @throws     
 * byte[]
 */
public byte[] recvice();

/**
 * 
 * @Title: isClose   
 * @Description: 是否已经关闭   
 * @return
 * @throws     
 * boolean
 */
public boolean isClose();

/**
 * 
 * @Title: resetConnect   
 * @Description: 设置是否重新连接   
 * @throws     
 * void
 */
public void resetConnect();

/**
 * 
 * @Title: close   
 * @Description: 关闭
 * @throws     
 * void
 */
public void close();

/**
 * 
 * @Title: setRecviceTimeOut   
 * @Description: 设置超时接收  
 * @param time
 * @throws     
 * void
 */
public void setRecviceTimeOut(int time);

/**
 * 
 * @Title: getSize   
 * @Description: 获取客户端已经接收的数据量   
 * @return
 * @throws     
 * int
 */
public  long getSize();

/**
 * 
 * @Title: isEmpty   
 * @Description: 判断是否是空 
 * @return
 * @throws     
 * boolean
 */
public boolean isEmpty();

/**
 * 
 * @Title: setHeartTime   
 * @Description: 设置心跳监测时间长度，在连接使用前  
 * @param time    
 * void
 */
public void setHeartTime(long time);
/**
 * 
 * @Title: setID   
 * @Description: 设置通信端编号   
 * @param id    
 * void
 */
public void setID(long id);

/**
 * 
 * @Title: getID   
 * @Description: 获取通信段ID
 * @return    
 * long
 */
public long getID();


}

/**    
 * 文件名：ConnectionListener.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.tcp;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;

/**    
 *     
 * 项目名称：mqnet    
 * 类名称：ConnectionListener    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 下午1:51:48    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 下午1:51:48    
 * 修改备注：    
 * @version     
 *     
 */
public class ConnectionListener implements ChannelFutureListener {
    NettyTcpClient client=null;
 public ConnectionListener(NettyTcpClient client)
 {
     this.client=client;
 }
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
            if(client.isResetCon())
            {
            final EventLoop loop = future.channel().eventLoop();
            loop.schedule(new Runnable() {
                @Override
                public void run() {
                    System.err.println("服务端链接不上，开始重连操作...");
                    client.doConnnect();
                }
            }, 1L, TimeUnit.SECONDS);
            }
        } else {
            System.err.println("服务端链接成功...");
        }
    }

}

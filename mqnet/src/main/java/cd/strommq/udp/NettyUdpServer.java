/**
 * 
 */
package cd.strommq.udp;

import cd.strommq.channel.NettyChannel;
import cd.strommq.channel.NettyRspClient;
import cd.strommq.channel.NettyServer;
import cd.strommq.channel.NettyServerData;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * @author jinyu
 *
 */
public class NettyUdpServer implements NettyServer{
private int port=9999;
EventLoopGroup group=null;
ChannelFuture future=null;
public void start()  
{
	Bootstrap b = new Bootstrap();
	group = new NioEventLoopGroup();
     try
     {
	b.group(group)
	.channel(NioDatagramChannel.class)
    .option(ChannelOption.SO_BROADCAST, true)
	.option(ChannelOption.SO_RCVBUF, 1024 * 2048)
	.handler(new NettyUdpSeverHandler(port));
	//.handler(new IdleStateHandler(5, 5, 5, TimeUnit.MINUTES));
	// 服务端监听在9999端口
	System.out.println("UDPServer bind "+port);
	 ChannelFuture future=b.bind(port).sync();
	 future.channel().closeFuture().await();//阻塞
     }
     catch(Exception ex)
     {
    	 ex.printStackTrace();
     }
     finally
     {
    	 group.shutdownGracefully();
     }
}
@Override
public boolean start(int port) {
   this.port=port;
   NettyChannel channel=new NettyChannel();
   NettyServerData.addChannel(port, channel);
     start();
    return true;
}
@Override
public NettyRspClient recvice() {
    return NettyServerData.getData(port);
}
@Override
public void close() {
    if(future!=null)
    {
        future.channel().close();
    }
    if(group!=null)
    group.shutdownGracefully();
    
}
}

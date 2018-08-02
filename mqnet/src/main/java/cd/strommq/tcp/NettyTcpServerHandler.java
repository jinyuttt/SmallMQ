/**
 * 
 */
package cd.strommq.tcp;

import cd.strommq.channel.NettyChannel;
import cd.strommq.channel.NettyRspClient;
import cd.strommq.channel.NettyServerData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

/**
 * @author jinyu
 *
 */
public class NettyTcpServerHandler extends ChannelInboundHandlerAdapter{
    //ChannelHandlerAdapter 
    //ChannelInboundHandlerAdapter
    private int localPort=0;
    private int lossConnectCount=0;
    private String heart="heartclient";
    private int heartLen=0;
    public NettyTcpServerHandler(int port)
    {
        this.localPort=port;
        this.heartLen=heart.getBytes(CharsetUtil.UTF_8).length;
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("未收到客户端的消息了！");
        if (evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent)evt;
            if (event.state()== IdleState.READER_IDLE){
                lossConnectCount++;
                if (lossConnectCount>2){
                    System.out.println("关闭这个不活跃通道！");
                    ctx.channel().close();
                    NettyServerData.getChannel(localPort).removeChenel(ctx.channel());
                 
                }
            }
        }else {
            super.userEventTriggered(ctx,evt);
        }
    }
    
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
	    System.out.println("TCPServer接收到数据");
		//Discard the received data silently
		//ByteBuf是一个引用计数对象实现ReferenceCounted，他就是在有对象引用的时候计数+1，无的时候计数-1，当为0对象释放内存
		ByteBuf in=(ByteBuf)msg;
		try {
		    int num=in.readableBytes();
		    byte[]data=new byte[num];
		    in.readBytes(data);
		    if(num==heartLen)
            {
                String heartStr=new String(data);
                if(this.heart.compareToIgnoreCase(heartStr)==0)
                {
                    ReferenceCountUtil.release(msg);
                    //不要该数据
                    return;
                }
            }
			 NettyChannel channel=NettyServerData.getChannel(localPort);
			 if(channel==null)
			 {
			     channel=new NettyChannel();
			     channel.addChannel(ctx.channel());
			     NettyServerData.addChannel(localPort, channel);
			 }
			 //
			 NettyRspClient client=new NettyRspClient();
	         client.chanel=ctx.channel();
	         client.client=null;
	         //channel.addRsp(client);
			 channel.add(data, client);
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
	/**
       * 客户端与服务端创建连接的时候调用
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("收到客户端连接...");
        NettyChannel channel=NettyServerData.getChannel(localPort);
        if(channel==null)
        {
          channel=new NettyChannel();
          NettyServerData.addChannel(localPort, channel);
        }
        channel.addChannel(ctx.channel());
    }
 
    /**
       * 客户端与服务端断开连接时调用
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端关闭...");
        NettyServerData.getChannel(localPort).removeChenel(ctx.channel());
    }
 
    /**
       * 服务端接收客户端发送过来的数据结束之后调用
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
        System.out.println("信息接收完毕...");
    }

}

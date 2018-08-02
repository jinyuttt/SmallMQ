/**
 * 
 */
package cd.strommq.tcp;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;

/**
 * @author jinyu
 *
 */
public class NettyTcpClientHandler extends SimpleChannelInboundHandler<Object>  {
    //ChannelHandlerAdapter
    //ChannelInboundHandlerAdapter
    NettyTcpClient client=null;
    private int beatTime;
    private int curTime;
    private String heart="heartserver";
    private int heartLen=0;
  public NettyTcpClientHandler(NettyTcpClient client)
  {
       this.client=client;
       this.heartLen=heart.getBytes(CharsetUtil.UTF_8).length;
  }
  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      System.out.println("客户端循环心跳监测发送: "+new Date());
      if (evt instanceof IdleStateEvent){
          IdleStateEvent event = (IdleStateEvent)evt;
          if (event.state()== IdleState.WRITER_IDLE){
              if (curTime<beatTime){
                  curTime++;
                  ctx.writeAndFlush("heartclient");
              }
          }
      }
  }
//	@Override
//	public void channelRead(ChannelHandlerContext ctx, Object msg)
//			throws Exception {
//		    System.out.println("我是TCP客户端：");
//	    ByteBuf in=(ByteBuf) msg;
//        try {
//        //  while(in.isReadable()){
//            //  System.out.println((char)in.readByte());
//            //  System.out.flush();
//            //}
//            int num=in.readableBytes();
//            byte[]data=new byte[num];
//            in.readBytes(data);
//            if(num==heartLen)
//            {
//                String heartStr=new String(data);
//                if(this.heart.compareToIgnoreCase(heartStr)==0)
//                {
//                   // ReferenceCountUtil.release(msg);
//                    //不要该数据
//                    return;
//                }
//            }
//            this.client.addData(data);
//        } finally {
//           // ReferenceCountUtil.release(msg);
//        }
//	}
	
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
        System.out.println("客户端Active...");
        ctx.writeAndFlush("client TEST");
        ctx.channel().writeAndFlush("client channel");
    }
 
    /**
     * 客户端与服务端断开连接时调用
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端Inactive.");
        if(client.isResetCon())
        {
        final EventLoop eventLoop = ctx.channel().eventLoop();
        eventLoop.schedule(new Runnable() {
            @Override
            public void run() {
                client.doConnnect();
            }
        }, 5L, TimeUnit.SECONDS);
        super.channelInactive(ctx);
        }
    }
 
    /**
     * 服务端接收客户端发送过来的数据结束之后调用
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
        System.out.println("通道读取完毕！");
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("我是TCP客户端：");
        ByteBuf in=(ByteBuf) msg;
        try {
            int num=in.readableBytes();
            byte[]data=new byte[num];
            in.readBytes(data);
            if(num==heartLen)
            {
                String heartStr=new String(data);
                if(this.heart.compareToIgnoreCase(heartStr)==0)
                {
                 
                    //不要该数据
                    return;
                }
            }
            this.client.addData(data);
        } finally {
          
        }
        
    }

}

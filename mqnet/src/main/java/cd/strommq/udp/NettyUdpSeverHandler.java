/**
 * 
 */
package cd.strommq.udp;

import cd.strommq.channel.NettyChannel;
import cd.strommq.channel.NettyRspClient;
import cd.strommq.channel.NettyServerData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

/**
 * @author jinyu
 *
 */
public class NettyUdpSeverHandler extends SimpleChannelInboundHandler<DatagramPacket>{

    private int localPort=0;
    private String heart="heartUDPClient";//客户端心跳包
    private String rspheart="rspHeartUDPClient";//回复客户端心跳包
    private String rspClientheart="rspHeartUDPServer";//客户端回复的心跳包
    private byte[] rspClientHeart=null;
    private int heartLen=0;//客户端心跳长度
    private int rspheartLen=0;//客户端回复的心跳长度
    public NettyUdpSeverHandler(int port)
    {
        heartLen=heart.getBytes(CharsetUtil.UTF_8).length;
        rspClientHeart=rspheart.getBytes(CharsetUtil.UTF_8);
        rspheartLen=rspClientheart.getBytes(CharsetUtil.UTF_8).length;
        this.localPort=port;
    }
    
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
		// 读取收到的数据
        System.out.println("我是UDP服务端接收");
		ByteBuf buf = (ByteBuf) packet.copy().content();
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		NettyChannel channel=NettyServerData.getChannel(localPort);
         if(channel==null)
         {
             channel=new NettyChannel();
             NettyServerData.addChannel(localPort, channel);
         }
         NettyRspClient client=channel.getRsp(packet.sender());
         if(client==null)
         {
             client=new NettyRspClient();
             client.client=packet.sender();
             channel.addRsp(client);
             UDPHeart.addRsp(client);
         }
         client.setReadTime();
         client.chanel=ctx.channel();
         client.client=packet.sender();
         //
         if(heartLen==data.length||rspheartLen==data.length)
         {
             String head=new String(data);
             if(head.equals(heart))
             {
                 //心跳包;回复
                 System.out.println("UDP收到客户端心跳包："+localPort);
                 ByteBuf rspHeart=Unpooled.wrappedBuffer(rspClientHeart);
                 ctx.writeAndFlush(new DatagramPacket(rspHeart,packet.sender()));
                 return;
             }
             else if(head.equals(rspClientheart))
             {
                 System.out.println("UDP收到客户端回执的心跳包："+localPort);
                 //客户端回复的心跳
                 return;
             }
         }
         channel.add(data, client);
         //channel.add(req, ctx.channel(), packet.sender());
		
	}
	


}



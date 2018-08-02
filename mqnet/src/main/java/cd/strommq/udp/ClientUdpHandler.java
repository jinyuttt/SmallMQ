/**
 * 
 */
package cd.strommq.udp;

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
public class ClientUdpHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    NettyUdpClient client=null;
    private String heart="heartUDPServer";//服务端的心跳
    private String rspheart="rspHeartUDPServer";//回复服务端的心跳
    private String rspServerheart="rspHeartUDPClient";//服务端回复的心跳 
    private byte[] rspServerHeart=null;//回复服务端
    private int heartLen=0;//服务端心跳长度
    private int rspHeartLen=0;//服务端回复的长度
    public ClientUdpHandler(NettyUdpClient client)
   {
       this.client=client;
       this.heartLen=heart.getBytes(CharsetUtil.UTF_8).length;
       this.rspServerHeart=rspheart.getBytes(CharsetUtil.UTF_8);
       this.rspHeartLen=rspServerheart.getBytes(CharsetUtil.UTF_8).length;
       UDPHeart.addClient(client);
   }
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
	    System.out.println("我是UDP客户端接收:"+client.getID());
	    client.setReadTime();
	    ByteBuf in=(ByteBuf) packet.content();
        try {
            int num=in.readableBytes();
            byte[]data=new byte[num];
            in.readBytes(data);
            if(num==heartLen||num==rspHeartLen)
            {
                String heartStr=new String(data);
                if(this.heart.equals(heartStr))
                {
                   //
                    //不要该数据
                    System.out.println("UDP收到服务端心跳包");
                    ByteBuf buf=Unpooled.wrappedBuffer(rspServerHeart);
                    ctx.writeAndFlush(new DatagramPacket(buf,packet.sender()));
                    return;
                }
                else if(this.heart.equals(heartStr))
                {
                    //服务端回复的心跳
                    System.out.println("UDP收到服务端回复的心跳包");
                    return;
                }
            }
            this.client.addData(data);
        } finally {
          
        }
	}


}

/**
 * 
 */
package cd.strommq.tcp;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import cd.strommq.channel.NettyClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @author jinyu
 *
 */
public class NettyTcpClient implements NettyClient{
private String host;
private int port ;
private  Channel channel=null;
private EventLoopGroup workerGroup=null;
private LinkedBlockingQueue<byte[]> queue=null;
private Bootstrap b=null;
private ConnectionListener channelFutureListener=null;
private volatile boolean isResetCon=false;
private volatile int recTimeOut=-1;
private  AtomicLong size=new AtomicLong(0);
private volatile long heartTime=5*60*1000;
private volatile long id=-1;
public boolean connect()
{
	 workerGroup = new NioEventLoopGroup();
	 queue=new LinkedBlockingQueue<byte[]>();
	 boolean isSucess=false;
	 NettyTcpClient client=this;
    try {
         b = new Bootstrap(); // (1)
        b.group(workerGroup); // (2)
        b.channel(NioSocketChannel.class); // (3)
        b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new NettyTcpClientHandler(client));
                ch.pipeline().addLast(new IdleStateHandler(heartTime,0,0, TimeUnit.MILLISECONDS));
            }
        });
        
        // Start the client.
       // ChannelFuture f = b.connect(host, port).sync(); // (5)
         channelFutureListener= new ConnectionListener(client);
        //f.addListener(channelFutureListener);
       // channel = f.sync().channel();
        doConnnect();
        isSucess=true;
    } 
    catch(Exception ex)
    {
        isSucess=false;
    	ex.printStackTrace();
    }
    finally {
       // workerGroup.shutdownGracefully();
    }
    return isSucess;
}
public boolean doConnnect()
{
    ChannelFuture future = null;
    try {
        future = b.connect(host, port);
        future.addListener(channelFutureListener);
        channel = future.sync().channel();
        future.channel().writeAndFlush("sssss");
    } catch (Exception e) {
        e.printStackTrace();
    }
    return false;
}
public int sendData(byte[]data)
{
     if(data==null)
     {
         return -1;
     }
	//channel.writeAndFlush(data);
     //channel.writeAndFlush("fffff");
     ByteBuf buf=Unpooled.wrappedBuffer(data);
     channel.writeAndFlush(buf);
    return data.length;
}
 void addData(byte[]data)
{
    queue.add(data);
    size.incrementAndGet();
}
public byte[] recvice()
{
    byte[] result=null;
    try {
        if(this.recTimeOut==-1)
        {
           result= queue.take();
           size.decrementAndGet();
        }
        else
        {
            result= queue.poll(recTimeOut, TimeUnit.MILLISECONDS);
            if(result!=null)
            {
                size.decrementAndGet();
            }
        }
    } catch (InterruptedException e) {
      
        e.printStackTrace();
    }
    if(size.get()<0)
    {
        size.set(0);
    }
    return result;
}
public void close()
{
	channel.close();
	if(queue!=null)
    {
        queue.clear();
    }
	try {
		 channel.closeFuture().sync();
		 workerGroup.shutdownGracefully();
	} catch (InterruptedException e) {
		e.printStackTrace();
	}
}
public boolean isResetCon()
{
    return this.isResetCon;
}
@Override
public boolean connect(String host, int port) {
    this.host=host;
    this.port=port;
    return this.connect();
}
@Override
public int send(byte[] data) {
   return this.sendData(data);
}
@Override
public int sendUDP(String host, int port, byte[] data) {
    return 0;
}
@Override
public boolean isClose() {
    // boolean r= channel.isActive();
    return workerGroup.isShutdown();
}
@Override
public void resetConnect() {
    isResetCon=true;
}
@Override
public void setRecviceTimeOut(int time) {
    recTimeOut=time;
    if(this.recTimeOut<-1)
    {
        this.recTimeOut=-1;
    }
}
@Override
public long getSize() {
   
    return size.get();
}
@Override
public boolean isEmpty() {
    return queue.isEmpty();
}
@Override
public void setID(long id) {
  this.id=id;
    
}
@Override
public long getID() {
 return this.id;
}
@Override
public void setHeartTime(long time) {
   this.heartTime=time;
    
}
}

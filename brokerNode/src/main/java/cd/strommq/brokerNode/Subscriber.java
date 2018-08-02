/**
 * 
 */
package cd.strommq.brokerNode;

import java.util.List;
import java.util.Objects;

import cd.strommq.channel.NettyRspClient;
import io.netty.util.CharsetUtil;

/**
 * @author jinyu
 *
 */
public class Subscriber {
public String address="";
public int port;
//public Channel chanel;
public boolean isPush=false;
public boolean  isCopy=true;
public String GroupName;
public List<String> lstTopic=null;
public long id=0;
//private  InetSocketAddress client=null;
public NettyRspClient rspClient=null;
/**
 * 
 * @Title: setRsp   
 * @Description: 回执数据
 * @param data      
 * void      
 * @throws
 */
public void setRsp(byte[]data)
{
    //ByteBuf buf=Unpooled.copiedBuffer(data);
    rspClient.setRsp(data);
//    if(client!=null)
//    {
//        System.out.println("udp回执："+client.getHostName()+","+client.getPort());
//        chanel.writeAndFlush(new DatagramPacket(buf,client));
//    }
//    else
//    {
//        chanel.writeAndFlush(buf);
//    }
   // ReferenceCountUtil.release(buf);
}

/**
 * 
 * @Title: setRsp   
 * @Description: 回执数据  
 * @param msg      
 * void      
 * @throws
 */
public void setRsp(String msg)
{
   byte[] data=msg.getBytes(CharsetUtil.UTF_8);
   this.setRsp(data);

}

/**
 * 
 * @Title: setRsp   
 * @Description: 回执数据  
 * @param msg      
 * void      
 * @throws
 */
public void setRsp(byte[]flage,String msg)
{
   byte[] data=msg.getBytes(CharsetUtil.UTF_8);
   if(flage==null||flage.length==0)
   {
       this.setRsp(data);
   }
   else
   {
       byte[]tmp=new byte[flage.length+data.length];
       System.arraycopy(flage, 0, tmp, 0, flage.length);
       System.arraycopy(data, 0, tmp, flage.length, data.length);
       this.setRsp(tmp); 
   }

}


/**
 * 
 * @Title: setRsp   
 * @Description: 回执数据   
 * @param datas      
 * void      
 * @throws
 */
public void setRsp(List<byte[]> datas)
{
    if(datas==null||datas.isEmpty())
    {
        return;
    }
    int size=datas.size();
    for(int i=0;i<size;i++)
    {
        this.setRsp(datas.get(i));
    }
}

/**
 * 
 * @Title: setRsp   
 * @Description: 数据回执  
 * @param flage 添加标识
 * @param data  真实数据
 * void      
 * @throws
 */
public void setRsp(byte[]flage,List<byte[]> data)
{
    
    if(data==null||data.isEmpty())
    {
        return;
    }
    int size=data.size();
    for(int i=0;i<size;i++)
    {
        if(flage==null||flage.length==0)
        {
           this.setRsp(data.get(i));
        }
        else
        {
            byte[] cur=data.get(i);
            if(cur==null)
            {
                continue;
            }
            byte[]tmp=new byte[flage.length+cur.length];
            System.arraycopy(flage, 0, tmp, 0, flage.length);
            System.arraycopy(cur, 0, tmp, flage.length, cur.length);
            this.setRsp(tmp); 
        }
        
    }
}

public boolean equals(Object o) {
    if (this == o) return true;  //先判断o是否为本对象，如果是就肯定是同一对象了，this 指向当前的对象
    if (o == null || getClass() != o.getClass()) return false; //再判断o是否为null，和o.类对象和本类对象是否一致
    Subscriber sub = (Subscriber) o;  //再把o对象强制转化为Transport类对象
    return Objects.equals(address, sub.address) && port==sub.port;  //查看两个对象的name和type属性值是否相等,返回结果
}
}

package cd.strommq.mqnet;

import cd.strommq.channel.NettyRspClient;
import cd.strommq.udp.NettyUdpClient;
import cd.strommq.udp.NettyUdpServer;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
       // NettyTcpServer server=new NettyTcpServer();
       // server.start();
//        NettyUdpServer server=new NettyUdpServer();
//        Thread dd=new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                server.start(9999);
//            }
//        });
//        dd.setDaemon(true);
//        dd.start();
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//         
//            e.printStackTrace();
//        }
//        NettyRspClient rsp = server.recvice();
//        System.out.println(new String((byte[])rsp.getData()));;
//        while(true)
//        {
//            String msg="收到:"+System.currentTimeMillis();
//        
//            rsp.setRsp(msg.getBytes());
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//           
//                e.printStackTrace();
//            }
//        }
//       
        
        NettyUdpClient client=new NettyUdpClient();
    	client.connect("192.168.17.1", 9999);
        client.send("dfgg".getBytes());
    	while(true)
    	{
    	
    	  byte[] rec=client.recvice();
    	  String rsp=new String(rec);
    	  System.out.println(rsp);
    	  try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		
			e.printStackTrace();
		}
    	}
  }
}

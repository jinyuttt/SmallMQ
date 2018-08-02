package cd.strommq.Test;

import java.util.Date;

import cd.strommq.mqclient.MQPublisher;
import cd.strommq.mqclient.MQSubscriber;
import cd.strommq.mqclient.SubscriberModel;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
//        MQPublisher publisher=new MQPublisher();
//        while(true)
//        {
//        publisher.publish("Test1", new Date().toString().getBytes());
//        publisher.publish("Test2", new Date().toString().getBytes());
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//           
//            e.printStackTrace();
//        }
//        }
        MQSubscriber  subscriber=new MQSubscriber();
        subscriber.Subscriber("Test1", null, false, false);
        subscriber.Subscriber("Test2", "ss", true, false);
         while(true)
         {
            SubscriberModel model = subscriber.getData();
            StringBuffer buf=new StringBuffer();
            buf.append(model.brokerName);
            buf.append(":");
            buf.append(model.topicName);
            buf.append(",");
            buf.append(new String(model.data));
            System.out.println(buf.toString());
         }
        
    }
}

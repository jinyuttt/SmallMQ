/**
 * 
 */
package cd.strommq.brokerNode;

import java.nio.charset.Charset;
import java.util.List;

/**
 * @author jinyu
 * broker配置
 */
public class NodeConfig {
public static String PubNet="udp";
public static String  PubIP="*";
public static int  PubPort=40000;
public static String SubNet="udp";
public static String SubIP="*";
public static int SubPort=50000;
public static List<String> TopicServer=null;
public static String LocalName="name";
public static long heartLive=0;//判断本节点存活时间
public static long heartTime=60*1000;//心跳间隔
public static long topicTime=120*1000;//主题信息刷新时间
public static int topicNameMaxLen=500;//topic最大程度
public final static int maxQueueSize=10000;//队列个数,扩展队列
public final static long maxQueueLen=1024*1024*50;//50M，扩展队列
public final static long maxQueueNum=8;//500M，扩展队列
public final static int maxCacheSize=100000;//队列个数,主要以内存为准
public final static long maxCacheMem=1024*1024*500;//500M
public final static boolean cacheSave=true;
public final static long cacheCheckTime=30*1000;
public final static long cacheFileTime=24*60*60*1000;//1天
public  static boolean isudpRsp=false;//udp时回复数据包
/**
 * 编码格式
 */
public static Charset charset=Charset.forName("utf-8")==null?Charset.defaultCharset():Charset.forName("utf-8");

}

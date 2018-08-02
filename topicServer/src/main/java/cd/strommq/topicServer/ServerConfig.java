/**
 * 
 */
package cd.strommq.topicServer;

import java.nio.charset.Charset;

/**
 * @author jinyu
 *
 */
public class ServerConfig {
	
	/**
	 * 服务IP
	 */
public static String srvIP="*";

/**
 * 服务端口
 */
public static int srvPort=30000;

/**
 * 类型
 */
public static String srvNet="tcp";

/**
 * 更新指令的最短时间间隔
 * 10分钟
 */
public static long updateCommandTime=10*60*1000;

/**
 * 监测存活时间默认2min;broker默认2min发送一次心跳
 */
public static long checkLiveTime=2*60*1000;

/**
 * 
 */
public static int topicNameMaxLen=500;//topic最大程度
/**
 * 编码格式
 */
public static Charset charset=Charset.forName("utf-8")==null?Charset.defaultCharset():Charset.forName("utf-8");

}

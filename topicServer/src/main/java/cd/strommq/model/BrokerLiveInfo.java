/**
 * 
 */
package cd.strommq.model;

/**
 * @author jinyu
 * 心跳结构
 */
public class BrokerLiveInfo {
public String brokerVision="1.0.0";
private long updateTime=System.currentTimeMillis();
public void setTime()
{
	updateTime=System.currentTimeMillis();
}
public long getLastUpdate()
{
	return updateTime;
}
}

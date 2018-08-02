/**
 * 
 */
package cd.strommq.model;

import java.util.HashMap;
import java.util.List;

/**
 * @author jinyu
 *
 */
public class TopicData {
	public final HashMap<String/* topic */, List<QueueData>> topicQueueTable=new HashMap<String/* topic */, List<QueueData>>();//topic队列表，存储了每个topic包含的队列数据

	/**
	 * broker列表
	 */
	public final HashMap<String/* brokerName */, BrokerData> brokerAddrTable=new HashMap<String/* brokerName */, BrokerData>(); //broker地址表

	/**
	 * broker心跳列表
	 * 防止不同网络不同机器需要不同的时间设置
	 * 默认使用服务固定监测
	 */
	public final HashMap<String/* brokerName */, Long> brokerHeartTable=new HashMap<String/* brokerName */, Long>(); //broker地址表

	/**
	 * 考虑不要
	 */
//	private final HashMap<String/* clusterName */, Set<String/* brokerName */>> clusterAddrTable=new HashMap<String/* clusterName */, Set<String/* brokerName */>>(); //集群主备信息表


	//当前活跃的broker列表
	public final HashMap<String/* brokerName */, BrokerLiveInfo> brokerLiveTable=new HashMap<String/* brokerAddr */, BrokerLiveInfo>(); //broker存活状态信息表，
	   // 其中的BrokerLiveInfo存储了broker的版本号，channel，和最近心跳时间等信息

	//private final HashMap<String/* brokerAddr */, List<String>/* Filter Server */> filterServerTable=new HashMap<String/* brokerAddr */, List<String>/* Filter Server */>(); 
	//记录了每个broker的filter信息.

	//private final HashMap<String/* Namespace */, HashMap<String/* Key */, String/* Value */>> configTable=new HashMap<String/* Namespace */, HashMap<String/* Key */, String/* Value */>>();
	 //根据namespace配置区分的config表
	
	public final HashMap<String/* brokerName */, BrokerCommand> brokerCommondTable=new HashMap<String/* brokerAddr */, BrokerCommand>(); //broker存活状态信息表，

public void clear()
{
	this.brokerAddrTable.clear();
	this.brokerCommondTable.clear();
	this.brokerHeartTable.clear();
	this.brokerLiveTable.clear();
	this.topicQueueTable.clear();
}

}

/**
 * 
 */
package cd.strommq.model;

/**
 * @author jinyu
 *
 */
public class QueueData {
	public String brokerName;  // broker的名称
	
	public int readQueueNums;  // 读队列数量

	public int writeQueueNums; // 写队列数量

	public int topicSynFlag;   // 同步复制还是异步复制标记
}

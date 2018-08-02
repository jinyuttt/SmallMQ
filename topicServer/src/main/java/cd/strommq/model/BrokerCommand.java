/**
 * 
 */
package cd.strommq.model;

/**
 * @author jinyu
 *
 */
public class BrokerCommand {
public String topicName;
public Command cmd;
public long createTime=System.currentTimeMillis();
}

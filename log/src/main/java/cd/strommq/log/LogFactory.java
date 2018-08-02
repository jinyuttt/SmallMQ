/**    
 * 文件名：LogFactory.java    
 *    
 * 版本信息：    
 * 日期：2018年7月15日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.log;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**    
 *     
 * 项目名称：msgmq    
 * 类名称：LogFactory    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月15日 下午7:28:01    
 * 修改人：jinyu    
 * 修改时间：2018年7月15日 下午7:28:01    
 * 修改备注：    
 * @version     
 *     
 */
public class LogFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(LogFactory.class);
	private static LogFactory instance=null;
	public static String appPath="";
	public static String confPath="";
	public  static LogFactory getInstance()
	{
		if(instance==null)
		{
			instance=new LogFactory();
		}
		return instance;
	}
	public LogFactory()
	{
		//
		initLog();
	}
	
	/**
	 * 
	 * @Title: initLog   
	 * @Description: 初始化配置文件    
	 * void      
	 * @throws
	 */
	public void initLog()
	{
		 LoggerContext logContext = (LoggerContext) LogManager.getContext(false);
		 String conf=appPath+"/config/log4j2.xml";
	     File conFile = new File(conf);
	     if(conFile.exists())
	     {
	       logContext.setConfigLocation(conFile.toURI());
	       logContext.reconfigure();
	     }
	     else
	     {
	    	 conFile = new File(confPath);
		     if(conFile.exists())
		     {
		    	 if(conFile.isFile()&&conFile.exists())
		    	 {
		           logContext.setConfigLocation(conFile.toURI());
		           logContext.reconfigure();
		    	 }
		    	 else
		    	 {
		    		 conFile = new File(confPath+"/log4j2.xml");
		    		 if(conFile.exists())
		    		 {
		    			   logContext.setConfigLocation(conFile.toURI());
				           logContext.reconfigure();
		    		 }
		    	 }
		     }
	     }
	}
	public void addLog(LogLevel level,String msg)
	{
		switch(level)
		{
		case debug:
			this.addDebug(msg);
			break;
		case error:
			this.addError(msg);
			break;
		case info:
			this.addInfo(msg);
			break;
		case warn:
			this.addWarn(msg);
			break;
		case trace:
			this.addTrace(msg);
			break;
		default:
			break;
		   
		}
	}
	public void addDebug(String msg)
	{
		logger.debug(msg);
	}
	public void addError(String msg)
	{
		logger.error(msg);
	}
	public void addInfo(String msg)
	{
		logger.info(msg);
	}
	public void addWarn(String msg)
	{
		logger.warn(msg);
	}
	public void addTrace(String msg)
	{
		logger.trace(msg);
	}
}

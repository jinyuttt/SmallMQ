/**    
 * 文件名：FileBroker.java    
 *    
 * 版本信息：    
 * 日期：2018年7月31日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.topicServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**    
 *     
 * 项目名称：topicServer    
 * 类名称：FileBroker    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月31日 上午4:17:53    
 * 修改人：jinyu    
 * 修改时间：2018年7月31日 上午4:17:53    
 * 修改备注：    
 * @version     
 *     
 */
public class FileBroker {
    public String path="broker.txt";
    
    /**
     * 
     * @Title: write   
     * @Description: 
     * @param content    
     * void
     */
public void write(String content)
{
    try {
        PrintWriter out = new PrintWriter(new FileWriter(path,true));
        out.println(content);
        out.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
}
public String[] read()
{
    Scanner in= null; 
    List<String> lst=new ArrayList<String>();
    String[]  lines=null;
    try {   
        try {
            in = new Scanner(new BufferedReader(new FileReader(path)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }   
        //使用字符串jdk作为分隔符   
        //s.useDelimiter("jdk");   
        while (in.hasNextLine()) {
            String line=in.nextLine();
            if(!lst.contains(line))
            {
               lst.add(line);
            }
        }   
    } finally {   
        if (in!= null) {   
            in.close();   
        }   
    }
    lines=new String[lst.size()];
    lst.toArray(lines);
    return lines;   
}

public void delete()
{
    File f=new File(path);
    f.deleteOnExit();
}
}

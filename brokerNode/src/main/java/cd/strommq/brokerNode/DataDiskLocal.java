/**    
 * 文件名：DataDiskLocal.java    
 *    
 * 版本信息：    
 * 日期：2018年7月29日    
 * Copyright 足下 Corporation 2018     
 * 版权所有    
 *    
 */
package cd.strommq.brokerNode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.msgpack.MessagePack;
import org.msgpack.template.Templates;

/**    
 *     
 * 项目名称：brokerNode    
 * 类名称：DataDiskLocal    
 * 类描述：    
 * 创建人：jinyu    
 * 创建时间：2018年7月29日 上午2:50:07    
 * 修改人：jinyu    
 * 修改时间：2018年7月29日 上午2:50:07    
 * 修改备注：    
 * @version     
 *     
 */
public class DataDiskLocal {
    private MessagePack msgpack = new MessagePack();
    public String path="cache";
    private HashMap<String,String> map=new HashMap<String,String>();
    private HashMap<String,String> mapPath=new HashMap<String,String>();
    private boolean isInit=false;
   public void add(String topic,List<byte[]> data)
   {
       String topicPath=mapPath.getOrDefault(topic, null);
       if(topicPath==null)
       {
           topicPath=checkDir(topicPath);
           mapPath.put(topic,topicPath);
       }
       try {
        byte[] raw =msgpack.write(data);
        long time=System.currentTimeMillis();
        map.put(topic, String.valueOf(time));
        String file=topicPath+"//"+time+".dat";
        writeFile(file,raw);
    } catch (IOException e) {
        e.printStackTrace();
    }
   }
   public List<byte[]> getData(String topic)
   {
       String dirPath=mapPath.getOrDefault(topic, null);
       if(dirPath==null)
       {
           return null;
       }
       else
       {
           File dir=new File(dirPath);
           String[] files=dir.list();
           if(files!=null&&files.length>0)
           {
               Arrays.sort(files);
              byte[] raw=readFile(files[0]);
              File f=new File(files[0]);
              f.delete();
              if(raw!=null)
              {
                  try {
                    List<byte[]> lst= msgpack.read(raw, Templates.tList(Templates.TByteArray));
                    return lst;
                  } catch (IOException e) {
                    e.printStackTrace();
                }
              }
           }
       }
    return null;
   }
   public void deleteFile(String topic,long timeLen)
   {
       String topicPath=mapPath.getOrDefault(topic, null);
       if(topicPath!=null)
       {
           File dir=new File(topicPath);
           File[] fs = dir.listFiles();
           if(fs!=null)
           {
               long curTime=System.currentTimeMillis();
               for(File f:fs)
               {
                  String name= f.getName().replaceAll(".dat", "");
                  long fileTime=Long.valueOf(name);
                  if(curTime>fileTime+timeLen)
                  {
                      f.delete();
                  }
               }
           }
       }
   }
   private byte[] readFile(String file)
   {
       try
       {
           FileInputStream fis = new FileInputStream("D:\\tengyicheng\\timg.jpg");
           DataInputStream dis = new DataInputStream(fis);
           byte[]raw=   dis.readAllBytes();
           dis.close();
           fis.close();
           return raw;
       }
       catch(Exception ex)
       {
           ex.printStackTrace();
       }
    return null;
   }
   private void writeFile(String file,byte[]data)
   {
       try
       {
       //创建输出流
        FileOutputStream fos = new FileOutputStream("D:\\tengyicheng\\myFile\\timg.jpg");
        DataOutputStream dos = new DataOutputStream(fos);
        dos.write(data);
        dos.close();
        fos.close();
       }
       catch(Exception ex)
       {
           ex.printStackTrace();
       }
   }
   private void init()
   {
      if(!isInit)
       {
      File dir=new File(path);
      if(!dir.exists())
      {
          dir.mkdir();
      }
         isInit=true;
       }
   }
   private String checkDir(String dirPath)
   {
       init();
       File dir=new File(dirPath);
       if(!dir.exists())
       {
           dir.mkdir();
       }
      return dir.getAbsolutePath();
     
   }
}

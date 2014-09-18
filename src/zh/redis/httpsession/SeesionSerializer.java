package zh.redis.httpsession;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import zh.redis.RedisClient;
import zh.redis.RedisSimpleTempalte;

public class SeesionSerializer {
	
	public static byte[] serialize(Serializable session){
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    ObjectOutputStream oos = null;
	    try{
	    	oos = new ObjectOutputStream(new BufferedOutputStream(bos));
		    oos.writeObject(session);	
		    oos.close();
		    return bos.toByteArray();
	    }
	    catch (IOException e){
	    	e.printStackTrace();
	    }
	    finally{
	    	if(oos != null){	    		
	    		try {
	    			bos.close();
					oos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
	    }
	    return null;
	}
	
	public static RedisHttpSession deserialize(byte[] data){
	    BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
	    ObjectInputStream ois = null;
	    RedisHttpSession session = null;
		try {
			ois = new ObjectInputStream(bis);
			session = (RedisHttpSession)ois.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		finally{
			if(ois != null){	    		
	    		try {
	    			bis.close();
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
		}
	    return session;
	}
	
	public static void main(String[] args) throws Exception{
		RedisClient rc = new RedisClient("112.124.6.82",6379);
		RedisSimpleTempalte rs = new RedisSimpleTempalte(rc);
		RedisHttpSession session = new RedisHttpSession();
		session.setAttribute("name", "zhanghua");
		byte[] bytes = serialize(session);
		rs.set("tests", serialize(session), 100);
		bytes = rs.getByte("tests");
		session = deserialize(bytes);
		System.out.println(session.getAttribute("name"));
	}
}

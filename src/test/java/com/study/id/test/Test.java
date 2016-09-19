package com.study.id.test;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.CloseableUtils;

import com.study.id.lonely.LonelyId;
import com.study.id.lonely.LonelyIdFactory;

public class Test {
	static Map<Long,Integer> ids = new ConcurrentHashMap<Long, Integer>(100);
	
	public static void main(String[] args) throws Exception {
		
		
		//final TestingServer server = new TestingServer();//测试
		
		String zookeeperList = "127.0.0.1:2181";//server.getConnectString()
		
		final LonelyIdFactory lonelyIdFactory = LonelyIdFactory.getInstance();
		lonelyIdFactory.init("",zookeeperList);
		final LonelyId lonelyId = lonelyIdFactory.getLonelyId();

		while(true) {  
			try{
				putId(lonelyId.nextId());
				Thread.sleep(100);
			}catch(Exception e){
				e.printStackTrace();
			}
			
        } 
		
	}
	
	public static void putId(long id){
		if(ids.containsKey(id)){
			throw new RuntimeException("重复id:"+ id);
		}else{
			ids.put(id, 0);
		}
	}

}

package com.study.id.lonely;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.test.KillSession;
import org.apache.curator.utils.CloseableUtils;
import org.apache.log4j.Logger;

import com.study.id.zookeeper.ZookeeperClient;


public class LonelyIdFactory {
	private static final Logger log = Logger.getLogger(LonelyIdFactory.class);
	
	private AtomicBoolean inited = new AtomicBoolean(false);
	 
	private ZookeeperClient zookeeperClient;
	
	private LonelyId lonelyId;
	
	private String zookeeperList;
	
	//单例
	private static LonelyIdFactory lonelyIdFactory = new LonelyIdFactory();
    private LonelyIdFactory(){};
    public static LonelyIdFactory getInstance(){
        return lonelyIdFactory;
    }
	
    
    
    public void init(String appName,String zookeeperList) throws Exception{
    	
    	init(appName, zookeeperList, 0);
    }
    
    public void init(String appName,String zookeeperList,int freeId) throws Exception{
    	if(inited.compareAndSet(false, true)){
    		if(StringUtils.isBlank(zookeeperList)){
        		throw new IllegalArgumentException("zookeeperList 参数无效");
        	}
        	this.zookeeperList = zookeeperList.trim();
        	
         	zookeeperClient = new ZookeeperClient(appName);
        	zookeeperClient.init(this.zookeeperList).registLonelyIdNode();
        	int workerId = zookeeperClient.getWorkerId();
         
        	lonelyId = new LonelyId(workerId, freeId);
        	
        	Runtime.getRuntime().addShutdownHook(new Thread(){
    			@Override
    			public void run() {
    				getZookeeperClient().deleteLonelyIdNode();

    				try {
    					KillSession.kill(getZookeeperClient().getClient().getZookeeperClient().getZooKeeper(), getZookeeperList());
    				} catch (Exception e) {
    					log.error("kill zookeeperClient session error", e);
    				}
    				CloseableUtils.closeQuietly(getZookeeperClient().getClient());
    			}
    		});
    	}
    }
    
    public LonelyId getLonelyId(){
    	return lonelyId;
    }
    
    public ZookeeperClient getZookeeperClient(){
    	return zookeeperClient;
    }
    
    public String getZookeeperList(){
    	return zookeeperList;
    }
    
    public void setLonelyIdWorkerId(int workerId){
    	getLonelyId().setWorkerId(workerId);
    }
    
}

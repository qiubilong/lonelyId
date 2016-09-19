package com.study.id.zookeeper;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.CloseableUtils;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;

import com.study.id.lonely.LonelyIdFactory;

//单例，线性安全
public class ZookeeperClient {
	private static final Logger log = Logger.getLogger(ZookeeperClient.class);
	
	private  String path = "/LonelyId/long64/#/id/workers";

 	CuratorFramework client = null;
 	
 	public final static int INVALID_NODEID = -1;
 	private int nodeId = INVALID_NODEID;
 	private String  nodePath;
 	private String appName = "unique";
 	
	
	public ZookeeperClient(String appName) {
		if(!StringUtils.isBlank(appName)){
			this.appName = appName.trim();
		}
		this.path = this.path.replace("#", this.appName);
	}

	public ZookeeperClient init(String zookeeperList) throws Exception{
		ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);//重试机制
		
		client = CuratorFrameworkFactory.builder().connectString(zookeeperList)
        .retryPolicy(retryPolicy)
        .connectionTimeoutMs(15 * 1000)
        .sessionTimeoutMs(60 * 1000)
        .build();
		
		client.getConnectionStateListenable().addListener(new ConnectionStateListener() {

             @Override
             public void stateChanged(CuratorFramework client, ConnectionState connectionState) {//连接状态监听
            	 log.info("received zookeeper client state:" + connectionState.name());
                 
                 if (connectionState == ConnectionState.LOST) {
                	 
                	 resetWorkerId(INVALID_NODEID);
                	 
                	 while (true) {
	                	 try {
		                	 if (client.getZookeeperClient().blockUntilConnectedOrTimedOut()) {//重新连上
		                		 resetWorkerId(registLonelyIdNode());
			                	 break;
		                	 }
	                	 } catch (Exception e) {
	                		 break;
	                	 }

                	 }
                 }
             
            }
         });
		
		client.start();//启动
		
		return this;
		
	}
	
	public int registLonelyIdNode(){
		int id = -1;
		try {
			nodePath = client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(path, new byte[0]);
			setMachineInfo(client,nodePath);
			log.info("create LonelyId node path:" + nodePath);
			id = parseLonelyIdNodeId(nodePath);
 		} catch (Exception e) {
			log.error("registLonelyIdNode error", e);
		}
		this.nodeId = id;
		
		return this.nodeId;
	}
	
	public void deleteLonelyIdNode(){
		if(nodePath != null){
			log.info("delete node path:" + nodePath);
			try {
				resetWorkerId(INVALID_NODEID);
				client.delete().guaranteed().forPath(nodePath);
			} catch (Exception e) {
				log.error("deleteLonelyIdNode error", e);
			}
		}
	}
	
	
	public int getWorkerId(){
		 
		return this.nodeId;
	}
	
	public void resetWorkerId(int id){
		log.info("reset  LonelyId's workerId:" + id);
		this.nodeId = id;
		LonelyIdFactory.getInstance().setLonelyIdWorkerId(this.nodeId);
	}
	
	public  int  parseLonelyIdNodeId(String nodeName){
		int id = -1;
		nodeName = nodeName.replace(path, "");
		id = Integer.parseInt(nodeName);
		this.nodeId = id;
		
		return id;
	}
	
	//设置机器信息
	public static void setMachineInfo(CuratorFramework client, String path) throws Exception {
         CuratorListener listener = new CuratorListener() {
            @Override
            public void eventReceived(CuratorFramework client, CuratorEvent event) throws Exception {
                // examine event for details
            }
        };
        client.getCuratorListenable().addListener(listener);
        byte[] address = InetAddress.getLocalHost().getHostAddress().getBytes();
        client.setData().inBackground().forPath(path, address);
    }
	
	public CuratorFramework getClient(){
	     return client;
	}
	
	public boolean isConnected(){
		return client.getZookeeperClient().isConnected();
	}
	
	public static void main(String[] args) throws Exception {
		TestingServer server = new TestingServer();//测试
		
		final ZookeeperClient zookeeperClient = new ZookeeperClient("");
		zookeeperClient.init(server.getConnectString());
		
		List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>();
         for (int i = 0; i < 100; i++) {
             tasks.add(new Callable<Integer>() {
                 @Override
                 public Integer call() throws Exception {
                	 
                    return zookeeperClient.getWorkerId();
                 }
             });
         }
		ExecutorService executorPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		executorPool.invokeAll(tasks);
		
		CloseableUtils.closeQuietly(zookeeperClient.client);
	    CloseableUtils.closeQuietly(server);
	}
}

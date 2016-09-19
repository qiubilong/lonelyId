package com.study.id.lonely;

import java.util.Random;

import org.apache.log4j.Logger;

/**
 *维持单例，只能通过LonelyIdFactory创建
 */
public class LonelyId {
	private static final Logger log = Logger.getLogger(LonelyId.class);

	private final static long EPOCH = 1451577600000L;//Thu, 04 Nov 2010 01:42:54 GMT,使用开始日期
	                                  
	private long workerId;//节点编号
	private final static long workerIdBits = 9L;//节点编号占用位数
 	private final static long maxWorkerId = -1L ^ (-1L << (int) workerIdBits);//最大节点编号512
 	
	private long sequence = 0L;//毫秒内自增值
 	private final static long sequenceBits = 8L;//自增值占用位数
 	private final static long sequenceMask = -1L ^ (-1L << (int) sequenceBits);//自增最大值256
 	
 	
 	private long freeId;//预留位
 	private final static long freeIdBits = 5L;//预留位
 	private final static long maxFreeId = -1L ^ (-1L << (int) freeIdBits);//预留位最大值32

 	private final static long freeIdShift = sequenceBits;//左移8位
	private final static long workerIdShift = sequenceBits +  freeIdBits;//左移13位
 	private final static long timestampLeftShift = sequenceBits +  freeIdBits + workerIdBits;//左移22位
	
	private long lastTimestamp = -1L;
	private static Random random = new Random();
	private static byte[] nextIdLock = new byte[0];
	  
	
	protected LonelyId(long workerId,long freeId){
		log.info("init LonelyId instance workerId:" + workerId + ",freeId:" + freeId);

		checkWorkedId(workerId);
		
		if (freeId > maxFreeId || freeId < 0) {
			throw new IllegalArgumentException(String.format("freeId  can't be greater than %d or less than 0",maxFreeId));
		}
		 
		this.workerId = workerId;
		this.freeId = freeId;
		
	}
	
	public  long nextId()  {
		boolean isConnected = LonelyIdFactory.getInstance().getZookeeperClient().isConnected();
		if(!isConnected){
			throw new RuntimeException("zookeeperClient lost connect, please retry later");
		}
		
		checkWorkedId(workerId);
		
		synchronized (nextIdLock) {
			long timestamp = timeGen();

			if (timestamp < lastTimestamp) {
				throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
			}

			
			if(lastTimestamp == timestamp){
				sequence = (sequence + 1) & sequenceMask;
				if (sequence == 0) {
					timestamp = tilNextMillis(lastTimestamp);
				}
			}else{
				sequence = random.nextInt(10);
			}
			lastTimestamp = timestamp;
			
			StringBuilder sb = new StringBuilder(100);
			sb.append(timestamp - EPOCH).append("-").append(workerId).append("-").append(freeId).append("-").append(sequence);
			
			long id = ((timestamp - EPOCH) << (int) timestampLeftShift) | (workerId << (int) workerIdShift) | (freeId << (int) freeIdShift) |sequence;
			
			if(log.isDebugEnabled()){
				log.debug( "encode-->" +sb.toString()  +" id-->"+ id + " decode-->"+ LonelyId.uncodeLonelyId(id));

			}
			
			return id;
		}
	
	}
	
	public synchronized void setWorkerId(long workerId){
		this.workerId = workerId;
	}
	
	protected static void checkWorkedId(long workerId){
		if (workerId > maxWorkerId || workerId < 0) {// sanity check for workerId
			throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0",maxWorkerId));
		}
	}

	//等待到下一毫秒
	protected long tilNextMillis(long lastTimestamp) {
		long timestamp = timeGen();
		while (timestamp <= lastTimestamp) {
			timestamp = timeGen();
		}
		return timestamp;
	}

	// 毫秒
	protected long timeGen() {
		return System.currentTimeMillis();
	}
	
	//id解码
	public static String uncodeLonelyId(long id){
		if(id <= 0){
			throw new IllegalArgumentException(String.format("id %d is not greater than  0",id));
		}
		StringBuilder sb = new StringBuilder(100);
		long time = id >> (int) timestampLeftShift;
		
		long workerId = id >> (int) workerIdShift;
		workerId = workerId << (64- (int) workerIdBits);
		workerId = workerId >>> (64- (int) workerIdBits);
		
		long freeId = id >> (int) freeIdShift;
		freeId = freeId << (64- (int) freeIdBits);
		freeId = freeId >>> (64- (int) freeIdBits);
		
		long sequence = id << (64 - (int) sequenceBits);
		sequence = sequence >>> (64 - (int) sequenceBits);
		
		sb.append(time).append("-").append(workerId).append("-").append(freeId).append("-").append(sequence);
		return sb.toString();
	}
	
	public static long GetTimeOfLonelyId(long id) {
		if(id <= 0){
			throw new IllegalArgumentException(String.format("id %d is not greater than  0",id));
		}
		long time = id >> (int) timestampLeftShift;
		time += EPOCH;
		return time;
	}
	
}

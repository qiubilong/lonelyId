package com.study.id.demo.twitter;

public class IdWorker {
	private long workerId;//节点id
	private long datacenterId;//数据中心id
	private long sequence = 0L;//毫秒内自增id

	private static long twepoch = 1288834974657L;//Thu, 04 Nov 2010 01:42:54 GMT,使用开始日期

	private static long workerIdBits = 5L;
	private static long datacenterIdBits = 5L;
	private static long maxWorkerId = -1L ^ (-1L << (int) workerIdBits);//32
	private static long maxDatacenterId = -1L ^ (-1L << (int) datacenterIdBits);//32
	private static long sequenceBits = 12L;

	private long workerIdShift = sequenceBits;//12
	private long datacenterIdShift = sequenceBits + workerIdBits;//17
	private long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;//22
	private long sequenceMask = -1L ^ (-1L << (int) sequenceBits);//4096

	private long lastTimestamp = -1L;
	private static Object syncRoot = new Object();

	public IdWorker(long workerId, long datacenterId){

		// sanity check for workerId
		if (workerId > maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0",maxWorkerId));
		}
		if (datacenterId > maxDatacenterId || datacenterId < 0) {
			throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0",maxDatacenterId));
		}
		this.workerId = workerId;
		this.datacenterId = datacenterId;
	}

	public long nextId()  {
		synchronized (syncRoot) {
			long timestamp = timeGen();

			if (timestamp < lastTimestamp) {
				throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
			}

			if (lastTimestamp == timestamp) {
				sequence = (sequence + 1) & sequenceMask;
				if (sequence == 0) {
					timestamp = tilNextMillis(lastTimestamp);
				}
			} else {
				sequence = 0L;
			}

			lastTimestamp = timestamp;

			// 最后按照规则拼出ID。
	        // 000000000000000000000000000000000000000000  00000            00000       000000000000
		    // time                                        datacenterId   workerId    sequence
			return ((timestamp - twepoch) << (int) timestampLeftShift)
					| (datacenterId << (int) datacenterIdShift)
					| (workerId << (int) workerIdShift) | sequence;
		}
	}

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
}
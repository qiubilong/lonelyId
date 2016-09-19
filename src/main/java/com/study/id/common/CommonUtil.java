package com.study.id.common;

import org.joda.time.DateTime;

import com.study.id.lonely.LonelyId;


public class CommonUtil {

	//还原id的生成时间
	public static String getMsTimeOfLonelyId(long id) {
		long time  = LonelyId.GetTimeOfLonelyId(id);
		DateTime dateTime = new DateTime(time);
		String formatter = "yyyy-MM-dd HH:mm:ss:SSS";
		return dateTime.toString(formatter);
	}
	// time时间戳-节点标识-预留位-毫秒内自增
	public static String uncodeLonelyId(long id){
		return LonelyId.uncodeLonelyId(id);
	}
	
	
	public static void main(String[] args) {
		 long id = LonelyId.GetTimeOfLonelyId(90112193793622016L);
		 System.out.println(getMsTimeOfLonelyId(id));
	}
}

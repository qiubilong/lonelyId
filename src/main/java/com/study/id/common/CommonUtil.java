package com.study.id.common;

import org.joda.time.DateTime;

import com.study.id.lonely.LonelyId;


public class CommonUtil {

	//还原id的生成时间
	public static String formatForMSTime(long time) {
		DateTime dateTime = new DateTime(time);
		String formatter = "yyyy-MM-dd HH:mm:ss:SSS";
		return dateTime.toString(formatter);
	}
	
	
	public static void main(String[] args) {
		 long id = LonelyId.GetTimeOfLonelyId(90112193793622016L);
		 System.out.println(formatForMSTime(id));
	}
}

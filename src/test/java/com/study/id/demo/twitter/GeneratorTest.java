package com.study.id.demo.twitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class GeneratorTest {

    @Test
    public void testIdGenerator() {
        long avg = 0;
        final IdWorker idGen = new IdWorker(0, 0);
        
        for (int k = 0; k < 10; k++) {
            List<Callable<Long>> partitions = new ArrayList<Callable<Long>>();
             
            for (int i = 0; i < 1000000; i++) {
                partitions.add(new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                    	long id = idGen.nextId();
                    	System.out.println(id);
                        return id;
                    }
                });
            }
            ExecutorService executorPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            try {
                long s = System.currentTimeMillis();
                executorPool.invokeAll(partitions, 10000, TimeUnit.SECONDS);
                long s_avg = System.currentTimeMillis() - s;
                avg += s_avg;
                System.out.println("���ʱ����Ҫ: " + s_avg / 1.0e3 + "��");
                executorPool.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("ƽ�����ʱ����Ҫ: " + avg / 10 / 1.0e3 + "��");
    }
}
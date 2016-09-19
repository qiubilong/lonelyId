
# lonelyId  ——简单高效的分布式的唯一的long型id生成器

##简介

Long型id只占64位，可以更加有效存储和计算，对作为主键索引更有效，节省计算机的资源。

使用zookeeper分配进程标识id后,不需要远程调用，可以直接在本地生成id， 没有存在单点故障，时延高的问题。

而且生成的id满足唯一性、时间相关、粗略有序、可反解、可制造特性。id由时间戳+进程标识+自增序列组成，设计如下:


| 42bit      |  9bit     | 5bit   |   8bit   |
|:----------:|:---------:|:------:|:--------:|
| time时间   | 节点标识  | 预留位 | 自增序列 |



* 时间戳：42位，精确到毫秒数。从20160101算起，可以支撑140年。

* 进程标识：9位，由zookeeper统一分配，进程结束后回收id,最大分配512个节点。

* 预留位 ：可以作为机房、业务的标识位，或者用户的分区标识，用于分库分表

* 自增序列 ：毫秒内的自增，最大值255，每节点每秒可以生成25w个id



##使用示例
    
```java
        String zookeeperList = "127.0.0.1:2181";
		
		final LonelyIdFactory lonelyIdFactory = LonelyIdFactory.getInstance();
		lonelyIdFactory.init("appName",zookeeperList);
		final LonelyId lonelyId = lonelyIdFactory.getLonelyId();//单例

		while(true) {  
			try{
			
				lonelyId.nextId();
				
				Thread.sleep(100);
			}catch(Exception e){
				e.printStackTrace();
			}
			
        }
		
```

```java
		
		//解析id信息
		long id = 95214749788143622L;//输出的id
		
		System.out.println(CommonUtil.uncodeLonelyId(id));//id反解，time时间戳-节点标识-预留位-毫秒内自增
		System.out.println(CommonUtil.getMsTimeOfLonelyId(id));//2016-09-19 17:49:25:354
		
```

##有问题反馈
在使用中有任何问题，欢迎反馈给我，可以用以下联系方式跟我交流

邮件(xueguichen#qq.com, 把#换成@)

QQ: 435337413
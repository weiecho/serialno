/**
* Organization: wei-echo <br>
* Date: 2019-03-25 20:19:20 <br>
* Automatically Generate By EasyCodeGenerine <br>
* Copyright (c) 2019 All Rights Reserved.
*/
package cn.echo.serialno.core;

import cn.echo.serialno.conf.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**************************
 * SerialnoCache
  *  编号池服务
 * @author lonyee
 *
 **************************/
public class SerialnoCache {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private RedisTemplate<String, Number> redisTemplate;
	//前缀
	private final static String preSerialNoKey = "SERIALNO:";

	public SerialnoCache(RedisTemplate<String, Number> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	/**
	 * 根据标识获取编号
	 */
	public Long getSerialno(SerialnoEnumerable serialnoEnum) {
		BoundListOperations<String, Number> operations = redisTemplate.boundListOps(preSerialNoKey + serialnoEnum.getBizTag());
		Number number = operations.leftPop();
		return number==null? 0: number.longValue();
	}
	
	/**
	 * 根据标识分配号池，返回起始号码
	 */
	public Long allocSerialnoByBizTag(SerialnoEnumerable serialnoEnum) {
		Long serialnoCount = redisTemplate.boundListOps(preSerialNoKey + serialnoEnum.getBizTag()).size();
		if (serialnoCount > serialnoEnum.getStep()) {
			log.info("编号池[{}]数据量[{}]已经远大于step[{}]", serialnoEnum.getBizTag(), serialnoCount, serialnoEnum.getStep());
			return -1L;
		}
		BoundHashOperations<String, String, Number> boundHashOperations = redisTemplate.boundHashOps(preSerialNoKey+"MAT");
		Number currMaxNo = boundHashOperations.get(serialnoEnum.getBizTag());

		long initNum = serialnoEnum.getInitNum();
		//读取配置内初始号
		if (serialnoEnum!=null && Config.SerialInitMap.containsKey(serialnoEnum.getBizTag())) {
			initNum = Config.SerialInitMap.get(serialnoEnum.getBizTag());
		}

		//开始或重新设置了起始位置时调整位置
		if (currMaxNo==null || currMaxNo.intValue() <= initNum) {
			boundHashOperations.put(serialnoEnum.getBizTag(), initNum + serialnoEnum.getStep());
			return initNum;
		}

		boundHashOperations.increment(serialnoEnum.getBizTag(), serialnoEnum.getStep());
		return currMaxNo.longValue();
	}
	
	/**
	 *  批量添加号池
	 */
	public void addSerialnos(SerialnoEnumerable serialnoEnum, List<Long> serialNos) {
		BoundListOperations<String, Number> operations = redisTemplate.boundListOps(preSerialNoKey + serialnoEnum.getBizTag());
		operations.rightPushAll(serialNos.toArray(new Number[0]));
	}
}

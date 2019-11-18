package cn.echo.serialno.core;

/**
 * 序列号池静态枚举基础接口 （业务enum继承实现）
 * @author lonyee
 */
public interface SerialnoEnumerable {

	/** 业务标识 **/
	String getBizTag();

	/** 初始起始值 **/
	Long getInitNum();

	/** 每次缓存号段长度 **/
	Long getStep();

	/** 生成号池策略  默认 顺序策略 1-9 **/
	SerialnoStrategy getStrategy();

	/** 是否排除幸运号 **/
	Boolean getExcludeLuckNum();

}

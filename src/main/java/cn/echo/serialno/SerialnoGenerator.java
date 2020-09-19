package cn.echo.serialno;

import cn.echo.serialno.core.SerialnoCache;
import cn.echo.serialno.core.SerialnoEnumerable;
import cn.echo.serialno.core.SerialnoHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 序列号生成器
 * @author lonyee
 */
public class SerialnoGenerator {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private SerialnoCache serialnoCache;

    private SerialnoHandle serialnoHandle;

    private List<SerialnoEnumerable> serialnoEnums;

    public SerialnoGenerator(RedisTemplate<String, Number> redisTemplate, List<SerialnoEnumerable> serialnoEnums) {
        this.serialnoCache = new SerialnoCache(redisTemplate);
        this.serialnoHandle = new SerialnoHandle(serialnoCache);
        this.serialnoEnums = serialnoEnums;
    }

    /**
     * 根据标识获取编号
     */
    public Long getSerialno(SerialnoEnumerable serialnoEnum) {
        return serialnoCache.getSerialno(serialnoEnum);
    }


    /**
     * 根据标识获取编号
     */
    public Long getSerialno(SerialnoEnumerable serialnoEnum, Integer prefix, Integer length) {
        return this.getSerialno(serialnoEnum, prefix, length, 0);
    }
    
    /**
     * 根据标识获取编号
     */
    public Long getSerialno(SerialnoEnumerable serialnoEnum, Integer randomLength) {
        return this.getSerialno(serialnoEnum, 0, 0, randomLength);
    }

    /**
     * 根据标识获取编号
     */
    public Long getSerialno(SerialnoEnumerable serialnoEnum, Integer prefix, Integer length, Integer randomLength) {
        int len = 0;
        long pow = 0;
        long pno = 0;
        if (prefix!=null && prefix>0) {
            len = (int) Math.log10(prefix) +1;
            if (len + randomLength >= length) {
                return 0L;
            }
            pow = (long) Math.pow(10, length - len);
            pno = prefix * pow;
        }
        long sno = serialnoCache.getSerialno(serialnoEnum);
        long suffix = 0;
        if (randomLength!=null && randomLength>0) {
            pow = (long) Math.pow(10, randomLength);
            suffix = ThreadLocalRandom.current().nextLong(pow);
            sno = sno * pow;
        }
        return pno + sno + suffix;
    }

    /**
     * 根据标识获取编号
     */
    public String getSerialno(SerialnoEnumerable serialnoEnum, String prefix, Integer length) {
        return this.getSerialno(serialnoEnum, prefix, length, 0);
    }

    /**
     * 根据标识获取编号
     */
    public String getSerialno(SerialnoEnumerable serialnoEnum, String prefix, Integer length, Integer randomLength) {
        if (prefix==null || length<=prefix.length()) {
            return "0";
        }

        long sno = serialnoCache.getSerialno(serialnoEnum);
        long suffix = 0;
        if (randomLength!=null && randomLength>0) {
            long pow = (long) Math.pow(10, randomLength);
            suffix = ThreadLocalRandom.current().nextLong(pow);
            sno = sno * pow;
        }
        String format = "%0"+ (length - prefix.length() - randomLength) +"d";
        return prefix + String.format(format, sno + suffix);
    }

    /**
     * 生成编号池
     * */
    public void generateSerialnos() {
        log.info("开始生成编号池 ... ");
        for (SerialnoEnumerable serialnoEnum: serialnoEnums) {
            serialnoHandle.generateSerialNo(serialnoEnum);
        }
        log.info("编号池生成完成. ");
    }

    /**
     * 自动生成编号池
     */
    @PostConstruct
    private void scheduledGenerateSerialnos() {
        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                Thread thread = new Thread(r, "schedule-serialno");
                thread.setDaemon(true);
                return thread;
            }
        }).scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                generateSerialnos();
            }
        }, 10*1000L, 10*60*1000L, TimeUnit.MILLISECONDS);
    }

}

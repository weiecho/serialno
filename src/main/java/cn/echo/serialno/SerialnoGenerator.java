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
     * 生成编号池
     * */
    public void generateSerialnos() {
        log.info("start generate serialno ... ");
        for (SerialnoEnumerable serialnoEnum: serialnoEnums) {
            serialnoHandle.generateSerialNo(serialnoEnum);
        }
    }

    /**
     * 自动生成编号池
     */
    @PostConstruct
    private void scheduledGenerateSerialnos() {
        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                Thread thread = new Thread(r, "schedule-serialno-generator");
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

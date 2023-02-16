package cn.echo.serialno;

import cn.echo.serialno.conf.Config;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 序列号生成器
 *
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
        int len = 0;
        long pow = 0;
        long pno = 0;
        if (!serialnoEnum.getPrefix().isEmpty() && this.isNumber(serialnoEnum.getPrefix())) {
            int prefix = Integer.parseInt(serialnoEnum.getPrefix());
            len = (int) Math.log10(prefix) + 1;
            if (len + serialnoEnum.getRandomLength() >= serialnoEnum.getLength()) {
                return 0L;
            }
            pow = (long) Math.pow(10, serialnoEnum.getLength() - len);
            pno = prefix * pow;
        }

        long sno = serialnoCache.getSerialno(serialnoEnum) + (10000000L * Config.envCode);
        long suffix = 0;
        if (serialnoEnum.getRandomLength() != null && serialnoEnum.getRandomLength() > 0) {
            pow = (long) Math.pow(10, serialnoEnum.getRandomLength());
            suffix = ThreadLocalRandom.current().nextLong(pow);
            sno = sno * pow;
        }
        return pno + sno + suffix;
    }


    /**
     * 通过正则表达式判断字符串是否为数字
     */
    private boolean isNumber(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher m = pattern.matcher(str);
        // 如果正则匹配通过 m.matches() 方法返回 true ，反之 false
        return m.matches();
    }


    /**
     * 根据标识获取编号
     */
    public String getSerialnoWithChar(SerialnoEnumerable serialnoEnum) {
        if (serialnoEnum.getPrefix() == null || serialnoEnum.getLength() <= serialnoEnum.getPrefix().length()) {
            return "0";
        }

        //根据当前环境Code配置+初始号
        long sno = serialnoCache.getSerialno(serialnoEnum) + (10000000L * Config.envCode);
        long suffix = 0;
        if (serialnoEnum.getRandomLength() != null && serialnoEnum.getRandomLength() > 0) {
            long pow = (long) Math.pow(10, serialnoEnum.getRandomLength());
            suffix = ThreadLocalRandom.current().nextLong(pow);
            sno = sno * pow;
        }
        String format = "%0" + (serialnoEnum.getLength() - serialnoEnum.getPrefix().length() - serialnoEnum.getRandomLength()) + "d";
        return serialnoEnum.getPrefix() + String.format(format, sno + suffix);
    }

    /**
     * 生成编号池
     */
    public void generateSerialnos() {
        log.info("开始生成编号池 ... ");
        for (SerialnoEnumerable serialnoEnum : serialnoEnums) {
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
        }, 10 * 1000L, 10 * 60 * 1000L, TimeUnit.MILLISECONDS);
    }

}

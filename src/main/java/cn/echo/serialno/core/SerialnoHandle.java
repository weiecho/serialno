package cn.echo.serialno.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 序列号业务处理
 */
public class SerialnoHandle {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // 判断是否幸运号（3位同号 + 4位连号）
    private final static Pattern luckSuidRegexPattern = Pattern.compile("^\\d*([0-9])\\1{2,}\\d*$" + // 连续相同数字
            "|\\d*(0(?=1)|1(?=2)|2(?=3)|3(?=4)|4(?=5)|5(?=6)|6(?=7)|7(?=8)|8(?=9)){4,}\\d" + // 连续递增数字
            "|\\d*(9(?=8)|8(?=7)|7(?=6)|6(?=5)|5(?=4)|4(?=3)|3(?=2)|2(?=1)|1(?=0)){4,}\\d"); // 连续递减数字

    private SerialnoCache serialnoCache;

    public SerialnoHandle(SerialnoCache serialnoCache) {
        this.serialnoCache = serialnoCache;
    }

    /**
     * 生成序列编号
     * 特殊处理：去除幸运编号/随机ID位置
     */
    public void generateSerialNo(SerialnoEnumerable serialnoEnum) {

        Integer oneTimeCount = 100000; // 一次调用生成编号数量
        List<Long> listSerialNo = new ArrayList<Long>();
        Long initNum = serialnoCache.allocSerialnoByBizTag(serialnoEnum);
        if(initNum <= -1L) {
            //如果号池超量则不再生成
            return;
        }

        long maxNum = initNum + serialnoEnum.getStep() - 1;
        for (long i = initNum; i <= maxNum; i++) {
            //去除幸运号
            if (serialnoEnum.getExcludeLuckNum()) {
                boolean isLuckyNum = luckSuidRegexPattern.matcher(i+"").find();
                if (!isLuckyNum) {
                    listSerialNo.add(i);
                } else if (i < maxNum){
                    continue;
                }
            } else {
                listSerialNo.add(i);
            }

            // 一次调用生成编号
            if (listSerialNo.size() >= oneTimeCount || i == maxNum) {
                //SerialnoStrategy.RANDOM 打乱顺序
                if (serialnoEnum.getStrategy() == SerialnoStrategy.RANDOM) {
                    Collections.shuffle(listSerialNo);
                }
                serialnoCache.addSerialnos(serialnoEnum, listSerialNo);
                log.info("编号池[{}]生产完成，本次生成编号量[{}]", serialnoEnum.getBizTag(), listSerialNo.size());
                listSerialNo.clear();
            }
        }
    }
}

package cn.echo.serialno.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Config {

    public static Map<String, Long> SerialInitMap;

    @Value("#{${serialno.init.nums:{}}}")
    public void setSerialInitMap(Map<String, Long> initMap) {
        SerialInitMap = initMap;
    }
}

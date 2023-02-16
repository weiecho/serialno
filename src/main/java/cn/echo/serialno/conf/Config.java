package cn.echo.serialno.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Config {

    /**
     * 环境编号（0-10）
     * 建议生产环境编号10
     * 其他每个环境预留1000w个编号
     */
    public static int envCode;

    @Value("${serialno.env.code:0}")
    public void setEnvCode(int code) {
        envCode = code;
    }
}

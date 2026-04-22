package com.abc.framework.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "abc.security")
public class SecurityProperties {
    /** HMAC 秘钥（>= 32 字节），生产从配置中心/K8s Secret 注入 */
    private String jwtSecret = "change-me-change-me-change-me-change-me";
    /** 过期时间（秒） */
    private long jwtExpire = 7200;
    /** 放行路径 */
    private List<String> permitAll = new ArrayList<>(List.of(
            "/auth/login", "/auth/register", "/auth/logout",
            "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/error"
    ));
}

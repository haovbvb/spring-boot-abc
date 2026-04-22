package com.abc.framework.redis;

import com.abc.framework.redis.config.RedisConfig;
import com.abc.framework.redis.ratelimit.RateLimitAspect;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({RedisConfig.class, RateLimitAspect.class})
public class RedisAutoConfiguration {
}

package com.abc.framework.redis.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 基于 Redis 的滑动窗口限流。key = prefix + ":" + SpEL(key)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String key() default "";

    String prefix() default "rl";

    /** 限流窗口（秒） */
    int period() default 1;

    /** 窗口内最大调用次数 */
    int limit() default 10;

    String message() default "too many requests";
}

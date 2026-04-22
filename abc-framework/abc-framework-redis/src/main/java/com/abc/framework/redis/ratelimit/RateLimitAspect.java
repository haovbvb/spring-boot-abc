package com.abc.framework.redis.ratelimit;

import com.abc.common.api.ResultCode;
import com.abc.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nd = new DefaultParameterNameDiscoverer();

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]); " +
                    "if tonumber(c) == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; " +
                    "return c;",
            Long.class);

    @Around("@annotation(rl)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rl) throws Throwable {
        String key = rl.prefix() + ":" + buildKey(pjp, rl.key());
        Long count = redisTemplate.execute(SCRIPT, Collections.singletonList(key), rl.period());
        if (count != null && count > rl.limit()) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS, rl.message());
        }
        return pjp.proceed();
    }

    private String buildKey(ProceedingJoinPoint pjp, String spel) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method m = sig.getMethod();
        String base = m.getDeclaringClass().getSimpleName() + "#" + m.getName();
        if (spel == null || spel.isBlank()) return base;
        EvaluationContext ctx = new StandardEvaluationContext();
        String[] names = nd.getParameterNames(m);
        Object[] args = pjp.getArgs();
        if (names != null) {
            for (int i = 0; i < names.length; i++) ctx.setVariable(names[i], args[i]);
        }
        Object v = parser.parseExpression(spel).getValue(ctx);
        return base + ":" + v;
    }
}

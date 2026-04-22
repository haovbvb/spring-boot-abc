package com.abc.framework.log;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Around("@annotation(op)")
    public Object around(ProceedingJoinPoint pjp, OperationLog op) throws Throwable {
        long start = System.currentTimeMillis();
        String user = currentUser();
        String uri = currentUri();
        String method = ((MethodSignature) pjp.getSignature()).getMethod().getName();
        try {
            Object ret = pjp.proceed();
            log.info("OP module={} action={} user={} method={} uri={} cost={}ms status=OK",
                    op.module(), op.value(), user, method, uri, System.currentTimeMillis() - start);
            return ret;
        } catch (Throwable ex) {
            log.warn("OP module={} action={} user={} method={} uri={} cost={}ms status=FAIL err={}",
                    op.module(), op.value(), user, method, uri,
                    System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }

    private String currentUser() {
        try {
            Class<?> holder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object ctx = holder.getMethod("getContext").invoke(null);
            Object auth = ctx.getClass().getMethod("getAuthentication").invoke(ctx);
            if (auth == null) return "anonymous";
            Object principal = auth.getClass().getMethod("getPrincipal").invoke(auth);
            return String.valueOf(principal);
        } catch (Throwable t) {
            return "anonymous";
        }
    }

    private String currentUri() {
        var attr = RequestContextHolder.getRequestAttributes();
        if (attr instanceof ServletRequestAttributes s) {
            HttpServletRequest r = s.getRequest();
            return r.getMethod() + " " + r.getRequestURI();
        }
        return "-";
    }
}

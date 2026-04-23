package com.abc.framework.security.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.abc.common.api.R;
import com.abc.common.api.ResultCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 将 Spring Security 在方法级（@PreAuthorize 等）抛出的鉴权异常
 * 统一转换为 {@link R} 响应体，并返回合适的 HTTP 状态码。
 *
 * <p>注意：过滤器链阶段（如无 Token / Token 失效）触发的异常不会经过本处理器，
 * 需在 SecurityConfig 里配置 AuthenticationEntryPoint / AccessDeniedHandler。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(R.fail(ResultCode.FORBIDDEN));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<R<Void>> handleAuthentication(AuthenticationException e) {
        log.warn("authentication failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(R.fail(ResultCode.UNAUTHORIZED));
    }
}

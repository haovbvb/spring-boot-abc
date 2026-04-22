package com.abc.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode implements IErrorCode {
    SUCCESS("000000", "success"),
    FAILED("999999", "system error"),
    VALIDATE_FAILED("100001", "parameter invalid"),
    UNAUTHORIZED("100401", "unauthorized"),
    FORBIDDEN("100403", "forbidden"),
    NOT_FOUND("100404", "resource not found"),
    TOO_MANY_REQUESTS("100429", "too many requests");

    private final String code;
    private final String message;
}

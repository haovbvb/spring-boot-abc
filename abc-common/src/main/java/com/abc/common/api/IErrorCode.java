package com.abc.common.api;

/**
 * 错误码规范：6 位 业务域(2) + 模块(2) + 序号(2)
 */
public interface IErrorCode {
    String getCode();

    String getMessage();
}

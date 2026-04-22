package com.abc.common.exception;

import com.abc.common.api.IErrorCode;
import com.abc.common.api.ResultCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final String code;

    public BizException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(IErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BizException(String message) {
        super(message);
        this.code = ResultCode.FAILED.getCode();
    }
}

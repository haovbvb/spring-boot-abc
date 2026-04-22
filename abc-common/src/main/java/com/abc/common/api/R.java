package com.abc.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.slf4j.MDC;

import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    public static final String TRACE_ID_KEY = "traceId";

    private String code;
    private String message;
    private T data;
    private String traceId;
    private long timestamp = System.currentTimeMillis();

    private R(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = MDC.get(TRACE_ID_KEY);
    }

    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> fail(IErrorCode code) {
        return new R<>(code.getCode(), code.getMessage(), null);
    }

    public static <T> R<T> fail(IErrorCode code, String message) {
        return new R<>(code.getCode(), message, null);
    }

    public static <T> R<T> fail(String code, String message) {
        return new R<>(code, message, null);
    }
}

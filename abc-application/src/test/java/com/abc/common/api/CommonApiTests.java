package com.abc.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.abc.common.exception.BizException;

/**
 * 基础契约冒烟测试：确保 R / ResultCode / BizException 的不变式不被破坏。
 * 不加载 Spring 上下文，纯单元测试。
 */
class CommonApiTests {

    @Test
    void okWithDataShouldUseSuccessCode() {
        R<String> r = R.ok("hi");
        assertEquals(ResultCode.SUCCESS.getCode(), r.getCode());
        assertEquals("hi", r.getData());
        assertNotNull(r.getMessage());
    }

    @Test
    void okWithoutDataShouldHaveNullData() {
        R<Void> r = R.ok();
        assertEquals(ResultCode.SUCCESS.getCode(), r.getCode());
        assertNull(r.getData());
    }

    @Test
    void failWithErrorCodeShouldPropagateCodeAndMessage() {
        R<Void> r = R.fail(ResultCode.FORBIDDEN);
        assertEquals(ResultCode.FORBIDDEN.getCode(), r.getCode());
        assertEquals(ResultCode.FORBIDDEN.getMessage(), r.getMessage());
    }

    @Test
    void failWithOverriddenMessageShouldKeepCodeReplaceMessage() {
        R<Void> r = R.fail(ResultCode.VALIDATE_FAILED, "custom");
        assertEquals(ResultCode.VALIDATE_FAILED.getCode(), r.getCode());
        assertEquals("custom", r.getMessage());
    }

    @Test
    void bizExceptionShouldCarryErrorCode() {
        BizException ex = new BizException(ResultCode.UNAUTHORIZED, "bad token");
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
        assertEquals("bad token", ex.getMessage());
    }
}

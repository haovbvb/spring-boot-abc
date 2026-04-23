package com.abc.auth.dto;

/**
 * 登录/注册返回体。
 */
public record AuthResp(Long userId, String username, String token) {
}

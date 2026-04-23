package com.abc.user.vo;

import com.abc.user.entity.SysUser;

/**
 * 用户对外视图，屏蔽密码等敏感字段。
 */
public record SysUserVO(Long id, String username, String nickname, Integer status) {

    public static SysUserVO from(SysUser user) {
        if (user == null) {
            return null;
        }
        return new SysUserVO(user.getId(), user.getUsername(), user.getNickname(), user.getStatus());
    }
}

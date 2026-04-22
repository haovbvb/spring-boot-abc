package com.abc.user.controller;

import com.abc.common.api.R;
import com.abc.framework.log.OperationLog;
import com.abc.framework.redis.ratelimit.RateLimit;
import com.abc.user.entity.SysUser;
import com.abc.user.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "user")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserMapper mapper;

    @Operation(summary = "get by id")
    @GetMapping("/{id}")
    @RateLimit(limit = 20, period = 1, key = "#id")
    @OperationLog(module = "user", value = "query")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public R<SysUser> get(@PathVariable("id") Long id) {
        return R.ok(mapper.selectById(id));
    }
}

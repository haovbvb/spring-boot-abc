package com.abc.auth;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abc.auth.dto.AuthResp;
import com.abc.auth.dto.LoginReq;
import com.abc.auth.dto.RegisterReq;
import com.abc.common.api.R;
import com.abc.common.api.ResultCode;
import com.abc.common.exception.BizException;
import com.abc.framework.security.jwt.JwtTokenProvider;
import com.abc.user.entity.SysUser;
import com.abc.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "auth")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider provider;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "login")
    @PostMapping("/login")
    public R<AuthResp> login(@Valid @RequestBody LoginReq req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.usernameTrimmed())
                .last("limit 1"));

        if (user == null || !passwordMatched(req.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "username or password invalid");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "user is disabled");
        }

        String token = provider.generate(user.getUsername(), Map.of("roles", "ROLE_USER", "uid", user.getId()));
        return R.ok(new AuthResp(user.getId(), user.getUsername(), token));
    }

    @Operation(summary = "register")
    @PostMapping("/register")
    public R<AuthResp> register(@Valid @RequestBody RegisterReq req) {
        long existed = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.usernameTrimmed()));
        if (existed > 0) {
            throw new BizException(ResultCode.VALIDATE_FAILED, "username already exists");
        }

        SysUser user = new SysUser();
        user.setUsername(req.usernameTrimmed());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(StringUtils.hasText(req.getNickname()) ? req.nicknameTrimmed() : req.usernameTrimmed());
        user.setStatus(1);
        userMapper.insert(user);

        String token = provider.generate(user.getUsername(), Map.of("roles", "ROLE_USER", "uid", user.getId()));
        return R.ok(new AuthResp(user.getId(), user.getUsername(), token));
    }

    private boolean passwordMatched(String rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return false;
        }
        if (passwordEncoder.matches(rawPassword, encodedPassword)) {
            return true;
        }
        // Keep backward compatibility with old dev data that may store plain-text password.
        return rawPassword.equals(encodedPassword);
    }
}

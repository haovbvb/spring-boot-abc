package com.abc.auth;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    public R<Map<String, Object>> login(@Valid @RequestBody LoginReq req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.username())
                .last("limit 1"));

        if (user == null || !passwordMatched(req.password(), user.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "username or password invalid");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "user is disabled");
        }

        String token = provider.generate(user.getUsername(), Map.of("roles", "ROLE_USER", "uid", user.getId()));
        return R.ok(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "token", token
        ));
    }

    @Operation(summary = "register")
    @PostMapping("/register")
    public R<Map<String, Object>> register(@Valid @RequestBody RegisterReq req) {
        long existed = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.username()));
        if (existed > 0) {
            throw new BizException(ResultCode.VALIDATE_FAILED, "username already exists");
        }

        SysUser user = new SysUser();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname().trim() : req.username());
        user.setStatus(1);
        userMapper.insert(user);

        String token = provider.generate(user.getUsername(), Map.of("roles", "ROLE_USER", "uid", user.getId()));
        return R.ok(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "token", token
        ));
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

    @Data
    public static class LoginReq {
        @NotBlank
        @Size(max = 64)
        private String username;
        @NotBlank
        @Size(max = 64)
        private String password;

        public String username() {
            return username == null ? null : username.trim();
        }

        public String password() {
            return password;
        }
    }

    @Data
    public static class RegisterReq {
        @NotBlank
        @Size(min = 3, max = 64)
        private String username;
        @NotBlank
        @Size(min = 6, max = 64)
        private String password;
        @Size(max = 64)
        private String nickname;

        public String username() {
            return username == null ? null : username.trim();
        }

        public String password() {
            return password;
        }
    }
}

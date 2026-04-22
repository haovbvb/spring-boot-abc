package com.abc.auth;

import com.abc.common.api.R;
import com.abc.framework.security.jwt.JwtTokenProvider;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider provider;

    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody LoginReq req) {
        // TODO: verify user/password from DB; demo only
        String token = provider.generate(req.username(), Map.of("roles", "ROLE_USER"));
        return R.ok(Map.of("token", token));
    }

    @Data
    public static class LoginReq {
        @NotBlank
        private String username;
        @NotBlank
        private String password;

        public String username() { return username; }
    }
}

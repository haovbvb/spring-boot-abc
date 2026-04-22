package com.abc.framework.security;

import com.abc.framework.security.config.SecurityConfig;
import com.abc.framework.security.config.SecurityProperties;
import com.abc.framework.security.jwt.JwtAuthenticationFilter;
import com.abc.framework.security.jwt.JwtTokenProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityProperties.class)
@Import({JwtTokenProvider.class, JwtAuthenticationFilter.class, SecurityConfig.class})
public class SecurityAutoConfiguration {
}

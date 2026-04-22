package com.abc.framework.web;

import com.abc.framework.web.config.WebMvcConfig;
import com.abc.framework.web.exception.GlobalExceptionHandler;
import com.abc.framework.web.trace.TraceIdFilter;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@Import({WebMvcConfig.class, GlobalExceptionHandler.class, TraceIdFilter.class})
public class WebAutoConfiguration {
}

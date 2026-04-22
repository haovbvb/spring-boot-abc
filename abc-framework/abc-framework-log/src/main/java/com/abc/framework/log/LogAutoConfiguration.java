package com.abc.framework.log;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({OperationLogAspect.class})
public class LogAutoConfiguration {
}

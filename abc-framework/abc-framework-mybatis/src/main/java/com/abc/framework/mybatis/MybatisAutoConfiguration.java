package com.abc.framework.mybatis;

import com.abc.framework.mybatis.config.MybatisPlusConfig;
import com.abc.framework.mybatis.handler.AuditMetaObjectHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({MybatisPlusConfig.class, AuditMetaObjectHandler.class})
public class MybatisAutoConfiguration {
}

package com.admin.template.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.admin.template.mapper")
public class PersistenceConfig {
}

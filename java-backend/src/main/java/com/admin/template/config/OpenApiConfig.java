package com.admin.template.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI adminTemplateOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Admin Template API")
                        .description("后台管理系统接口文档")
                        .version("1.0"));
    }
}

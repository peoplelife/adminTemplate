package com.admin.template.config;

import com.admin.template.security.PermissionInterceptor;
import com.admin.template.security.LoginRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final PermissionInterceptor permissionInterceptor;
    private final LoginRateLimitInterceptor loginRateLimitInterceptor;
    private final UploadProperties uploadProperties;

    public WebMvcConfig(PermissionInterceptor permissionInterceptor,
                        LoginRateLimitInterceptor loginRateLimitInterceptor,
                        UploadProperties uploadProperties) {
        this.permissionInterceptor = permissionInterceptor;
        this.loginRateLimitInterceptor = loginRateLimitInterceptor;
        this.uploadProperties = uploadProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginRateLimitInterceptor).addPathPatterns("/v1/auth/login");
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/v1/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path publicRoot = Path.of(uploadProperties.getDirectory()).toAbsolutePath().normalize();
        String resourceLocation = publicRoot.toUri().toString();
        if (!resourceLocation.endsWith("/")) resourceLocation += "/";
        registry.addResourceHandler("/public/**")
                .addResourceLocations(resourceLocation);
    }
}

package com.admin.template.security;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionInterceptor implements HandlerInterceptor {
    private final PermissionService permissionService;

    public PermissionInterceptor(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) return true;
        Permission permission = method.getMethodAnnotation(Permission.class);
        if (permission == null) permission = method.getBeanType().getAnnotation(Permission.class);
        if (permission == null) return true;

        Object principal = request.getAttribute(JwtAuthenticationFilter.USER_ATTRIBUTE);
        if (!(principal instanceof AuthenticatedUser user)) {
            throw new BizException(BusinessError.AUTH_REQUIRED);
        }
        permissionService.requirePermissions(user, permission.value());
        return true;
    }
}

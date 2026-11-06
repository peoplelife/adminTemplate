package com.admin.template.security;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.mapper.MenuMapper;
import com.admin.template.mapper.UserRoleMapper;
import com.admin.template.dto.security.ActiveRole;
import com.admin.template.dto.security.MenuPermission;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionService {
    private static final String SUPER_ADMIN = "super_admin";
    private final UserRoleMapper userRoleMapper;
    private final MenuMapper menuMapper;

    public PermissionService(UserRoleMapper userRoleMapper, MenuMapper menuMapper) {
        this.userRoleMapper = userRoleMapper;
        this.menuMapper = menuMapper;
    }

    public void requirePermissions(AuthenticatedUser user, String... requiredPermissions) {
        List<ActiveRole> roles = userRoleMapper.selectActiveRolesByUserId(user.id());
        if (roles.isEmpty()) {
            throw new BizException(BusinessError.ACCOUNT_WITHOUT_ROLE);
        }

        boolean superAdmin = roles.stream().anyMatch(role -> SUPER_ADMIN.equals(role.code()));
        List<MenuPermission> menus = superAdmin
                ? menuMapper.selectActivePermissionsForAllMenus()
                : menuMapper.selectActivePermissionsForRoleIds(roles.stream().map(ActiveRole::id).toList());
        if (menus.isEmpty()) {
            throw new BizException(BusinessError.ACCOUNT_WITHOUT_PERMISSION);
        }
        if (superAdmin) return;

        Set<String> granted = menus.stream().map(MenuPermission::permissionCode)
                .filter(code -> code != null && !code.isBlank()).collect(Collectors.toSet());
        for (String required : requiredPermissions) {
            if (!granted.contains(required)) {
                throw new BizException(BusinessError.PERMISSION_DENIED);
            }
        }
    }
}

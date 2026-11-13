package com.admin.template.service.auth;

import com.admin.template.dto.auth.LoginResponse;
import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.mapper.MenuMapper;
import com.admin.template.mapper.UserMapper;
import com.admin.template.mapper.UserRoleMapper;
import com.admin.template.dto.security.ActiveRole;
import com.admin.template.entity.MenuEntity;
import com.admin.template.dto.security.MenuPermission;
import com.admin.template.entity.UserEntity;
import com.admin.template.security.AuthenticatedUser;
import com.admin.template.security.JwtService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AuthService {
    private static final String SUPER_ADMIN = "super_admin";
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final MenuMapper menuMapper;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, UserRoleMapper userRoleMapper,
                       MenuMapper menuMapper, JwtService jwtService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.menuMapper = menuMapper;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(String username, String password) {
        UserEntity user = userMapper.selectOne(new QueryWrapper<UserEntity>().eq("username", username));
        if (user == null || user.password == null || !String.valueOf(password).equals(user.password)) {
            throw new BizException(BusinessError.INVALID_CREDENTIALS);
        }
        return finishLogin(user);
    }

    @Transactional
    public LoginResponse loginTrustedUser(long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null || user.deletedAt != null) throw new BizException(BusinessError.USER_NOT_FOUND);
        return finishLogin(user);
    }

    private LoginResponse finishLogin(UserEntity user) {
        if (!Boolean.TRUE.equals(user.status)) {
            throw new BizException(BusinessError.ACCOUNT_DISABLED);
        }

        List<ActiveRole> roles = userRoleMapper.selectActiveRolesByUserId(user.id);
        if (roles.isEmpty()) {
            throw new BizException(BusinessError.ACCOUNT_WITHOUT_ROLE);
        }
        List<MenuPermission> menus = roles.stream().anyMatch(role -> SUPER_ADMIN.equals(role.code()))
                ? menuMapper.selectActivePermissionsForAllMenus()
                : menuMapper.selectActivePermissionsForRoleIds(roles.stream().map(ActiveRole::id).toList());
        if (menus.isEmpty()) {
            throw new BizException(BusinessError.ACCOUNT_WITHOUT_PERMISSION);
        }

        boolean initialPassword = user.lastLogin == null;
        user.lastLogin = LocalDateTime.now(ZoneOffset.UTC);
        user.updatedAt = user.lastLogin;
        userMapper.updateById(user);
        Instant issuedAt = Instant.now();
        // JWT iat 精度为秒；同秒重新登录时签发时间必须晚于退出截止时间。
        if (user.lastLogout != null && issuedAt.getEpochSecond() <= user.lastLogout.toEpochSecond(ZoneOffset.UTC)) {
            issuedAt = user.lastLogout.toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
        }
        String token = jwtService.createToken(user.id, user.username, roles.stream().map(ActiveRole::code).toList(), issuedAt);
        return new LoginResponse(token, initialPassword);
    }

    @Transactional
    public void logout(long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) return;
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC);
        if (user.lastLogout != null && !cutoff.isAfter(user.lastLogout.plusSeconds(1))) {
            cutoff = user.lastLogout.plusSeconds(1);
        }
        user.lastLogout = cutoff;
        user.updatedAt = user.lastLogout;
        userMapper.updateById(user);
    }

    public AuthenticatedUser currentUser(long userId, String username, List<String> roles) {
        return new AuthenticatedUser(userId, username, roles);
    }
}

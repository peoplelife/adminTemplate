package com.admin.template.service.auth;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.mapper.MenuMapper;
import com.admin.template.mapper.UserMapper;
import com.admin.template.mapper.UserRoleMapper;
import com.admin.template.dto.security.ActiveRole;
import com.admin.template.dto.security.MenuPermission;
import com.admin.template.entity.UserEntity;
import com.admin.template.security.JwtService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void passwordlessAccountsRejectNullAndTheLiteralNullPassword() {
        UserEntity user = new UserEntity();
        user.id = 5L;
        user.username = "github_account";
        user.status = true;
        UserMapper users = mock(UserMapper.class);
        when(users.selectOne(any(QueryWrapper.class))).thenReturn(user);
        UserRoleMapper roles = mock(UserRoleMapper.class);
        MenuMapper menus = mock(MenuMapper.class);
        when(roles.selectActiveRolesByUserId(5L)).thenReturn(List.of(new ActiveRole(2L, "user", "User")));
        when(menus.selectActivePermissionsForRoleIds(List.of(2L))).thenReturn(List.of(new MenuPermission("home", "read")));
        AuthService service = new AuthService(users, roles, menus, new JwtService(SECRET, false));

        for (String password : new String[]{null, "null", "", "legacy-password"}) {
            assertThatThrownBy(() -> service.login("github_account", password))
                    .isInstanceOf(BizException.class)
                    .hasFieldOrPropertyWithValue("error", BusinessError.INVALID_CREDENTIALS);
        }
        assertThat(user.lastLogin).isNull();
    }

    @Test
    void rejectsDisabledAccountsBeforeIssuingToken() {
        UserEntity user = new UserEntity();
        user.id = 5L;
        user.password = "secret123";
        user.status = false;
        UserMapper users = mock(UserMapper.class);
        when(users.selectOne(any(QueryWrapper.class))).thenReturn(user);
        AuthService service = new AuthService(users, mock(UserRoleMapper.class), mock(MenuMapper.class),
                new JwtService(SECRET, false));

        assertThatThrownBy(() -> service.login("alice", "secret123"))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.ACCOUNT_DISABLED);
    }

    @Test
    void issuesTokenAndReportsInitialPasswordWhenLoginAndMenuAccessAreValid() {
        UserEntity user = new UserEntity();
        user.id = 5L;
        user.username = "alice";
        user.password = "secret123";
        user.status = true;
        UserMapper users = mock(UserMapper.class);
        UserRoleMapper userRoles = mock(UserRoleMapper.class);
        MenuMapper menus = mock(MenuMapper.class);
        when(users.selectOne(any(QueryWrapper.class))).thenReturn(user);
        when(userRoles.selectActiveRolesByUserId(5L)).thenReturn(List.of(new ActiveRole(2L, "operator", "Operator")));
        when(menus.selectActivePermissionsForRoleIds(List.of(2L)))
                .thenReturn(List.of(new MenuPermission("home", "system:user:list")));
        AuthService service = new AuthService(users, userRoles, menus, new JwtService(SECRET, false));

        var response = service.login("alice", "secret123");

        assertThat(response.initialPasswordFlag()).isTrue();
        assertThat(new JwtService(SECRET, false).parseToken(response.token()).roles()).containsExactly("operator");
        verify(users).updateById(user);
    }

    @Test
    void immediateReloginAndRepeatedLogoutInvalidateEachPreviousToken() {
        UserEntity user = new UserEntity();
        user.id = 5L;
        user.username = "alice";
        user.password = "secret123";
        user.status = true;
        UserMapper users = mock(UserMapper.class);
        UserRoleMapper roles = mock(UserRoleMapper.class);
        MenuMapper menus = mock(MenuMapper.class);
        when(users.selectOne(any(QueryWrapper.class))).thenReturn(user);
        when(users.selectById(5L)).thenReturn(user);
        when(roles.selectActiveRolesByUserId(5L)).thenReturn(List.of(new ActiveRole(2L, "operator", "Operator")));
        when(menus.selectActivePermissionsForRoleIds(List.of(2L))).thenReturn(List.of(new MenuPermission("home", "read")));
        JwtService jwt = new JwtService(SECRET, false);
        AuthService service = new AuthService(users, roles, menus, jwt);
        for (int i = 0; i < 3; i++) {
            var token = jwt.parseTokenDetails(service.login("alice", "secret123").token());
            if (user.lastLogout != null) assertThat(token.issuedAt()).isAfter(user.lastLogout.toInstant(ZoneOffset.UTC));
            service.logout(5L);
            assertThat(token.issuedAt()).isBeforeOrEqualTo(user.lastLogout.toInstant(ZoneOffset.UTC));
        }
    }

    @Test
    @ResourceLock("java.util.TimeZone.default")
    void writesLoginAndLogoutAuditTimesInUtcRegardlessOfHostTimezone() {
        TimeZone originalZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            UserEntity user = new UserEntity();
            user.id = 5L;
            user.username = "alice";
            user.password = "secret123";
            user.status = true;
            UserMapper users = mock(UserMapper.class);
            UserRoleMapper userRoles = mock(UserRoleMapper.class);
            MenuMapper menus = mock(MenuMapper.class);
            when(users.selectOne(any(QueryWrapper.class))).thenReturn(user);
            when(users.selectById(5L)).thenReturn(user);
            when(userRoles.selectActiveRolesByUserId(5L))
                    .thenReturn(List.of(new ActiveRole(2L, "operator", "Operator")));
            when(menus.selectActivePermissionsForRoleIds(List.of(2L)))
                    .thenReturn(List.of(new MenuPermission("home", "system:user:list")));
            AuthService service = new AuthService(users, userRoles, menus, new JwtService(SECRET, false));

            LocalDateTime beforeLogin = LocalDateTime.now(ZoneOffset.UTC);
            service.login("alice", "secret123");
            LocalDateTime afterLogin = LocalDateTime.now(ZoneOffset.UTC);

            assertThat(user.lastLogin).isBetween(beforeLogin, afterLogin);
            assertThat(user.updatedAt).isEqualTo(user.lastLogin);

            LocalDateTime beforeLogout = LocalDateTime.now(ZoneOffset.UTC);
            service.logout(5L);
            LocalDateTime afterLogout = LocalDateTime.now(ZoneOffset.UTC);

            assertThat(user.lastLogout).isBetween(beforeLogout, afterLogout);
            assertThat(user.updatedAt).isEqualTo(user.lastLogout);
        } finally {
            TimeZone.setDefault(originalZone);
        }
    }
}

package com.admin.template.service.user;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.dto.menu.MenuCreateRequest;
import com.admin.template.dto.menu.MenuUpdateRequest;
import com.admin.template.dto.role.RoleCreateRequest;
import com.admin.template.dto.role.RoleUpdateRequest;
import com.admin.template.dto.user.UserCreateRequest;
import com.admin.template.dto.user.UserUpdateRequest;
import com.admin.template.entity.MenuEntity;
import com.admin.template.entity.RoleEntity;
import com.admin.template.entity.UserEntity;
import com.admin.template.mapper.MenuMapper;
import com.admin.template.mapper.RoleMapper;
import com.admin.template.mapper.RoleMenuMapper;
import com.admin.template.mapper.UserMapper;
import com.admin.template.mapper.UserRoleMapper;
import com.admin.template.service.menu.MenuService;
import com.admin.template.service.role.RoleService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessConflictTest {
    @Test
    void userCreationConvertsTheDatabaseRaceToUsernameConflict() {
        UserMapper users = mock(UserMapper.class);
        when(users.insert(any(UserEntity.class))).thenThrow(new DuplicateKeyException("username race"));
        UserService service = new UserService(users, mock(RoleMapper.class), mock(UserRoleMapper.class));

        assertThatThrownBy(() -> service.createUser(new UserCreateRequest("alice", "123456", null, null, null, null, 1, List.of())))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.USERNAME_EXISTS);
    }

    @Test
    void userUpdateConvertsTheDatabaseRaceToUsernameConflict() {
        UserMapper users = mock(UserMapper.class);
        UserEntity existing = new UserEntity();
        existing.id = 2L;
        existing.username = "old";
        when(users.selectById(2L)).thenReturn(existing);
        when(users.updateById(any(UserEntity.class))).thenThrow(new DuplicateKeyException("username race"));
        UserService service = new UserService(users, mock(RoleMapper.class), mock(UserRoleMapper.class));

        assertThatThrownBy(() -> service.updateUser(2, new UserUpdateRequest("alice", null, null, null, null, null, null)))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.USERNAME_EXISTS);
    }

    @Test
    void roleCreationConvertsTheDatabaseRaceToRoleCodeConflict() {
        RoleMapper roles = mock(RoleMapper.class);
        when(roles.insert(any(RoleEntity.class))).thenThrow(new DuplicateKeyException("role code race"));
        RoleService service = new RoleService(roles, mock(RoleMenuMapper.class), mock(UserRoleMapper.class));

        assertThatThrownBy(() -> service.create(new RoleCreateRequest("operator", "Operator", null, 1, List.of())))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.ROLE_CODE_EXISTS);
    }

    @Test
    void roleUpdateConvertsTheDatabaseRaceToRoleCodeConflict() {
        RoleMapper roles = mock(RoleMapper.class);
        RoleEntity existing = new RoleEntity();
        existing.id = 2L;
        existing.code = "old";
        when(roles.selectById(2L)).thenReturn(existing);
        when(roles.updateById(any(RoleEntity.class))).thenThrow(new DuplicateKeyException("role code race"));
        RoleService service = new RoleService(roles, mock(RoleMenuMapper.class), mock(UserRoleMapper.class));

        assertThatThrownBy(() -> service.update(2, new RoleUpdateRequest("operator", null, null, null, null)))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.ROLE_CODE_EXISTS);
    }

    @Test
    void menuCreationConvertsAnIdCollisionToMenuIdConflict() {
        MenuMapper menus = mock(MenuMapper.class);
        when(menus.insert(any(MenuEntity.class))).thenThrow(new DuplicateKeyException("menu id collision"));
        MenuService service = new MenuService(menus, mock(UserRoleMapper.class), mock(RoleMapper.class), mock(RoleMenuMapper.class));

        assertThatThrownBy(() -> service.create(new MenuCreateRequest("New", "C", null, null, null, null, null, null, null, null)))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.MENU_ID_EXISTS);
    }

    @Test
    void menuUpdateConvertsTheDatabaseRaceToMenuIdConflict() {
        MenuMapper menus = mock(MenuMapper.class);
        MenuEntity existing = new MenuEntity();
        existing.id = 2L;
        existing.menuId = "old";
        when(menus.selectById(2L)).thenReturn(existing);
        when(menus.update(any(MenuEntity.class), any(UpdateWrapper.class))).thenThrow(new DuplicateKeyException("menu id race"));
        MenuService service = new MenuService(menus, mock(UserRoleMapper.class), mock(RoleMapper.class), mock(RoleMenuMapper.class));

        assertThatThrownBy(() -> service.update(2, new MenuUpdateRequest("new", null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.MENU_ID_EXISTS);
    }
}

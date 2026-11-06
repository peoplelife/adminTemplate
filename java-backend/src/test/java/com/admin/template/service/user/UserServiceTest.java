package com.admin.template.service.user;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.mapper.RoleMapper;
import com.admin.template.mapper.UserMapper;
import com.admin.template.mapper.UserRoleMapper;
import com.admin.template.entity.UserEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {
    @Test
    void preventsDeletingTheCurrentUser() {
        UserMapper users = mock(UserMapper.class);
        when(users.selectById(42L)).thenReturn(new UserEntity());
        UserService service = new UserService(users, mock(RoleMapper.class), mock(UserRoleMapper.class));

        assertThatThrownBy(() -> service.deleteUser(42L, 42L))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.CANNOT_DELETE_SELF);
    }
}

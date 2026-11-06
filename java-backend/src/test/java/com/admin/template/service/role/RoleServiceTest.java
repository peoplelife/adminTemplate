package com.admin.template.service.role;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.mapper.RoleMapper;
import com.admin.template.mapper.RoleMenuMapper;
import com.admin.template.mapper.UserRoleMapper;
import com.admin.template.entity.RoleEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleServiceTest {
    @Test
    void protectsSystemRolesFromDeletion() {
        RoleEntity role = new RoleEntity();
        role.id = 9L;
        role.code = "admin";
        RoleMapper roles = mock(RoleMapper.class);
        when(roles.selectById(9L)).thenReturn(role);
        RoleService service = new RoleService(roles, mock(RoleMenuMapper.class), mock(UserRoleMapper.class));

        assertThatThrownBy(() -> service.delete(9L))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.SYSTEM_ROLE_PROTECTED);
    }
}

package com.admin.template.service.menu;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.mapper.MenuMapper;
import com.admin.template.mapper.RoleMapper;
import com.admin.template.mapper.RoleMenuMapper;
import com.admin.template.mapper.UserRoleMapper;
import com.admin.template.entity.MenuEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuServiceTest {
    @Test
    void preventsDeletingMenuWithActiveChildren() {
        MenuEntity menu = new MenuEntity();
        menu.id = 12L;
        menu.menuId = "parent";
        menu.status = true;
        MenuMapper menus = mock(MenuMapper.class);
        when(menus.selectById(12L)).thenReturn(menu);
        when(menus.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        MenuService service = new MenuService(menus, mock(UserRoleMapper.class), mock(RoleMapper.class),
                mock(RoleMenuMapper.class));

        assertThatThrownBy(() -> service.delete(12L))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.MENU_HAS_CHILDREN);
        verify(menus, never()).updateById(any(MenuEntity.class));
    }
}

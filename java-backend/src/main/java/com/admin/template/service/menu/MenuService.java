package com.admin.template.service.menu;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.mapper.MenuMapper;
import com.admin.template.mapper.RoleMapper;
import com.admin.template.mapper.RoleMenuMapper;
import com.admin.template.mapper.UserRoleMapper;
import com.admin.template.dto.security.ActiveRole;
import com.admin.template.entity.MenuEntity;
import com.admin.template.entity.RoleEntity;
import com.admin.template.entity.RoleMenuEntity;
import com.admin.template.dto.menu.MenuCreateRequest;
import com.admin.template.dto.menu.MenuResponse;
import com.admin.template.dto.menu.MenuUpdateRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class MenuService {
    private static final String SUPER_ADMIN = "super_admin";
    private final MenuMapper menuMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;

    public MenuService(MenuMapper menuMapper, UserRoleMapper userRoleMapper,
                       RoleMapper roleMapper, RoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    public List<MenuResponse> list(String menuName, String menuType) {
        QueryWrapper<MenuEntity> query = new QueryWrapper<MenuEntity>().eq("status", true)
                .orderByAsc("sort");
        if (menuName != null && !menuName.isBlank()) query.like("name", menuName.trim());
        if (menuType != null && !menuType.isBlank()) query.eq("type", menuType.trim().toUpperCase());
        return menuMapper.selectList(query).stream().map(menu -> toResponse(menu, true)).toList();
    }

    public List<MenuResponse> userMenus(long userId) {
        List<ActiveRole> roles = userRoleMapper.selectActiveRolesByUserId(userId);
        if (roles.isEmpty()) throw new BizException(BusinessError.ACCOUNT_WITHOUT_ROLE);
        List<MenuEntity> menus;
        if (roles.stream().anyMatch(role -> SUPER_ADMIN.equals(role.code()))) {
            menus = menuMapper.selectList(new QueryWrapper<MenuEntity>().eq("status", true)
                    .orderByAsc("sort").orderByAsc("id"));
        } else {
            List<Long> roleIds = roles.stream().map(ActiveRole::id).toList();
            List<String> menuIds = roleMenuMapper.selectList(new QueryWrapper<RoleMenuEntity>().in("role_id", roleIds))
                    .stream().map(link -> link.menuId).distinct().toList();
            menus = menuIds.isEmpty() ? List.of() : menuMapper.selectList(new QueryWrapper<MenuEntity>()
                    .eq("status", true).in("menu_id", menuIds).orderByAsc("sort").orderByAsc("id"));
        }
        if (menus.isEmpty()) throw new BizException(BusinessError.ACCOUNT_WITHOUT_PERMISSION);
        return menus.stream().map(menu -> toResponse(menu, false)).toList();
    }

    @Transactional
    public MenuResponse create(MenuCreateRequest request) {
        validateParent(request.parentId());
        MenuEntity menu = new MenuEntity();
        menu.menuId = "menu_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 3);
        menu.parentId = normalizeParent(request.parentId());
        menu.name = request.name().trim();
        menu.path = request.path();
        menu.component = request.component();
        menu.icon = request.icon();
        menu.sort = request.sort();
        menu.hidden = request.hidden() == 1;
        menu.isFull = request.isFull() == 1;
        menu.type = request.type();
        menu.buttonPermission = request.buttonPermission();
        menu.status = true;
        menu.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        menu.updatedAt = menu.createdAt;
        try {
            menuMapper.insert(menu);
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueViolation(exception)) throw menuIdExists();
            throw exception;
        }
        return toResponse(menu, true);
    }

    @Transactional
    public MenuResponse update(long id, MenuUpdateRequest request) {
        MenuEntity menu = menuMapper.selectById(id);
        if (menu == null) throw menuMissing();
        validateParent(request.parentId());
        if (request.menuId() != null && !request.menuId().equals(menu.menuId)
                && menuMapper.selectOne(new QueryWrapper<MenuEntity>().eq("menu_id", request.menuId())) != null) {
            throw new BizException(BusinessError.MENU_ID_EXISTS);
        }
        if (request.menuId() != null) menu.menuId = request.menuId();
        if (request.hasField("parentId")) menu.parentId = normalizeParent(request.parentId());
        if (request.name() != null) menu.name = request.name();
        if (request.hasField("path")) menu.path = request.path();
        if (request.hasField("component")) menu.component = request.component();
        if (request.hasField("icon")) menu.icon = request.icon();
        if (request.sort() != null) menu.sort = request.sort();
        if (request.hidden() != null) menu.hidden = request.hidden() == 1;
        if (request.isFull() != null) menu.isFull = request.isFull() == 1;
        if (request.type() != null) menu.type = request.type();
        if (request.hasField("buttonPermission")) menu.buttonPermission = request.buttonPermission();
        menu.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
        UpdateWrapper<MenuEntity> changes = new UpdateWrapper<MenuEntity>().eq("id", id);
        changes.set(request.hasField("path") && request.path() == null, "path", null);
        changes.set(request.hasField("component") && request.component() == null, "component", null);
        changes.set(request.hasField("icon") && request.icon() == null, "icon", null);
        changes.set(request.hasField("buttonPermission") && request.buttonPermission() == null, "buttonPermission", null);
        try {
            menuMapper.update(menu, changes);
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueViolation(exception)) throw menuIdExists();
            throw exception;
        }
        return toResponse(menu, true);
    }

    @Transactional
    public void delete(long id) {
        MenuEntity menu = menuMapper.selectById(id);
        if (menu == null || !Boolean.TRUE.equals(menu.status)) throw menuMissing();
        long children = menuMapper.selectCount(new QueryWrapper<MenuEntity>()
                .eq("parent_id", menu.menuId).eq("status", true));
        if (children > 0) throw new BizException(BusinessError.MENU_HAS_CHILDREN,
                "请先删除当前菜单的子菜单", null);
        menu.status = false;
        menu.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
        menuMapper.updateById(menu);
    }

    private void validateParent(String parentId) {
        if (parentId == null || parentId.isBlank() || "-1".equals(parentId)) return;
        MenuEntity parent = menuMapper.selectOne(new QueryWrapper<MenuEntity>().eq("menu_id", parentId));
        if (parent == null || !Boolean.TRUE.equals(parent.status)) {
            throw new BizException(BusinessError.MENU_PARENT_NOT_FOUND);
        }
    }

    private String normalizeParent(String parentId) {
        return parentId == null || parentId.isBlank() ? "0" : parentId;
    }

    private MenuResponse toResponse(MenuEntity menu, boolean includeId) {
        return new MenuResponse(includeId ? menu.id : null, menu.menuId, menu.parentId, menu.name, menu.path,
                menu.component, menu.icon, menu.sort == null ? 0 : menu.sort,
                Boolean.TRUE.equals(menu.hidden) ? 1 : 0, Boolean.TRUE.equals(menu.isFull) ? 1 : 0,
                menu.type, menu.buttonPermission);
    }

    private BizException menuMissing() {
        return new BizException(BusinessError.MENU_NOT_FOUND);
    }

    private BizException menuIdExists() {
        return new BizException(BusinessError.MENU_ID_EXISTS);
    }

    private boolean isUniqueViolation(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof DuplicateKeyException) return true;
            String message = current.getMessage();
            if (message != null && (message.toLowerCase().contains("duplicate")
                    || message.toLowerCase().contains("unique")
                    || message.toLowerCase().contains("idx_menu_id"))) return true;
        }
        return false;
    }
}

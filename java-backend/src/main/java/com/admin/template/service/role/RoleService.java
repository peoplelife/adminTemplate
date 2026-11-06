package com.admin.template.service.role;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.mapper.RoleMapper;
import com.admin.template.mapper.RoleMenuMapper;
import com.admin.template.mapper.UserRoleMapper;
import com.admin.template.entity.RoleEntity;
import com.admin.template.entity.RoleMenuEntity;
import com.admin.template.entity.UserRoleEntity;
import com.admin.template.dto.role.RoleCreateRequest;
import com.admin.template.dto.role.RolePageResponse;
import com.admin.template.dto.role.RoleResponse;
import com.admin.template.dto.role.RoleUpdateRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class RoleService {
    private static final List<String> SYSTEM_ROLES = List.of("admin", "super_admin");
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleService(RoleMapper roleMapper, RoleMenuMapper roleMenuMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public RolePageResponse list(int page, int pageSize, String name, Integer status) {
        QueryWrapper<RoleEntity> query = roleQuery(name, status).orderByDesc("id");
        long total = roleMapper.selectCount(roleQuery(name, status));
        List<RoleResponse> list = roleMapper.selectList(query.last(
                        "LIMIT " + pageSize + " OFFSET " + ((long) page - 1) * pageSize))
                .stream().map(this::toResponse).toList();
        return new RolePageResponse(list, new RolePageResponse.Pagination(page, pageSize, total,
                (int) Math.ceil((double) total / pageSize)));
    }

    public RoleResponse get(long id) {
        RoleEntity role = roleMapper.selectById(id);
        if (role == null) throw missing();
        return toResponse(role);
    }

    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        if (roleMapper.selectOne(new QueryWrapper<RoleEntity>().eq("code", request.code())) != null) {
            throw new BizException(BusinessError.ROLE_CODE_EXISTS);
        }
        RoleEntity role = new RoleEntity();
        role.code = request.code();
        role.name = request.name();
        role.description = request.description();
        role.status = request.status() == 1;
        role.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        role.updatedAt = role.createdAt;
        try {
            roleMapper.insert(role);
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueViolation(exception)) throw roleCodeExists();
            throw exception;
        }
        replaceMenus(role.id, request.menuIds());
        return toResponse(role);
    }

    @Transactional
    public RoleResponse update(long id, RoleUpdateRequest request) {
        RoleEntity role = roleMapper.selectById(id);
        if (role == null) throw missing();
        if (request.code() != null && !request.code().equals(role.code)
                && roleMapper.selectOne(new QueryWrapper<RoleEntity>().eq("code", request.code())) != null) {
            throw new BizException(BusinessError.ROLE_CODE_EXISTS, "角色代码已被其他角色使用", null);
        }
        if (request.code() != null) role.code = request.code();
        if (request.name() != null) role.name = request.name();
        if (request.description() != null) role.description = request.description();
        if (request.status() != null) role.status = request.status() == 1;
        role.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
        try {
            roleMapper.updateById(role);
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueViolation(exception)) throw roleCodeExists();
            throw exception;
        }
        if (request.menuIds() != null) replaceMenus(id, request.menuIds());
        return toResponse(role);
    }

    @Transactional
    public void delete(long id) {
        RoleEntity role = roleMapper.selectById(id);
        if (role == null) throw missing();
        if (SYSTEM_ROLES.contains(role.code)) {
            throw new BizException(BusinessError.SYSTEM_ROLE_PROTECTED);
        }
        long users = userRoleMapper.selectCount(new QueryWrapper<UserRoleEntity>().eq("role_id", id));
        if (users > 0) {
            throw new BizException(BusinessError.ROLE_IN_USE, "该角色下还有" + users + "个用户，无法删除", null);
        }
        roleMenuMapper.delete(new QueryWrapper<RoleMenuEntity>().eq("role_id", id));
        roleMapper.deleteById(id);
    }

    private void replaceMenus(long roleId, List<String> menuIds) {
        roleMenuMapper.delete(new QueryWrapper<RoleMenuEntity>().eq("role_id", roleId));
        if (menuIds == null) return;
        for (String menuId : menuIds.stream().distinct().toList()) {
            RoleMenuEntity assignment = new RoleMenuEntity();
            assignment.roleId = roleId;
            assignment.menuId = menuId;
            assignment.createdAt = LocalDateTime.now(ZoneOffset.UTC);
            roleMenuMapper.insert(assignment);
        }
    }

    private QueryWrapper<RoleEntity> roleQuery(String name, Integer status) {
        QueryWrapper<RoleEntity> query = new QueryWrapper<>();
        if (name != null && !name.isBlank()) query.and(w -> w.like("name", name.trim()).or().like("code", name.trim()));
        if (status != null) query.eq("status", status == 1);
        return query;
    }

    private RoleResponse toResponse(RoleEntity role) {
        List<String> menuIds = roleMenuMapper.selectList(new QueryWrapper<RoleMenuEntity>().eq("role_id", role.id))
                .stream().map(link -> link.menuId).toList();
        return new RoleResponse(role.id, role.code, role.name, role.description, Boolean.TRUE.equals(role.status) ? 1 : 0,
                menuIds, role.createdAt, role.updatedAt);
    }

    private BizException missing() {
        return new BizException(BusinessError.ROLE_NOT_FOUND);
    }

    private BizException roleCodeExists() {
        return new BizException(BusinessError.ROLE_CODE_EXISTS);
    }

    private boolean isUniqueViolation(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof DuplicateKeyException) return true;
            String message = current.getMessage();
            if (message != null && (message.toLowerCase().contains("duplicate")
                    || message.toLowerCase().contains("unique")
                    || message.toLowerCase().contains("idx_role_code"))) return true;
        }
        return false;
    }
}

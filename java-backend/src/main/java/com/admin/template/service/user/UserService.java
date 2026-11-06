package com.admin.template.service.user;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.mapper.RoleMapper;
import com.admin.template.mapper.UserMapper;
import com.admin.template.mapper.UserRoleMapper;
import com.admin.template.entity.RoleEntity;
import com.admin.template.entity.UserEntity;
import com.admin.template.entity.UserRoleEntity;
import com.admin.template.dto.user.UserCreateRequest;
import com.admin.template.dto.user.UserPageResponse;
import com.admin.template.dto.user.UserResponse;
import com.admin.template.dto.user.UserUpdateRequest;
import com.admin.template.dto.validation.SchemaInput;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    @Value("${app.user.default-password:123456}")
    private String defaultUserPassword = "123456";

    public UserService(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public UserResponse getUserById(long id) {
        UserEntity user = requireUser(id);
        return toResponse(user, false);
    }

    public UserPageResponse getUsers(int page, int pageSize, String username, Integer status) {
        QueryWrapper<UserEntity> query = userQuery(username, status).orderByDesc("id")
                .last("LIMIT " + pageSize + " OFFSET " + ((long) page - 1) * pageSize);
        List<UserEntity> users = userMapper.selectList(query);
        long total = userMapper.selectCount(userQuery(username, status));
        List<UserResponse> list = users.stream().map(user -> toResponse(user, true)).toList();
        return new UserPageResponse(list, new UserPageResponse.Pagination(page, pageSize, total,
                (int) Math.ceil((double) total / pageSize)));
    }

    public List<UserResponse> getExportData(String username, Integer status) {
        return userMapper.selectList(userQuery(username, status).orderByDesc("id")).stream()
                .map(user -> toResponse(user, true)).toList();
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userMapper.selectOne(new QueryWrapper<UserEntity>().eq("username", request.username())) != null) {
            throw new BizException(BusinessError.USERNAME_EXISTS);
        }
        UserEntity user = new UserEntity();
        user.username = request.username().trim();
        user.password = request.password() == null ? defaultUserPassword.trim() : request.password();
        if (user.password.length() < 6) throw SchemaInput.invalid("password", "密码至少需要6位字符", "too_small");
        user.nickname = request.nickname();
        user.email = request.email();
        user.phone = request.phone();
        user.avatar = request.avatar();
        user.status = request.status() == 1;
        user.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        user.updatedAt = user.createdAt;
        try {
            userMapper.insert(user);
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueViolation(exception)) throw usernameExists();
            throw exception;
        }
        replaceRoles(user.id, request.roleCodes());
        return toResponse(requireUser(user.id), false);
    }

    @Transactional
    public UserResponse updateUser(long id, UserUpdateRequest request) {
        UserEntity user = requireUser(id);
        if (request.username() != null
                && !request.username().equals(user.username)
                && userMapper.selectOne(new QueryWrapper<UserEntity>().eq("username", request.username())) != null) {
            throw new BizException(BusinessError.USERNAME_EXISTS);
        }
        if (request.username() != null) user.username = request.username();
        if (request.nickname() != null) user.nickname = request.nickname();
        if (request.email() != null) user.email = request.email();
        if (request.phone() != null) user.phone = request.phone();
        if (request.avatar() != null) user.avatar = request.avatar();
        if (request.status() != null) user.status = request.status() == 1;
        user.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
        try {
            userMapper.updateById(user);
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueViolation(exception)) throw usernameExists();
            throw exception;
        }
        if (request.roleCodes() != null) replaceRoles(id, request.roleCodes());
        return toResponse(requireUser(id), false);
    }

    @Transactional
    public void deleteUser(long id, long currentUserId) {
        UserEntity user = requireUser(id);
        if (id == currentUserId) {
            throw new BizException(BusinessError.CANNOT_DELETE_SELF);
        }
        userMapper.softDeleteActive(id, LocalDateTime.now(ZoneOffset.UTC));
    }

    private UserEntity requireUser(long id) {
        UserEntity user = userMapper.selectById(id);
        if (user == null) throw new BizException(BusinessError.USER_NOT_FOUND);
        return user;
    }

    private BizException usernameExists() {
        return new BizException(BusinessError.USERNAME_EXISTS);
    }

    private boolean isUniqueViolation(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof DuplicateKeyException) return true;
            String message = current.getMessage();
            if (message != null && (message.toLowerCase().contains("duplicate")
                    || message.toLowerCase().contains("unique")
                    || message.toLowerCase().contains("idx_username"))) return true;
        }
        return false;
    }

    private QueryWrapper<UserEntity> userQuery(String username, Integer status) {
        QueryWrapper<UserEntity> query = new QueryWrapper<>();
        if (username != null && !username.isBlank()) {
            query.and(wrapper -> wrapper.like("username", username.trim()).or().like("nickname", username.trim()));
        }
        if (status != null) query.eq("status", status == 1);
        return query;
    }

    private void replaceRoles(long userId, List<String> roleCodes) {
        userRoleMapper.delete(new QueryWrapper<UserRoleEntity>().eq("user_id", userId));
        if (roleCodes == null || roleCodes.isEmpty()) return;
        List<RoleEntity> roles = roleMapper.selectList(new QueryWrapper<RoleEntity>().in("code", roleCodes));
        for (RoleEntity role : roles) {
            UserRoleEntity assignment = new UserRoleEntity();
            assignment.userId = userId;
            assignment.roleId = role.id;
            assignment.createdAt = LocalDateTime.now(ZoneOffset.UTC);
            assignment.updatedAt = assignment.createdAt;
            userRoleMapper.insert(assignment);
        }
    }

    private UserResponse toResponse(UserEntity user, boolean includeLastLogin) {
        List<UserRoleEntity> assignments = userRoleMapper.selectList(
                new QueryWrapper<UserRoleEntity>().eq("user_id", user.id));
        List<Long> roleIds = assignments.stream().map(link -> link.roleId).toList();
        List<String> codes = roleIds.isEmpty() ? List.of() : roleMapper.selectBatchIds(roleIds).stream()
                .map(role -> role.code).toList();
        return new UserResponse(user.id, user.username, user.nickname, user.email, user.phone, user.avatar, codes,
                Boolean.TRUE.equals(user.status) ? 1 : 0, includeLastLogin ? user.lastLogin : null);
    }
}

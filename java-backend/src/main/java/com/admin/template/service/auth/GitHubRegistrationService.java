package com.admin.template.service.auth;

import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.dto.auth.LoginResponse;
import com.admin.template.entity.UserEntity;
import com.admin.template.entity.UserOAuthAccountEntity;
import com.admin.template.entity.UserRoleEntity;
import com.admin.template.mapper.RoleMapper;
import com.admin.template.mapper.UserMapper;
import com.admin.template.mapper.UserOAuthAccountMapper;
import com.admin.template.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class GitHubRegistrationService {
    private final UserMapper users;
    private final RoleMapper roles;
    private final UserRoleMapper userRoles;
    private final UserOAuthAccountMapper accounts;
    private final AuthService auth;

    public GitHubRegistrationService(UserMapper users, RoleMapper roles, UserRoleMapper userRoles,
                                     UserOAuthAccountMapper accounts, AuthService auth) {
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
        this.accounts = accounts;
        this.auth = auth;
    }

    @Transactional
    public LoginResponse register(GitHubOAuthClient.Profile profile) {
        var role = roles.selectActiveByCode("user");
        if (role == null) throw new BizException(BusinessError.ACCOUNT_WITHOUT_ROLE);
        var now = LocalDateTime.now(ZoneOffset.UTC);
        var user = new UserEntity();
        user.username = "github_" + UUID.randomUUID().toString().replace("-", "");
        user.nickname = profile.login().substring(0, Math.min(profile.login().length(), 50));
        user.status = true;
        user.createdAt = now;
        user.updatedAt = now;
        users.insert(user);

        var assignment = new UserRoleEntity();
        assignment.userId = user.id;
        assignment.roleId = role.id;
        assignment.createdAt = now;
        assignment.updatedAt = now;
        userRoles.insert(assignment);

        var account = new UserOAuthAccountEntity();
        account.userId = user.id;
        account.provider = "github";
        account.providerUserId = profile.id();
        account.providerUsername = profile.login();
        account.createdAt = now;
        account.updatedAt = now;
        accounts.insert(account);
        // Login permission checks join this transaction, so an unusable account is rolled back too.
        return auth.loginTrustedUser(user.id);
    }
}

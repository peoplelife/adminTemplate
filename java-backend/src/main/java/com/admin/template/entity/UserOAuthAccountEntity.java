package com.admin.template.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("user_oauth_accounts")
public class UserOAuthAccountEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long userId;
    public String provider;
    public String providerUserId;
    public String providerUsername;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}

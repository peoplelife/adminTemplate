package com.admin.template.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("users")
public class UserEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String username;
    public String password;
    public String nickname;
    public String email;
    public String phone;
    public String avatar;
    public Boolean status;
    @TableField("last_login")
    public LocalDateTime lastLogin;
    @TableField("last_logout")
    public LocalDateTime lastLogout;
    @TableField("created_at")
    public LocalDateTime createdAt;
    @TableField("updated_at")
    public LocalDateTime updatedAt;
    @TableLogic(value = "null", delval = "now()")
    @TableField("deleted_at")
    public LocalDateTime deletedAt;
}

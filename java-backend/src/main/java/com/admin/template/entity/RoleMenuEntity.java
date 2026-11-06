package com.admin.template.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("role_menus")
public class RoleMenuEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    @TableField("role_id")
    public Long roleId;
    @TableField("menu_id")
    public String menuId;
    @TableField("created_at")
    public LocalDateTime createdAt;
}

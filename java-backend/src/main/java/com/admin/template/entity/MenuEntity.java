package com.admin.template.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("menus")
public class MenuEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    @TableField("menu_id")
    public String menuId;
    @TableField("parent_id")
    public String parentId;
    public String name;
    public String path;
    public String component;
    public String icon;
    public Integer sort;
    public Boolean hidden;
    @TableField("is_full")
    public Boolean isFull;
    public String type;
    @TableField("buttonPermission")
    public String buttonPermission;
    public Boolean status;
    @TableField("created_at")
    public LocalDateTime createdAt;
    @TableField("updated_at")
    public LocalDateTime updatedAt;
}

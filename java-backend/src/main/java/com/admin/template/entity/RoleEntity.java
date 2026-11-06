package com.admin.template.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("roles")
public class RoleEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String name;
    public String code;
    public String description;
    public Boolean status;
    public Integer sort;
    @TableField("created_at")
    public LocalDateTime createdAt;
    @TableField("updated_at")
    public LocalDateTime updatedAt;
}

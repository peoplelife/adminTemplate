package com.admin.template.dto.menu;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

public record MenuResponse(
        @Schema(description = "菜单数据库 ID，当前用户菜单接口不返回该字段", example = "1")
        @JsonInclude(JsonInclude.Include.NON_NULL) Long id,
        @Schema(description = "菜单业务 ID", example = "menu_user") String menuId,
        @Schema(description = "父菜单业务 ID", example = "menu_system") String parentId,
        @Schema(description = "菜单名称", example = "用户管理") String name,
        @Schema(description = "前端路由路径", example = "/system/users") String path,
        @Schema(description = "前端页面组件路径", example = "system/users/index") String component,
        @Schema(description = "菜单图标", example = "user") String icon,
        @Schema(description = "排序值", example = "0") int sort,
        @Schema(description = "是否隐藏：0 显示，1 隐藏", minimum = "0", maximum = "1") int hidden,
        @Schema(description = "是否全屏：0 否，1 是", minimum = "0", maximum = "1") int isFull,
        @Schema(description = "类型：M 目录，C 页面，B 按钮", allowableValues = {"M", "C", "B"}) String type,
        @Schema(description = "按钮权限标识", example = "system:user:list") String buttonPermission) {
}

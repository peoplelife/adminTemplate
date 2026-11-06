package com.admin.template.dto.menu;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MenuListResponse(@Schema(description = "菜单列表") List<MenuResponse> list) {
}

package com.admin.template.mapper;

import com.admin.template.entity.MenuEntity;
import com.admin.template.dto.security.MenuPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<MenuEntity> {
    @Select("<script>SELECT m.menu_id, m.buttonPermission FROM role_menus rm "
            + "INNER JOIN menus m ON m.menu_id = rm.menu_id "
            + "WHERE rm.role_id IN <foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>"
            + "#{roleId}</foreach> AND m.status = 1</script>")
    @ConstructorArgs({
            @Arg(column = "menu_id", javaType = String.class),
            @Arg(column = "buttonPermission", javaType = String.class)
    })
    List<MenuPermission> selectActivePermissionsForRoleIds(@Param("roleIds") List<Long> roleIds);

    @Select("SELECT m.menu_id, m.buttonPermission FROM menus m WHERE m.status = 1")
    @ConstructorArgs({
            @Arg(column = "menu_id", javaType = String.class),
            @Arg(column = "buttonPermission", javaType = String.class)
    })
    List<MenuPermission> selectActivePermissionsForAllMenus();
}

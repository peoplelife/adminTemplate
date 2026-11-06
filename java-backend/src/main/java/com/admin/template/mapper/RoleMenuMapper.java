package com.admin.template.mapper;

import com.admin.template.entity.RoleMenuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuEntity> {
    @Select("SELECT menu_id FROM role_menus WHERE role_id = #{roleId} ORDER BY id")
    List<String> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}

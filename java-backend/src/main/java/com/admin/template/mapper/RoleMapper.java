package com.admin.template.mapper;

import com.admin.template.entity.RoleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {
    @Select("SELECT id, name, code, description, status, sort, created_at, updated_at "
            + "FROM roles WHERE code = #{code} AND status = 1")
    RoleEntity selectActiveByCode(@Param("code") String code);
}

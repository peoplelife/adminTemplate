package com.admin.template.mapper;

import com.admin.template.dto.security.ActiveRole;
import com.admin.template.entity.UserRoleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {
    @Select("SELECT r.id, r.code, r.name FROM user_roles ur "
            + "INNER JOIN roles r ON r.id = ur.role_id "
            + "INNER JOIN users u ON u.id = ur.user_id "
            + "WHERE ur.user_id = #{userId} AND r.status = 1 AND u.deleted_at IS NULL")
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "code", javaType = String.class),
            @Arg(column = "name", javaType = String.class)
    })
    List<ActiveRole> selectActiveRolesByUserId(@Param("userId") Long userId);
}

package com.admin.template.mapper;

import com.admin.template.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
    @Update("UPDATE users SET deleted_at = #{deletedAt}, updated_at = #{deletedAt} WHERE id = #{id} AND deleted_at IS NULL")
    int softDeleteActive(@Param("id") long id, @Param("deletedAt") LocalDateTime deletedAt);

    @Select("SELECT id, username, password, nickname, email, phone, avatar, status, last_login, last_logout, "
            + "created_at, updated_at, deleted_at FROM users WHERE username = #{username} AND deleted_at IS NULL")
    UserEntity selectActiveByUsername(@Param("username") String username);

    @Select("SELECT id, username, password, nickname, email, phone, avatar, status, last_login, last_logout, "
            + "created_at, updated_at, deleted_at FROM users WHERE id = #{id} AND deleted_at IS NULL")
    UserEntity selectActiveById(@Param("id") Long id);
}

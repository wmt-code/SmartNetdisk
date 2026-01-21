package com.wmt.smartnetdisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wmt.smartnetdisk.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper 接口
 *
 * @author wmt
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户实体
     */
    @Select("SELECT * FROM sys_user WHERE email = #{email} AND deleted = 0")
    User selectByEmail(@Param("email") String email);

    /**
     * 根据用户名或邮箱查询用户（登录用）
     *
     * @param account 用户名或邮箱
     * @return 用户实体
     */
    @Select("SELECT * FROM sys_user WHERE (username = #{account} OR email = #{account}) AND deleted = 0")
    User selectByUsernameOrEmail(@Param("account") String account);
}

package com.wmt.smartnetdisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wmt.smartnetdisk.entity.User;
import com.wmt.smartnetdisk.vo.UserVO;

/**
 * 用户服务接口
 *
 * @author wmt
 * @since 1.0.0
 */
public interface IUserService extends IService<User> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    User getByUsername(String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户实体
     */
    User getByEmail(String email);

    /**
     * 根据用户名或邮箱查询用户
     *
     * @param account 用户名或邮箱
     * @return 用户实体
     */
    User getByUsernameOrEmail(String account);

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否已存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 将用户实体转换为视图对象
     *
     * @param user 用户实体
     * @return 用户视图对象
     */
    UserVO toVO(User user);

    /**
     * 更新用户已用空间
     *
     * @param userId 用户ID
     * @param delta  空间变化量（正数增加，负数减少）
     */
    void updateUsedSpace(Long userId, Long delta);
}

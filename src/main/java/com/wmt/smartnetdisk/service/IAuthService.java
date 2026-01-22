package com.wmt.smartnetdisk.service;

import com.wmt.smartnetdisk.dto.request.LoginDTO;
import com.wmt.smartnetdisk.dto.request.RegisterDTO;
import com.wmt.smartnetdisk.vo.LoginVO;
import com.wmt.smartnetdisk.vo.UserVO;

/**
 * 认证服务接口
 *
 * @author wmt
 * @since 1.0.0
 */
public interface IAuthService {

    /**
     * 用户注册
     *
     * @param registerDTO 注册请求
     */
    void register(RegisterDTO registerDTO);

    /**
     * 用户登录
     *
     * @param loginDTO 登录请求
     * @return 登录响应（包含 Token 和用户信息）
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 用户退出登录
     */
    void logout();

    /**
     * 获取当前登录用户信息
     *
     * @return 当前用户信息
     */
    UserVO getCurrentUser();

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID，未登录返回 null
     */
    Long getCurrentUserId();

    /**
     * 检查当前是否已登录
     *
     * @return 是否已登录
     */
    boolean isLogin();

    /**
     * 刷新 Token
     * <p>
     * 续期当前登录的 Token 有效期，返回新的 Token 信息
     * </p>
     *
     * @return 刷新后的登录信息
     */
    LoginVO refreshToken();
}

package com.wmt.smartnetdisk.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.wmt.smartnetdisk.common.exception.BusinessException;
import com.wmt.smartnetdisk.common.result.ResultCode;
import com.wmt.smartnetdisk.dto.request.LoginDTO;
import com.wmt.smartnetdisk.dto.request.RegisterDTO;
import com.wmt.smartnetdisk.entity.User;
import com.wmt.smartnetdisk.service.IAuthService;
import com.wmt.smartnetdisk.service.IUserService;
import com.wmt.smartnetdisk.vo.LoginVO;
import com.wmt.smartnetdisk.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现类
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final IUserService userService;

    /**
     * BCrypt 密码编码器
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 默认用户空间大小：10GB
     */
    private static final long DEFAULT_TOTAL_SPACE = 10L * 1024 * 1024 * 1024;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        // 校验两次密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_VALID_ERROR, "两次密码不一致");
        }

        // 检查用户名是否已存在
        if (userService.existsByUsername(registerDTO.getUsername())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXIST, "用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userService.existsByEmail(registerDTO.getEmail())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXIST, "邮箱已被注册");
        }

        // 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        // BCrypt 加密密码
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setUsedSpace(0L);
        user.setTotalSpace(DEFAULT_TOTAL_SPACE);
        user.setStatus(1);

        boolean saved = userService.save(user);
        if (!saved) {
            throw new BusinessException(ResultCode.DATA_SAVE_FAIL, "注册失败，请稍后重试");
        }

        log.info("用户注册成功: username={}, email={}", user.getUsername(), user.getEmail());
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 根据用户名或邮箱查询用户
        User user = userService.getByUsernameOrEmail(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 检查账号状态
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // Sa-Token 登录
        if (Boolean.TRUE.equals(loginDTO.getRememberMe())) {
            // 记住登录：30天
            StpUtil.login(user.getId(), 30 * 24 * 60 * 60);
        } else {
            // 普通登录：使用配置的默认时间
            StpUtil.login(user.getId());
        }

        // 构建响应
        String token = StpUtil.getTokenValue();
        String tokenName = StpUtil.getTokenName();
        UserVO userVO = userService.toVO(user);

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return LoginVO.of(token, tokenName, userVO);
    }

    @Override
    public void logout() {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            StpUtil.logout();
            log.info("用户退出登录: userId={}", userId);
        }
    }

    @Override
    public UserVO getCurrentUser() {
        if (!isLogin()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Long userId = getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        return userService.toVO(user);
    }

    @Override
    public Long getCurrentUserId() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public boolean isLogin() {
        return StpUtil.isLogin();
    }

    @Override
    public LoginVO refreshToken() {
        // 检查是否登录
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 检查账号状态
        if (user.getStatus() != 1) {
            // 账号被禁用，强制退出
            StpUtil.logout();
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 获取当前 Token 的剩余有效期（秒）
        long tokenTimeout = StpUtil.getTokenTimeout();

        // 默认刷新后的有效期：7天（可根据业务调整）
        long newTimeout = 7 * 24 * 60 * 60L;

        // 如果当前 Token 剩余时间超过一半，则不刷新，避免频繁刷新
        if (tokenTimeout > newTimeout / 2) {
            log.debug("Token 剩余有效期充足，无需刷新: userId={}, remaining={}s", userId, tokenTimeout);
        } else {
            // 刷新 Token 有效期
            StpUtil.renewTimeout(newTimeout);
            log.info("Token 刷新成功: userId={}, newTimeout={}s", userId, newTimeout);
        }

        // 构建响应
        String token = StpUtil.getTokenValue();
        String tokenName = StpUtil.getTokenName();
        UserVO userVO = userService.toVO(user);

        return LoginVO.of(token, tokenName, userVO);
    }
}

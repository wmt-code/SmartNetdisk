package com.wmt.smartnetdisk.controller;

import com.wmt.smartnetdisk.common.result.Result;
import com.wmt.smartnetdisk.dto.request.LoginDTO;
import com.wmt.smartnetdisk.dto.request.RegisterDTO;
import com.wmt.smartnetdisk.service.IAuthService;
import com.wmt.smartnetdisk.vo.LoginVO;
import com.wmt.smartnetdisk.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author wmt
 * @since 1.0.0
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    /**
     * 用户注册
     *
     * @param registerDTO 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success("注册成功", null);
    }

    /**
     * 用户登录
     *
     * @param loginDTO 登录请求
     * @return Token 和用户信息
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = authService.login(loginDTO);
        return Result.success("登录成功", loginVO);
    }

    /**
     * 退出登录
     *
     * @return 退出结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success("退出成功", null);
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<UserVO> getCurrentUser() {
        UserVO userVO = authService.getCurrentUser();
        return Result.success(userVO);
    }

    /**
     * 检查登录状态
     *
     * @return 登录状态
     */
    @GetMapping("/check")
    public Result<Boolean> checkLogin() {
        return Result.success(authService.isLogin());
    }

    /**
     * 刷新 Token
     * <p>
     * 续期当前 Token 的有效期，返回更新后的 Token 信息
     * </p>
     *
     * @return 刷新后的登录信息
     */
    @PostMapping("/refresh")
    public Result<LoginVO> refreshToken() {
        LoginVO loginVO = authService.refreshToken();
        return Result.success("Token 刷新成功", loginVO);
    }
}

package com.wmt.smartnetdisk.controller;

import com.wmt.smartnetdisk.common.exception.BusinessException;
import com.wmt.smartnetdisk.common.result.Result;
import com.wmt.smartnetdisk.common.result.ResultCode;
import com.wmt.smartnetdisk.dto.request.ChangePasswordDTO;
import com.wmt.smartnetdisk.dto.request.UpdateUserDTO;
import com.wmt.smartnetdisk.entity.User;
import com.wmt.smartnetdisk.service.IAuthService;
import com.wmt.smartnetdisk.service.IUserService;
import com.wmt.smartnetdisk.utils.MinioUtils;
import com.wmt.smartnetdisk.vo.SpaceVO;
import com.wmt.smartnetdisk.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户控制器
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final IAuthService authService;
    private final MinioUtils minioUtils;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        return Result.success(authService.getCurrentUser());
    }

    /**
     * 更新用户信息
     *
     * @param updateDTO 更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/info")
    public Result<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserDTO updateDTO) {
        Long userId = authService.getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 更新用户名
        if (updateDTO.getUsername() != null && !updateDTO.getUsername().isBlank()) {
            // 检查用户名是否已被其他人使用
            User existUser = userService.getByUsername(updateDTO.getUsername());
            if (existUser != null && !existUser.getId().equals(userId)) {
                throw new BusinessException(ResultCode.USER_ALREADY_EXIST, "用户名已被使用");
            }
            user.setUsername(updateDTO.getUsername());
        }

        // 更新头像
        if (updateDTO.getAvatar() != null) {
            user.setAvatar(updateDTO.getAvatar());
        }

        userService.updateById(user);
        log.info("用户信息更新成功: userId={}", userId);
        return Result.success("更新成功", userService.toVO(user));
    }

    /**
     * 修改密码
     *
     * @param changeDTO 修改密码请求
     * @return 修改结果
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO changeDTO) {
        // 校验两次密码是否一致
        if (!changeDTO.getNewPassword().equals(changeDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_VALID_ERROR, "两次密码不一致");
        }

        Long userId = authService.getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 验证原密码
        if (!passwordEncoder.matches(changeDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "原密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(changeDTO.getNewPassword()));
        userService.updateById(user);

        log.info("用户密码修改成功: userId={}", userId);
        return Result.success("密码修改成功", null);
    }

    /**
     * 获取空间使用情况
     *
     * @return 空间使用情况
     */
    @GetMapping("/space")
    public Result<SpaceVO> getSpace() {
        Long userId = authService.getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        SpaceVO spaceVO = SpaceVO.of(user.getUsedSpace(), user.getTotalSpace());
        return Result.success(spaceVO);
    }
}

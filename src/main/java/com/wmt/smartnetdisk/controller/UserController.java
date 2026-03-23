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
import java.util.Set;

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
    @GetMapping({"/info", "/profile"})
    public Result<UserVO> getUserInfo() {
        return Result.success(authService.getCurrentUser());
    }

    /**
     * 更新用户信息
     *
     * @param updateDTO 更新请求
     * @return 更新后的用户信息
     */
    @PutMapping({"/info", "/profile"})
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
     * 上传头像
     *
     * @param file 头像文件
     * @return 头像URL
     */
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_VALID_ERROR, "请选择要上传的头像");
        }

        // 校验文件大小（最大2MB）
        long maxAvatarSize = 2 * 1024 * 1024L;
        if (file.getSize() > maxAvatarSize) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEED, "头像大小不能超过2MB");
        }

        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase()
                : "";
        Set<String> allowedExtensions = Set.of("jpg", "jpeg", "png", "gif", "webp");
        if (!allowedExtensions.contains(ext)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED, "头像仅支持 jpg、jpeg、png、gif、webp 格式");
        }

        Long userId = authService.getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 保存旧头像路径，用于后续删除
        String oldAvatarPath = user.getAvatar();

        // 上传新头像
        String avatarPath = minioUtils.uploadAvatar(file, userId);

        // 生成头像访问URL（有效期7天）
        String avatarUrl = minioUtils.getAvatarPresignedUrl(avatarPath, 7 * 24 * 60 * 60);

        // 更新用户头像路径
        user.setAvatar(avatarPath);
        userService.updateById(user);

        // 删除旧头像（异步删除，不影响响应）
        if (oldAvatarPath != null && !oldAvatarPath.isBlank()) {
            minioUtils.deleteAvatar(oldAvatarPath);
        }

        log.info("用户头像上传成功: userId={}, path={}", userId, avatarPath);

        Map<String, String> data = new HashMap<>();
        data.put("avatarPath", avatarPath);
        data.put("avatarUrl", avatarUrl);
        return Result.success("头像上传成功", data);
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

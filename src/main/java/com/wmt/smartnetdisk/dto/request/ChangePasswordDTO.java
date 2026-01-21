package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class ChangePasswordDTO {

    /**
     * 原密码
     */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20位")
    private String newPassword;

    /**
     * 确认新密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}

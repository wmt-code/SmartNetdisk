package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class LoginDTO {

    /**
     * 用户名或邮箱
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20位")
    private String password;

    /**
     * 是否记住登录（延长token有效期）
     */
    private Boolean rememberMe = false;
}

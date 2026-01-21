package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户信息请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class UpdateUserDTO {

    /**
     * 用户名（可选更新）
     */
    @Size(min = 3, max = 20, message = "用户名长度为3-20位")
    private String username;

    /**
     * 头像 URL（可选更新）
     */
    private String avatar;
}

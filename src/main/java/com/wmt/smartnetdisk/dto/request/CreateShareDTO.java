package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建分享请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class CreateShareDTO {

    /**
     * 要分享的文件ID
     */
    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    /**
     * 提取码（可选，不填则生成随机码）
     */
    private String password;

    /**
     * 有效天数（默认7天，0表示永久）
     */
    private Integer expireDays = 7;

    /**
     * 最大访问次数（可选，0表示无限制）
     */
    private Integer maxViewCount = 0;
}

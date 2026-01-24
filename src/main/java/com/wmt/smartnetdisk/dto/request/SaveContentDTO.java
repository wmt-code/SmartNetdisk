package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 保存文件内容请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class SaveContentDTO {

    /**
     * 文件内容
     */
    @NotNull(message = "内容不能为空")
    private String content;
}

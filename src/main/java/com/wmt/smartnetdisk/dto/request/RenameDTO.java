package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重命名请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class RenameDTO {

    /**
     * 新名称
     */
    @NotBlank(message = "名称不能为空")
    @Size(max = 200, message = "名称不能超过200字符")
    private String name;
}

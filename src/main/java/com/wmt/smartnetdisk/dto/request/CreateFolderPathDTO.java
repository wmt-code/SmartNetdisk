package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 按路径创建文件夹请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class CreateFolderPathDTO {

    /**
     * 文件夹路径 (例如: "a/b/c")
     */
    @NotBlank(message = "文件夹路径不能为空")
    private String path;

    /**
     * 父文件夹ID（0表示根目录）
     */
    private Long parentId = 0L;
}

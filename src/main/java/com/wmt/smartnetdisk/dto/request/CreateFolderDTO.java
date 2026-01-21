package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建文件夹请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class CreateFolderDTO {

    /**
     * 文件夹名称
     */
    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 100, message = "文件夹名称不能超过100字符")
    private String folderName;

    /**
     * 父文件夹ID（0表示根目录）
     */
    private Long parentId = 0L;
}

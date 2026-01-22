package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 复制请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class CopyDTO {

    /**
     * 目标文件夹ID（0表示根目录）
     */
    @NotNull(message = "目标文件夹不能为空")
    private Long targetFolderId;

    /**
     * 要复制的文件ID列表（批量复制时使用）
     */
    private List<Long> fileIds;
}

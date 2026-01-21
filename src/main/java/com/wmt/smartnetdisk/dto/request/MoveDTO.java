package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 移动请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class MoveDTO {

    /**
     * 目标文件夹ID（0表示根目录）
     */
    @NotNull(message = "目标文件夹不能为空")
    private Long targetFolderId;

    /**
     * 要移动的文件ID列表（批量移动时使用）
     */
    private List<Long> fileIds;
}

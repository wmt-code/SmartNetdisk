package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分片合并请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class ChunkMergeDTO {

    /**
     * 文件 MD5
     */
    @NotBlank(message = "文件MD5不能为空")
    private String fileMd5;

    /**
     * 原始文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 总分片数
     */
    @NotNull(message = "总分片数不能为空")
    @Min(value = 1, message = "总分片数不能小于1")
    private Integer totalChunks;

    /**
     * 文件总大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小不能小于1")
    private Long totalSize;

    /**
     * 目标文件夹ID（0表示根目录）
     */
    private Long folderId = 0L;
}

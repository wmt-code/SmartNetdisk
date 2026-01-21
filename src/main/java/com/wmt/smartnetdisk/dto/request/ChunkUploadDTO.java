package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分片上传请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class ChunkUploadDTO {

    /**
     * 文件 MD5
     */
    @NotBlank(message = "文件MD5不能为空")
    private String fileMd5;

    /**
     * 分片索引（从0开始）
     */
    @NotNull(message = "分片索引不能为空")
    @Min(value = 0, message = "分片索引不能小于0")
    private Integer chunkIndex;

    /**
     * 总分片数
     */
    @NotNull(message = "总分片数不能为空")
    @Min(value = 1, message = "总分片数不能小于1")
    private Integer totalChunks;

    /**
     * 分片大小（字节）
     */
    @NotNull(message = "分片大小不能为空")
    @Min(value = 1, message = "分片大小不能小于1")
    private Long chunkSize;
}

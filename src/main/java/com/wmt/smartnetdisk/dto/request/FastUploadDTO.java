package com.wmt.smartnetdisk.dto.request;

import lombok.Data;

/**
 * 秒传检测请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class FastUploadDTO {

    /**
     * 文件 MD5
     */
    private String fileMd5;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 目标文件夹ID
     */
    private Long folderId = 0L;
}

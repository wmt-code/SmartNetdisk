package com.wmt.smartnetdisk.dto.request;

import com.wmt.smartnetdisk.common.result.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件列表查询 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileListDTO extends PageRequest {

    /**
     * 所属文件夹ID（0表示根目录）
     */
    private Long folderId = 0L;

    /**
     * 文件类型过滤（image/video/audio/document/other）
     */
    private String fileType;

    /**
     * 搜索关键词（文件名）
     */
    private String keyword;

    /**
     * 是否查询回收站（默认 false）
     */
    private Boolean recycled = false;
}

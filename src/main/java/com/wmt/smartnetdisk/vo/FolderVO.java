package com.wmt.smartnetdisk.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件夹视图对象
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class FolderVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件夹ID
     */
    private Long id;

    /**
     * 文件夹名称
     */
    private String folderName;

    /**
     * 父文件夹ID
     */
    private Long parentId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 子文件夹列表（用于树形结构）
     */
    private List<FolderVO> children;
}

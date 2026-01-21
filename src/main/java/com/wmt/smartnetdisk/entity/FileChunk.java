package com.wmt.smartnetdisk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件分片实体类
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
@TableName("file_chunk")
public class FileChunk implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分片ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件MD5
     */
    private String fileMd5;

    /**
     * 分片索引（从0开始）
     */
    private Integer chunkIndex;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 分片大小（字节）
     */
    private Long chunkSize;

    /**
     * 分片存储路径
     */
    private String chunkPath;

    /**
     * 状态: 0-上传中, 1-已完成
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

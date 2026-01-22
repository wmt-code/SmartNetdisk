package com.wmt.smartnetdisk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件信息实体类
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
@TableName("file_info")
public class FileInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 所属文件夹ID, 0表示根目录
     */
    private Long folderId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件MD5
     */
    private String fileMd5;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型: image/video/audio/document/other
     */
    private String fileType;

    /**
     * 文件扩展名
     */
    private String fileExt;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 存储路径（MinIO对象路径）
     */
    private String storagePath;

    /**
     * 缩略图路径
     */
    private String thumbnailPath;

    /**
     * 是否已向量化: 0-否, 1-是
     */
    private Integer isVectorized;

    /**
     * 状态: 0-上传中, 1-正常, 2-转码中
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除: 0-未删除, 1-已删除
     * 注意：不使用 @TableLogic，手动管理删除逻辑以支持回收站功能
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    /**
     * 删除时间（进入回收站的时间）
     */
    private LocalDateTime deleteTime;
}

package com.wmt.smartnetdisk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分享项明细实体类
 *
 * @author wmt
 * @since 2.0.0
 */
@Data
@TableName("share_item")
public class ShareItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分享项ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的分享ID
     */
    private Long shareId;

    /**
     * 项类型: 0-文件, 1-文件夹
     */
    private Integer itemType;

    /**
     * 文件ID（当itemType=0时）
     */
    private Long fileId;

    /**
     * 文件夹ID（当itemType=1时）
     */
    private Long folderId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

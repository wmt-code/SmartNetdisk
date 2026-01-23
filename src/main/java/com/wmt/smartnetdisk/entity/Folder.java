package com.wmt.smartnetdisk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件夹实体类
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
@TableName("folder")
public class Folder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件夹ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 父文件夹ID, 0表示根目录
     */
    private Long parentId;

    /**
     * 文件夹名称
     */
    private String folderName;

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

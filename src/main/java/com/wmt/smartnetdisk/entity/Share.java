package com.wmt.smartnetdisk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分享实体类
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
@TableName("share")
public class Share implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分享ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分享者用户ID
     */
    private Long userId;

    /**
     * 被分享的文件ID
     */
    private Long fileId;

    /**
     * 分享短码（唯一）
     */
    private String shareCode;

    /**
     * 提取密码（可选）
     */
    private String password;

    /**
     * 过期时间，NULL表示永久
     */
    private LocalDateTime expireTime;

    /**
     * 访问次数
     */
    private Integer viewCount;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 最大访问次数限制，NULL表示无限制
     */
    private Integer maxViewCount;

    /**
     * 状态: 0-已取消, 1-有效, 2-已过期
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

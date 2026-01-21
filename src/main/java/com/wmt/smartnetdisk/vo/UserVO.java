package com.wmt.smartnetdisk.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户视图对象（脱敏）
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱（部分脱敏）
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 已用空间（字节）
     */
    private Long usedSpace;

    /**
     * 总空间（字节）
     */
    private Long totalSpace;

    /**
     * 空间使用百分比
     */
    private Double usedPercent;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

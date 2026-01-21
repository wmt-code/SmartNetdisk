package com.wmt.smartnetdisk.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分享视图对象
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class ShareVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分享ID
     */
    private Long id;

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件大小（格式化）
     */
    private String fileSizeStr;

    /**
     * 分享码
     */
    private String shareCode;

    /**
     * 分享链接
     */
    private String shareUrl;

    /**
     * 是否有密码
     */
    private Boolean hasPassword;

    /**
     * 提取码（仅创建时返回）
     */
    private String password;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 是否已过期
     */
    private Boolean expired;

    /**
     * 访问次数
     */
    private Integer viewCount;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 最大访问次数（0表示无限制）
     */
    private Integer maxViewCount;

    /**
     * 状态：0-已取消，1-有效，2-已过期
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

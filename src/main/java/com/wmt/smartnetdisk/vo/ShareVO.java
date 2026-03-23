package com.wmt.smartnetdisk.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

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
     * 分享类型: 0-单文件, 1-目录, 2-批量
     */
    private Integer shareType;

    /**
     * 文件ID（单文件分享时使用）
     */
    private Long fileId;

    /**
     * 文件夹ID（目录分享时使用）
     */
    private Long folderId;

    /**
     * 文件夹名称（目录分享时使用）
     */
    private String folderName;

    /**
     * 分享标题（批量分享时的自定义标题）
     */
    private String shareTitle;

    /**
     * 文件名（单文件分享时使用）
     */
    private String fileName;

    /**
     * 文件大小（单文件时使用）
     */
    private Long fileSize;

    /**
     * 分享总大小（字节）
     */
    private Long totalSize;

    /**
     * 文件大小（格式化）
     */
    private String fileSizeStr;

    /**
     * 文件类型（扩展名）
     */
    private String fileType;

    /**
     * 包含的文件数量
     */
    private Integer fileCount;

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

    /**
     * 分享者用户名
     */
    private String sharerUsername;

    /**
     * 分享者头像
     */
    private String sharerAvatar;

    /**
     * 分享项列表（批量分享时使用）
     */
    private List<ShareItemVO> items;

    /**
     * 分享项视图对象
     */
    @Data
    public static class ShareItemVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 项类型: 0-文件, 1-文件夹
         */
        private Integer itemType;

        /**
         * 文件ID
         */
        private Long fileId;

        /**
         * 文件夹ID
         */
        private Long folderId;

        /**
         * 文件/文件夹名称
         */
        private String name;

        /**
         * 大小（字节）
         */
        private Long size;

        /**
         * 大小（格式化）
         */
        private String sizeStr;

        /**
         * 文件类型（文件时使用）
         */
        private String fileType;

        /**
         * 包含的子项数量（文件夹时使用）
         */
        private Integer childCount;
    }
}

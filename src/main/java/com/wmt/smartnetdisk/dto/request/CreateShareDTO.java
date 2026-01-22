package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建分享请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class CreateShareDTO {

    /**
     * 分享类型（0-单文件, 1-目录, 2-批量）
     */
    @NotNull(message = "分享类型不能为空")
    private Integer shareType = 0;

    /**
     * 要分享的文件ID（单文件分享时使用）
     */
    private Long fileId;

    /**
     * 要分享的文件夹ID（目录分享时使用）
     */
    private Long folderId;

    /**
     * 分享项列表（批量分享时使用）
     */
    private List<ShareItemDTO> items;

    /**
     * 分享标题（批量分享时的自定义标题，可选）
     */
    private String shareTitle;

    /**
     * 提取码（可选，不填则生成随机码）
     */
    private String password;

    /**
     * 有效天数（默认7天，0表示永久）
     */
    private Integer expireDays = 7;

    /**
     * 最大访问次数（可选，0表示无限制）
     */
    private Integer maxViewCount = 0;

    /**
     * 分享项 DTO（用于批量分享）
     */
    @Data
    public static class ShareItemDTO {
        /**
         * 项类型（0-文件, 1-文件夹）
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
    }
}


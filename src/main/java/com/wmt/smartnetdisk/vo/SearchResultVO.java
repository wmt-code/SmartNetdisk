package com.wmt.smartnetdisk.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 语义搜索结果视图对象
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class SearchResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 搜索结果列表
     */
    private List<SearchItem> items;

    /**
     * 搜索耗时（毫秒）
     */
    private Long costMs;

    @Data
    public static class SearchItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 文件ID
         */
        private Long fileId;

        /**
         * 文件名
         */
        private String fileName;

        /**
         * 匹配的文本片段
         */
        private String content;

        /**
         * 相似度分数
         */
        private Double score;

        /**
         * 在文档中的块索引
         */
        private Integer chunkIndex;
    }
}

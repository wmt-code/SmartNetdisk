package com.wmt.smartnetdisk.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 智能问答结果视图对象
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class ChatResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * AI 回答
     */
    private String answer;

    /**
     * 引用的文档来源
     */
    private List<Source> sources;

    /**
     * 耗时（毫秒）
     */
    private Long costMs;

    @Data
    public static class Source implements Serializable {
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
         * 引用的文本片段
         */
        private String content;
    }
}

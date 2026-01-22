package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 语义搜索请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class SemanticSearchDTO {

    /**
     * 搜索查询
     */
    @NotBlank(message = "搜索内容不能为空")
    private String query;

    /**
     * 返回结果数量（默认5）
     */
    private Integer topK = 5;

    /**
     * 最小相似度阈值（默认0.2，降低以提高召回率）
     */
    private Double minScore = 0.2;
}

package com.wmt.smartnetdisk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务配置类
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.siliconflow")
public class AiConfig {

    /**
     * API Key（对话模型）
     */
    private String apiKey;

    /**
     * API 基础 URL
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /**
     * 对话模型
     */
    private String model = "tongyi-xiaomi-analysis-pro";

    /**
     * Embedding API Key（可选，为空则使用 apiKey）
     */
    private String embeddingApiKey;

    /**
     * Embedding API 基础 URL（可选，为空则使用 baseUrl）
     */
    private String embeddingBaseUrl;

    /**
     * 向量化模型
     */
    private String embeddingModel = "text-embedding-v3";

    /**
     * 向量维度
     */
    private int embeddingDimension = 1024;

    /**
     * 获取 Embedding 使用的 API Key
     */
    public String getEffectiveEmbeddingApiKey() {
        return (embeddingApiKey != null && !embeddingApiKey.isBlank()) ? embeddingApiKey : apiKey;
    }

    /**
     * 获取 Embedding 使用的 Base URL
     */
    public String getEffectiveEmbeddingBaseUrl() {
        return (embeddingBaseUrl != null && !embeddingBaseUrl.isBlank()) ? embeddingBaseUrl : baseUrl;
    }

    /**
     * 文档分块大小（字符数）
     */
    private int chunkSize = 2000;

    /**
     * 分块重叠大小
     */
    private int chunkOverlap = 200;

    /**
     * 支持向量化的文件扩展名
     */
    private java.util.List<String> vectorizableFileExtensions = java.util.Arrays.asList("txt", "md", "pdf", "doc",
            "docx");
}

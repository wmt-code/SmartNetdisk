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
     * API Key
     */
    private String apiKey;

    /**
     * API 基础 URL
     */
    private String baseUrl = "https://api.siliconflow.cn/v1";

    /**
     * 对话模型
     */
    private String model = "Qwen/Qwen2.5-7B-Instruct";

    /**
     * 向量化模型
     */
    private String embeddingModel = "BAAI/bge-large-zh-v1.5";

    /**
     * 向量维度（bge-large-zh-v1.5 为 1024）
     */
    private int embeddingDimension = 1024;

    /**
     * 文档分块大小（字符数）
     */
    private int chunkSize = 500;

    /**
     * 分块重叠大小
     */
    private int chunkOverlap = 50;

    /**
     * 支持向量化的文件扩展名
     */
    private java.util.List<String> vectorizableFileExtensions = java.util.Arrays.asList("txt", "md", "pdf", "doc",
            "docx");
}

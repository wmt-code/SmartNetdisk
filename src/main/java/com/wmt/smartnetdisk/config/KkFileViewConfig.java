package com.wmt.smartnetdisk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * kkFileView 配置类
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "kkfileview")
public class KkFileViewConfig {

    /**
     * kkFileView 服务基础 URL
     * 例如: http://localhost:8012
     */
    private String baseUrl;

    /**
     * 获取文件预览 URL
     * 
     * @param fileUrl 文件访问 URL（需要是 kkFileView 可访问的 URL）
     * @return kkFileView 预览页面 URL
     */
    public String getPreviewUrl(String fileUrl) {
        // kkFileView 要求: encodeURIComponent(base64Encode(url))
        // 1. 先 Base64 编码
        String base64Url = Base64.getEncoder().encodeToString(fileUrl.getBytes(StandardCharsets.UTF_8));
        // 2. 再 URL 编码
        String encodedUrl = java.net.URLEncoder.encode(base64Url, StandardCharsets.UTF_8);
        return baseUrl + "/onlinePreview?url=" + encodedUrl;
    }
}

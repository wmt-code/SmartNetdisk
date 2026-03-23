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
     * kkFileView 服务基础 URL（浏览器访问用）
     * 例如: http://localhost:8012
     */
    private String baseUrl;

    /**
     * 后端回调 URL（kkFileView 容器访问后端用）
     * 本地开发时 kkFileView 在 Docker 内，需通过 host.docker.internal 访问宿主机后端
     * 例如: http://host.docker.internal:8081
     * 生产环境可设为后端的内网/公网地址
     * 如果为空，则使用 MinIO presigned URL（适用于 kkFileView 可直接访问 MinIO 的场景）
     */
    private String callbackUrl;

    /**
     * 获取文件预览 URL
     *
     * @param fileUrl 文件访问 URL（需要是 kkFileView 可访问的 URL）
     * @return kkFileView 预览页面 URL
     */
    public String getPreviewUrl(String fileUrl) {
        String base64Url = Base64.getEncoder().encodeToString(fileUrl.getBytes(StandardCharsets.UTF_8));
        String encodedUrl = java.net.URLEncoder.encode(base64Url, StandardCharsets.UTF_8);
        return baseUrl + "/onlinePreview?url=" + encodedUrl;
    }
}

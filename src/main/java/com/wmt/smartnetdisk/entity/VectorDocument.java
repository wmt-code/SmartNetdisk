package com.wmt.smartnetdisk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 向量文档实体类
 * <p>
 * 用于存储文档向量化后的分块内容和向量数据
 * 注意：embedding 字段使用 pgvector 类型，在 Java 中作为 Object 处理
 * </p>
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
@TableName("vector_document")
public class VectorDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 向量文档ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的文件ID
     */
    private Long fileId;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 分块索引（从0开始）
     */
    private Integer chunkIndex;

    /**
     * 分块文本内容
     */
    private String content;

    /**
     * 向量数据（pgvector类型）
     * <p>
     * 存储 Embedding 向量，维度根据模型配置
     * 在 Java 中作为 String 或 float[] 处理
     * </p>
     */
    @TableField(exist = false)
    private float[] embedding;

    /**
     * 向量数据字符串形式（用于数据库存取）
     */
    @TableField("embedding")
    private String embeddingStr;

    /**
     * Token 数量
     */
    private Integer tokenCount;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 将 float[] 转换为 pgvector 格式字符串
     *
     * @param vector float 数组
     * @return pgvector 格式字符串，如 [0.1,0.2,0.3]
     */
    public static String vectorToString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 将 pgvector 格式字符串转换为 float[]
     *
     * @param vectorStr pgvector 格式字符串
     * @return float 数组
     */
    public static float[] stringToVector(String vectorStr) {
        if (vectorStr == null || vectorStr.isBlank()) {
            return null;
        }
        // 移除方括号
        String content = vectorStr.substring(1, vectorStr.length() - 1);
        String[] parts = content.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return vector;
    }
}

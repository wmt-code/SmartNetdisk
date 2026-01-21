package com.wmt.smartnetdisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wmt.smartnetdisk.entity.VectorDocument;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 向量文档 Mapper 接口
 *
 * @author wmt
 * @since 1.0.0
 */
@Mapper
public interface VectorDocumentMapper extends BaseMapper<VectorDocument> {

    /**
     * 根据文件ID查询所有向量块
     *
     * @param fileId 文件ID
     * @return 向量文档列表
     */
    @Select("SELECT * FROM vector_document WHERE file_id = #{fileId} ORDER BY chunk_index")
    List<VectorDocument> selectByFileId(@Param("fileId") Long fileId);

    /**
     * 根据文件ID删除所有向量块
     *
     * @param fileId 文件ID
     * @return 影响行数
     */
    @Delete("DELETE FROM vector_document WHERE file_id = #{fileId}")
    int deleteByFileId(@Param("fileId") Long fileId);

    /**
     * 检查文件是否已向量化
     *
     * @param fileId 文件ID
     * @return 向量块数量
     */
    @Select("SELECT COUNT(*) FROM vector_document WHERE file_id = #{fileId}")
    int countByFileId(@Param("fileId") Long fileId);

    /**
     * 向量相似度搜索（使用 pgvector）
     * 注意：此方法需要 PostgreSQL 安装 pgvector 扩展
     *
     * @param userId    用户ID
     * @param embedding 查询向量（需转换为 pgvector 格式）
     * @param limit     返回数量
     * @param minScore  最小相似度
     * @return 相似文档列表
     */
    @Select("""
            SELECT *, 1 - (embedding <=> #{embedding}::vector) as similarity
            FROM vector_document
            WHERE user_id = #{userId}
            AND 1 - (embedding <=> #{embedding}::vector) >= #{minScore}
            ORDER BY embedding <=> #{embedding}::vector
            LIMIT #{limit}
            """)
    @Results({
            @Result(property = "similarity", column = "similarity")
    })
    List<VectorDocument> searchSimilar(
            @Param("userId") Long userId,
            @Param("embedding") String embedding,
            @Param("limit") int limit,
            @Param("minScore") double minScore);

    /**
     * 在指定文件中搜索
     */
    @Select("""
            SELECT *, 1 - (embedding <=> #{embedding}::vector) as similarity
            FROM vector_document
            WHERE user_id = #{userId}
            AND file_id IN (${fileIds})
            AND 1 - (embedding <=> #{embedding}::vector) >= #{minScore}
            ORDER BY embedding <=> #{embedding}::vector
            LIMIT #{limit}
            """)
    List<VectorDocument> searchSimilarInFiles(
            @Param("userId") Long userId,
            @Param("fileIds") String fileIds,
            @Param("embedding") String embedding,
            @Param("limit") int limit,
            @Param("minScore") double minScore);
}

package com.wmt.smartnetdisk.service;

import com.wmt.smartnetdisk.dto.request.ChatDTO;
import com.wmt.smartnetdisk.dto.request.SemanticSearchDTO;
import com.wmt.smartnetdisk.vo.ChatResultVO;
import com.wmt.smartnetdisk.vo.SearchResultVO;

/**
 * AI 服务接口
 *
 * @author wmt
 * @since 1.0.0
 */
public interface IAiService {

    /**
     * 文档向量化
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     */
    void vectorizeDocument(Long userId, Long fileId);

    /**
     * 获取向量化状态
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     * @return 状态信息（isVectorized, chunkCount）
     */
    VectorizeStatus getVectorizeStatus(Long userId, Long fileId);

    /**
     * 语义搜索
     *
     * @param userId    用户ID
     * @param searchDTO 搜索请求
     * @return 搜索结果
     */
    SearchResultVO semanticSearch(Long userId, SemanticSearchDTO searchDTO);

    /**
     * 智能问答（RAG）
     *
     * @param userId  用户ID
     * @param chatDTO 问答请求
     * @return 问答结果
     */
    ChatResultVO chat(Long userId, ChatDTO chatDTO);

    /**
     * 生成文档摘要
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     * @return 文档摘要
     */
    String generateSummary(Long userId, Long fileId);

    /**
     * 异步文档向量化（用于文件上传后自动触发）
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     */
    void vectorizeDocumentAsync(Long userId, Long fileId);

    /**
     * 检查文件是否支持向量化
     *
     * @param fileExt 文件扩展名
     * @return 是否支持
     */
    boolean isFileVectorizable(String fileExt);

    /**
     * 向量化状态
     */
    record VectorizeStatus(boolean isVectorized, int chunkCount) {
    }
}

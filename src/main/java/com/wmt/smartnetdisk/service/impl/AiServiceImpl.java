package com.wmt.smartnetdisk.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wmt.smartnetdisk.common.exception.BusinessException;
import com.wmt.smartnetdisk.common.result.ResultCode;
import com.wmt.smartnetdisk.config.AiConfig;
import com.wmt.smartnetdisk.dto.request.ChatDTO;
import com.wmt.smartnetdisk.dto.request.SemanticSearchDTO;
import com.wmt.smartnetdisk.entity.FileInfo;
import com.wmt.smartnetdisk.entity.VectorDocument;
import com.wmt.smartnetdisk.mapper.VectorDocumentMapper;
import com.wmt.smartnetdisk.service.IAiService;
import com.wmt.smartnetdisk.service.IFileService;
import com.wmt.smartnetdisk.utils.MinioUtils;
import com.wmt.smartnetdisk.vo.ChatResultVO;
import com.wmt.smartnetdisk.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 服务实现类
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements IAiService {

    private final AiConfig aiConfig;
    private final IFileService fileService;
    private final MinioUtils minioUtils;
    private final VectorDocumentMapper vectorDocumentMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void vectorizeDocument(Long userId, Long fileId) {
        // 验证文件权限
        FileInfo fileInfo = fileService.getFileWithPermission(userId, fileId);

        // 检查文件类型是否支持向量化
        if (!isVectorizable(fileInfo.getFileExt())) {
            throw new BusinessException(ResultCode.AI_VECTORIZE_UNSUPPORTED);
        }

        // 删除旧的向量数据
        vectorDocumentMapper.deleteByFileId(fileId);

        // 下载并读取文件内容
        String content = readFileContent(fileInfo);
        if (content == null || content.isBlank()) {
            throw new BusinessException(ResultCode.AI_VECTORIZE_FAIL, "文件内容为空");
        }

        // 分块处理
        List<String> chunks = splitIntoChunks(content);
        log.info("文件分块完成: fileId={}, chunks={}", fileId, chunks.size());

        // 批量向量化并保存
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            float[] embedding = getEmbedding(chunk);

            VectorDocument doc = new VectorDocument();
            doc.setFileId(fileId);
            doc.setUserId(userId);
            doc.setContent(chunk);
            doc.setChunkIndex(i);
            doc.setTokenCount(chunk.length()); // 简化处理
            doc.setEmbeddingStr(VectorDocument.vectorToString(embedding));

            vectorDocumentMapper.insert(doc);
        }

        // 更新文件向量化状态
        fileInfo.setIsVectorized(1);
        fileService.updateById(fileInfo);

        log.info("文档向量化完成: fileId={}, chunks={}", fileId, chunks.size());
    }

    @Override
    public VectorizeStatus getVectorizeStatus(Long userId, Long fileId) {
        fileService.getFileWithPermission(userId, fileId);
        int count = vectorDocumentMapper.countByFileId(fileId);
        return new VectorizeStatus(count > 0, count);
    }

    @Override
    public SearchResultVO semanticSearch(Long userId, SemanticSearchDTO searchDTO) {
        long startTime = System.currentTimeMillis();

        // 获取查询向量
        float[] queryEmbedding = getEmbedding(searchDTO.getQuery());
        String embeddingStr = VectorDocument.vectorToString(queryEmbedding);

        // 执行向量搜索
        List<VectorDocument> results = vectorDocumentMapper.searchSimilar(
                userId,
                embeddingStr,
                searchDTO.getTopK(),
                searchDTO.getMinScore());

        // 构建返回结果
        SearchResultVO resultVO = new SearchResultVO();
        List<SearchResultVO.SearchItem> items = new ArrayList<>();

        for (VectorDocument doc : results) {
            SearchResultVO.SearchItem item = new SearchResultVO.SearchItem();
            item.setFileId(doc.getFileId());
            item.setContent(doc.getContent());
            item.setChunkIndex(doc.getChunkIndex());
            // 查询文件名
            FileInfo fileInfo = fileService.getById(doc.getFileId());
            if (fileInfo != null) {
                item.setFileName(fileInfo.getFileName());
            }
            items.add(item);
        }

        resultVO.setItems(items);
        resultVO.setCostMs(System.currentTimeMillis() - startTime);
        return resultVO;
    }

    @Override
    public ChatResultVO chat(Long userId, ChatDTO chatDTO) {
        long startTime = System.currentTimeMillis();

        // 先进行语义搜索获取相关文档
        SemanticSearchDTO searchDTO = new SemanticSearchDTO();
        searchDTO.setQuery(chatDTO.getQuestion());
        searchDTO.setTopK(5);
        searchDTO.setMinScore(0.3);

        SearchResultVO searchResult = semanticSearch(userId, searchDTO);

        // 构建上下文
        StringBuilder context = new StringBuilder();
        List<ChatResultVO.Source> sources = new ArrayList<>();

        for (SearchResultVO.SearchItem item : searchResult.getItems()) {
            context.append("【").append(item.getFileName()).append("】\n");
            context.append(item.getContent()).append("\n\n");

            ChatResultVO.Source source = new ChatResultVO.Source();
            source.setFileId(item.getFileId());
            source.setFileName(item.getFileName());
            source.setContent(item.getContent().length() > 100
                    ? item.getContent().substring(0, 100) + "..."
                    : item.getContent());
            sources.add(source);
        }

        // 构建 prompt
        String systemPrompt = """
                你是一个智能文档助手。请根据以下文档内容回答用户的问题。
                如果文档内容不足以回答问题，请如实说明。
                回答要简洁准确，并引用相关文档内容。

                相关文档内容：
                """ + context;

        // 调用 LLM
        String answer = callLLM(systemPrompt, chatDTO.getQuestion(), chatDTO.getHistory());

        ChatResultVO resultVO = new ChatResultVO();
        resultVO.setAnswer(answer);
        resultVO.setSources(sources);
        resultVO.setCostMs(System.currentTimeMillis() - startTime);

        return resultVO;
    }

    @Override
    public String generateSummary(Long userId, Long fileId) {
        FileInfo fileInfo = fileService.getFileWithPermission(userId, fileId);

        // 读取文件内容
        String content = readFileContent(fileInfo);
        if (content == null || content.isBlank()) {
            throw new BusinessException(ResultCode.AI_SUMMARY_FAIL, "文件内容为空");
        }

        // 截取前面部分内容（避免超出 token 限制）
        if (content.length() > 5000) {
            content = content.substring(0, 5000);
        }

        String prompt = "请为以下文档生成一个简洁的摘要（200字以内）：\n\n" + content;
        return callLLM("你是一个专业的文档摘要助手。", prompt, null);
    }

    // ==================== 私有方法 ====================

    /**
     * 获取文本的向量嵌入
     */
    private float[] getEmbedding(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiConfig.getApiKey());

            Map<String, Object> body = new HashMap<>();
            body.put("model", aiConfig.getEmbeddingModel());
            body.put("input", text);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String url = aiConfig.getBaseUrl() + "/embeddings";

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode embeddingNode = jsonNode.get("data").get(0).get("embedding");

            float[] embedding = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }
            return embedding;
        } catch (Exception e) {
            log.error("获取向量嵌入失败", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "向量化服务异常");
        }
    }

    /**
     * 调用 LLM
     */
    private String callLLM(String systemPrompt, String userMessage, List<ChatDTO.ChatMessage> history) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiConfig.getApiKey());

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            if (history != null) {
                for (ChatDTO.ChatMessage msg : history) {
                    messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
                }
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> body = new HashMap<>();
            body.put("model", aiConfig.getModel());
            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 2048);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String url = aiConfig.getBaseUrl() + "/chat/completions";

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            log.error("调用 LLM 失败", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 服务异常");
        }
    }

    /**
     * 读取文件内容（仅支持文本类文件）
     */
    private String readFileContent(FileInfo fileInfo) {
        try {
            InputStream inputStream = minioUtils.downloadFile(fileInfo.getStoragePath());
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            log.error("读取文件内容失败: {}", fileInfo.getStoragePath(), e);
            throw new BusinessException(ResultCode.AI_VECTORIZE_FAIL, "读取文件失败");
        }
    }

    /**
     * 将文本分割成块
     */
    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = aiConfig.getChunkSize();
        int overlap = aiConfig.getChunkOverlap();

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            chunks.add(content.substring(start, end));
            start = end - overlap;
            if (start < 0)
                start = 0;
            if (end >= content.length())
                break;
        }
        return chunks;
    }

    /**
     * 检查文件是否支持向量化
     */
    private boolean isVectorizable(String ext) {
        if (ext == null) {
            return false;
        }
        return aiConfig.getVectorizableFileExtensions().contains(ext.toLowerCase());
    }
}

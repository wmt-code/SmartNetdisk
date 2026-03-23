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
    private final com.wmt.smartnetdisk.service.INotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(30));
        factory.setReadTimeout(java.time.Duration.ofSeconds(120));
        return new RestTemplate(factory);
    }

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
        // 清除 PostgreSQL 不支持的 NULL 字节
        content = content.replace("\u0000", "");

        // 分块处理
        List<String> chunks = splitIntoChunks(content);
        log.info("文件分块完成: fileId={}, chunks={}, 内容长度={}", fileId, chunks.size(), content.length());

        // 批量向量化并保存（每批最多 16 个 chunk）
        int batchSize = 16;
        for (int batchStart = 0; batchStart < chunks.size(); batchStart += batchSize) {
            int batchEnd = Math.min(batchStart + batchSize, chunks.size());
            List<String> batch = chunks.subList(batchStart, batchEnd);

            // 批量获取 embedding
            List<float[]> embeddings = getBatchEmbeddings(batch);

            for (int j = 0; j < batch.size(); j++) {
                VectorDocument doc = new VectorDocument();
                doc.setFileId(fileId);
                doc.setUserId(userId);
                doc.setContent(batch.get(j));
                doc.setChunkIndex(batchStart + j);
                doc.setTokenCount(batch.get(j).length());
                doc.setEmbeddingStr(VectorDocument.vectorToString(embeddings.get(j)));
                vectorDocumentMapper.insertVectorDocument(doc);
            }
            log.info("向量化进度: fileId={}, {}/{}", fileId, batchEnd, chunks.size());
        }

        // 更新文件向量化状态
        fileInfo.setIsVectorized(1);
        fileService.updateById(fileInfo);

        notificationService.createNotification(userId, "ai", "智能分析完成", "文件已完成向量化分析", fileId);

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

        log.info("开始语义搜索: userId={}, query={}, topK={}, minScore={}",
                userId, searchDTO.getQuery(), searchDTO.getTopK(), searchDTO.getMinScore());

        // 获取查询向量
        float[] queryEmbedding = getEmbedding(searchDTO.getQuery());
        log.info("查询向量生成成功, 维度: {}", queryEmbedding.length);
        String embeddingStr = VectorDocument.vectorToString(queryEmbedding);

        // 执行向量搜索
        List<VectorDocument> results = vectorDocumentMapper.searchSimilar(
                userId,
                embeddingStr,
                searchDTO.getTopK(),
                searchDTO.getMinScore());

        log.info("向量搜索完成，找到 {} 条结果", results.size());

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

        String prompt = "请用一两句话概括以下文档的核心内容，不超过80个字，不要使用换行符：\n\n" + content;
        String summary = callLLM("你是一个专业的文档摘要助手，只输出摘要内容，不要任何前缀和解释。", prompt, null);
        // 清理并截断
        summary = summary.replace("\n", " ").replace("\r", "").trim();
        if (summary.length() > 100) {
            summary = summary.substring(0, 100) + "...";
        }

        // 保存摘要到数据库
        fileInfo.setAiSummary(summary);
        fileService.updateById(fileInfo);

        return summary;
    }

    // ==================== 私有方法 ====================

    /**
     * 批量获取向量嵌入
     */
    private List<float[]> getBatchEmbeddings(List<String> texts) {
        String embeddingModel = aiConfig.getEmbeddingModel();
        String baseUrl = aiConfig.getEffectiveEmbeddingBaseUrl();
        boolean isDashScopeNative = baseUrl.contains("dashscope.aliyuncs.com/api/v1");
        boolean isVolcanoVision = embeddingModel != null
                && embeddingModel.contains("doubao") && embeddingModel.contains("vision");

        // DashScope 原生 API 和火山引擎 vision 不支持批量，逐个调用
        if (isDashScopeNative || isVolcanoVision) {
            List<float[]> results = new ArrayList<>();
            for (String text : texts) {
                results.add(getEmbedding(text));
            }
            return results;
        }

        // 标准模型：一次 API 调用处理多个文本
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiConfig.getEffectiveEmbeddingApiKey());

            Map<String, Object> body = new HashMap<>();
            body.put("model", embeddingModel);
            body.put("input", texts);
            body.put("encoding_format", "float");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String url = aiConfig.getEffectiveEmbeddingBaseUrl() + "/embeddings";

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode dataNode = jsonNode.get("data");

            List<float[]> results = new ArrayList<>();
            int dim = aiConfig.getEmbeddingDimension();
            for (int i = 0; i < dataNode.size(); i++) {
                JsonNode embNode = dataNode.get(i).get("embedding");
                int actualDim = Math.min(embNode.size(), dim);
                float[] emb = new float[actualDim];
                for (int j = 0; j < actualDim; j++) {
                    emb[j] = (float) embNode.get(j).asDouble();
                }
                results.add(emb);
            }
            return results;
        } catch (Exception e) {
            log.error("批量获取向量嵌入失败", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "向量化服务异常");
        }
    }

    /**
     * 获取文本的向量嵌入（自动适配多种 API 格式）
     */
    private float[] getEmbedding(String text) {
        try {
            String embeddingModel = aiConfig.getEmbeddingModel();
            String baseUrl = aiConfig.getEffectiveEmbeddingBaseUrl();
            boolean isDashScopeNative = baseUrl.contains("dashscope.aliyuncs.com/api/v1");
            boolean isVolcanoVision = embeddingModel != null
                    && embeddingModel.contains("doubao") && embeddingModel.contains("vision");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiConfig.getEffectiveEmbeddingApiKey());

            Map<String, Object> body = new HashMap<>();
            body.put("model", embeddingModel);
            String url;

            if (isDashScopeNative) {
                // DashScope 原生 multimodal embedding API
                // POST https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding
                body.put("input", Map.of("contents", java.util.List.of(Map.of("text", text))));
                body.put("parameters", Map.of("dimension", aiConfig.getEmbeddingDimension()));
                url = baseUrl;
            } else if (isVolcanoVision) {
                // 火山引擎 doubao-embedding-vision
                body.put("input", java.util.List.of(Map.of("type", "text", "text", text)));
                url = baseUrl + "/embeddings/multimodal";
            } else {
                // 标准 OpenAI 兼容格式
                body.put("input", java.util.List.of(text));
                url = baseUrl + "/embeddings";
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            // 解析 embedding 向量（兼容多种响应格式）
            JsonNode embeddingNode = null;

            // DashScope 原生格式: output.embeddings[0].embedding
            JsonNode outputNode = jsonNode.get("output");
            if (outputNode != null) {
                JsonNode embeddingsArr = outputNode.get("embeddings");
                if (embeddingsArr != null && embeddingsArr.isArray() && !embeddingsArr.isEmpty()) {
                    embeddingNode = embeddingsArr.get(0).get("embedding");
                }
            }

            // OpenAI 兼容格式: data[0].embedding 或 data.embedding
            if (embeddingNode == null) {
                JsonNode dataNode = jsonNode.get("data");
                if (dataNode != null) {
                    if (dataNode.isArray() && !dataNode.isEmpty()) {
                        embeddingNode = dataNode.get(0).get("embedding");
                    } else if (dataNode.isObject()) {
                        embeddingNode = dataNode.get("embedding");
                    }
                }
            }

            if (embeddingNode == null || !embeddingNode.isArray()) {
                log.error("Embedding 响应无法解析向量: {}", response.getBody());
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "向量化响应格式异常");
            }

            int dim = Math.min(embeddingNode.size(), aiConfig.getEmbeddingDimension());
            float[] embedding = new float[dim];
            for (int i = 0; i < dim; i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }
            return embedding;
        } catch (BusinessException e) {
            throw e;
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
            // 清理输入中的特殊字符
            userMessage = userMessage.replace("\u0000", "").replace("\r", "");
            if (systemPrompt != null) {
                systemPrompt = systemPrompt.replace("\u0000", "");
            }
            // 限制总输入长度（避免超出模型 token 限制）
            if (userMessage.length() > 4000) {
                userMessage = userMessage.substring(0, 4000) + "\n...(内容已截断)";
            }
            log.info("调用 LLM: model={}, userMessageLen={}", aiConfig.getModel(), userMessage.length());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiConfig.getApiKey());

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            if (history != null) {
                for (ChatDTO.ChatMessage msg : history) {
                    String content = msg.getContent().replace("\u0000", "");
                    messages.add(Map.of("role", msg.getRole(), "content", content));
                }
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> body = new HashMap<>();
            body.put("model", aiConfig.getModel());
            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 2000);

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
     * 读取文件内容（根据文件类型选择提取方式）
     */
    private String readFileContent(FileInfo fileInfo) {
        String ext = fileInfo.getFileExt() != null ? fileInfo.getFileExt().toLowerCase() : "";
        try (InputStream inputStream = minioUtils.downloadFile(fileInfo.getStoragePath())) {
            return switch (ext) {
                case "pdf" -> extractPdfText(inputStream);
                case "docx" -> extractDocxText(inputStream);
                case "doc" -> extractDocText(inputStream);
                default -> extractPlainText(inputStream);
            };
        } catch (Exception e) {
            log.error("读取文件内容失败: ext={}, path={}", ext, fileInfo.getStoragePath(), e);
            throw new BusinessException(ResultCode.AI_VECTORIZE_FAIL, "读取文件失败: " + e.getMessage());
        }
    }

    /** 提取 PDF 文本 */
    private String extractPdfText(InputStream inputStream) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(inputStream.readAllBytes())) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    /** 提取 docx 文本 */
    private String extractDocxText(InputStream inputStream) throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(inputStream)) {
            StringBuilder sb = new StringBuilder();
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph para : doc.getParagraphs()) {
                sb.append(para.getText()).append("\n");
            }
            // 也提取表格内容
            for (org.apache.poi.xwpf.usermodel.XWPFTable table : doc.getTables()) {
                for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                    for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                        sb.append(cell.getText()).append("\t");
                    }
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
    }

    /** 提取 doc (Word 97-2003) 文本 */
    private String extractDocText(InputStream inputStream) throws Exception {
        try (org.apache.poi.hwpf.HWPFDocument doc = new org.apache.poi.hwpf.HWPFDocument(inputStream)) {
            return doc.getDocumentText();
        }
    }

    /** 提取纯文本 (txt, md, json, xml 等) */
    private String extractPlainText(InputStream inputStream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
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
     * 检查文件是否支持向量化（私有方法）
     */
    private boolean isVectorizable(String ext) {
        if (ext == null) {
            return false;
        }
        return aiConfig.getVectorizableFileExtensions().contains(ext.toLowerCase());
    }

    @Override
    public boolean isFileVectorizable(String fileExt) {
        return isVectorizable(fileExt);
    }

    @Override
    @org.springframework.scheduling.annotation.Async
    public void vectorizeDocumentAsync(Long userId, Long fileId) {
        try {
            log.info("开始异步向量化文件: userId={}, fileId={}", userId, fileId);
            vectorizeDocument(userId, fileId);
            log.info("异步向量化完成: userId={}, fileId={}", userId, fileId);
        } catch (Exception e) {
            log.error("异步向量化失败: userId={}, fileId={}, error={}", userId, fileId, e.getMessage());
            // 异步任务失败不抛出异常，只记录日志
        }
    }
}

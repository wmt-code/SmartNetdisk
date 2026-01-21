package com.wmt.smartnetdisk.controller;

import com.wmt.smartnetdisk.common.result.Result;
import com.wmt.smartnetdisk.dto.request.ChatDTO;
import com.wmt.smartnetdisk.dto.request.SemanticSearchDTO;
import com.wmt.smartnetdisk.service.IAiService;
import com.wmt.smartnetdisk.service.IAuthService;
import com.wmt.smartnetdisk.vo.ChatResultVO;
import com.wmt.smartnetdisk.vo.SearchResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 智能控制器
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final IAiService aiService;
    private final IAuthService authService;

    /**
     * 文档向量化
     */
    @PostMapping("/vectorize/{fileId}")
    public Result<Void> vectorizeDocument(@PathVariable("fileId") Long fileId) {
        Long userId = authService.getCurrentUserId();
        aiService.vectorizeDocument(userId, fileId);
        return Result.success("向量化完成", null);
    }

    /**
     * 获取向量化状态
     */
    @GetMapping("/vectorize/status/{fileId}")
    public Result<Map<String, Object>> getVectorizeStatus(@PathVariable("fileId") Long fileId) {
        Long userId = authService.getCurrentUserId();
        IAiService.VectorizeStatus status = aiService.getVectorizeStatus(userId, fileId);
        Map<String, Object> data = new HashMap<>();
        data.put("isVectorized", status.isVectorized());
        data.put("chunkCount", status.chunkCount());
        return Result.success(data);
    }

    /**
     * 语义搜索
     */
    @PostMapping("/search")
    public Result<SearchResultVO> semanticSearch(@Valid @RequestBody SemanticSearchDTO searchDTO) {
        Long userId = authService.getCurrentUserId();
        SearchResultVO result = aiService.semanticSearch(userId, searchDTO);
        return Result.success(result);
    }

    /**
     * 智能问答
     */
    @PostMapping("/chat")
    public Result<ChatResultVO> chat(@Valid @RequestBody ChatDTO chatDTO) {
        Long userId = authService.getCurrentUserId();
        ChatResultVO result = aiService.chat(userId, chatDTO);
        return Result.success(result);
    }

    /**
     * 生成文档摘要
     */
    @PostMapping("/summary/{fileId}")
    public Result<Map<String, String>> generateSummary(@PathVariable("fileId") Long fileId) {
        Long userId = authService.getCurrentUserId();
        String summary = aiService.generateSummary(userId, fileId);
        Map<String, String> data = new HashMap<>();
        data.put("summary", summary);
        return Result.success(data);
    }
}

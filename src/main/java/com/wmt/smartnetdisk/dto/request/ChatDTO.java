package com.wmt.smartnetdisk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 智能问答请求 DTO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class ChatDTO {

    /**
     * 用户问题
     */
    @NotBlank(message = "问题不能为空")
    private String question;

    /**
     * 指定文件ID列表（可选，不指定则搜索所有已向量化文档）
     */
    private List<Long> fileIds;

    /**
     * 对话历史（可选）
     */
    private List<ChatMessage> history;

    @Data
    public static class ChatMessage {
        private String role; // user 或 assistant
        private String content;
    }
}

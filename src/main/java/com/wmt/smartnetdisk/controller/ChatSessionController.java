package com.wmt.smartnetdisk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wmt.smartnetdisk.common.result.Result;
import com.wmt.smartnetdisk.entity.ChatSession;
import com.wmt.smartnetdisk.mapper.ChatSessionMapper;
import com.wmt.smartnetdisk.service.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat-session")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionMapper chatSessionMapper;
    private final IAuthService authService;

    /**
     * 获取用户的所有会话（最近20个）
     */
    @GetMapping("/list")
    public Result<List<ChatSession>> list() {
        Long userId = authService.getCurrentUserId();
        List<ChatSession> sessions = chatSessionMapper.selectList(
            new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime)
                .last("LIMIT 20")
        );
        return Result.success(sessions);
    }

    /**
     * 创建新会话
     */
    @PostMapping
    public Result<ChatSession> create(@RequestBody Map<String, String> body) {
        Long userId = authService.getCurrentUserId();
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(body.getOrDefault("title", "新对话"));
        session.setMode(body.getOrDefault("mode", "global"));
        session.setMessages(body.getOrDefault("messages", "[]"));
        session.setScopedFileIds(body.getOrDefault("scopedFileIds", "[]"));
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return Result.success(session);
    }

    /**
     * 更新会话（消息、标题等）
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        Long userId = authService.getCurrentUserId();
        ChatSession session = chatSessionMapper.selectById(id);
        if (session == null || !session.getUserId().equals(userId)) {
            return Result.fail(404, "会话不存在");
        }
        if (body.containsKey("title")) session.setTitle(body.get("title"));
        if (body.containsKey("messages")) session.setMessages(body.get("messages"));
        if (body.containsKey("scopedFileIds")) session.setScopedFileIds(body.get("scopedFileIds"));
        if (body.containsKey("mode")) session.setMode(body.get("mode"));
        session.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(session);
        return Result.success("更新成功", null);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        Long userId = authService.getCurrentUserId();
        ChatSession session = chatSessionMapper.selectById(id);
        if (session == null || !session.getUserId().equals(userId)) {
            return Result.fail(404, "会话不存在");
        }
        chatSessionMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    /**
     * 清空所有会话
     */
    @DeleteMapping("/all")
    public Result<Void> deleteAll() {
        Long userId = authService.getCurrentUserId();
        chatSessionMapper.delete(
            new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
        );
        return Result.success("已清空", null);
    }
}

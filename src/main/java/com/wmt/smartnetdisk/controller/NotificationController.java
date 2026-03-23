package com.wmt.smartnetdisk.controller;

import com.wmt.smartnetdisk.common.result.Result;
import com.wmt.smartnetdisk.entity.Notification;
import com.wmt.smartnetdisk.service.IAuthService;
import com.wmt.smartnetdisk.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;
    private final IAuthService authService;

    /**
     * 获取通知列表
     */
    @GetMapping("/list")
    public Result<List<Notification>> list() {
        Long userId = authService.getCurrentUserId();
        return Result.success(notificationService.listNotifications(userId));
    }

    /**
     * 获取未读数量
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> unreadCount() {
        Long userId = authService.getCurrentUserId();
        Map<String, Integer> data = new HashMap<>();
        data.put("count", notificationService.getUnreadCount(userId));
        return Result.success(data);
    }

    /**
     * 标记单条已读
     */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable("id") Long id) {
        Long userId = authService.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.success("已读", null);
    }

    /**
     * 全部标记已读
     */
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = authService.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.success("全部已读", null);
    }
}

package com.wmt.smartnetdisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wmt.smartnetdisk.entity.Notification;
import java.util.List;

public interface INotificationService extends IService<Notification> {
    /**
     * 创建通知
     */
    void createNotification(Long userId, String type, String title, String content, Long relatedId);

    /**
     * 获取用户通知列表（最近50条）
     */
    List<Notification> listNotifications(Long userId);

    /**
     * 获取未读数量
     */
    int getUnreadCount(Long userId);

    /**
     * 标记单条已读
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 全部标记已读
     */
    void markAllAsRead(Long userId);
}

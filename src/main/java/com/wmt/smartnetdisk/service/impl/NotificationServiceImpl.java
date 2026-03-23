package com.wmt.smartnetdisk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmt.smartnetdisk.entity.Notification;
import com.wmt.smartnetdisk.mapper.NotificationMapper;
import com.wmt.smartnetdisk.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

    @Override
    public void createNotification(Long userId, String type, String title, String content, Long relatedId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRelatedId(relatedId);
        n.setIsRead(0);
        save(n);
    }

    @Override
    public List<Notification> listNotifications(Long userId) {
        return list(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime)
                .last("LIMIT 50"));
    }

    @Override
    public int getUnreadCount(Long userId) {
        return baseMapper.countUnread(userId);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification n = getById(notificationId);
        if (n != null && n.getUserId().equals(userId)) {
            n.setIsRead(1);
            updateById(n);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        baseMapper.markAllRead(userId);
    }
}

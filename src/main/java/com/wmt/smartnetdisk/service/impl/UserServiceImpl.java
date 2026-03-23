package com.wmt.smartnetdisk.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmt.smartnetdisk.entity.User;
import com.wmt.smartnetdisk.mapper.UserMapper;
import com.wmt.smartnetdisk.service.IUserService;
import com.wmt.smartnetdisk.utils.MinioUtils;
import com.wmt.smartnetdisk.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类
 *
 * @author wmt
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final MinioUtils minioUtils;

    @Override
    public User getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Override
    public User getByEmail(String email) {
        return baseMapper.selectByEmail(email);
    }

    @Override
    public User getByUsernameOrEmail(String account) {
        return baseMapper.selectByUsernameOrEmail(account);
    }

    @Override
    public boolean existsByUsername(String username) {
        return getByUsername(username) != null;
    }

    @Override
    public boolean existsByEmail(String email) {
        return getByEmail(email) != null;
    }

    @Override
    public UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        // 邮箱脱敏：保留前3位和@后面的部分
        vo.setEmail(maskEmail(user.getEmail()));
        // 头像：将 MinIO 路径转为 presigned URL
        if (user.getAvatar() != null && !user.getAvatar().isBlank()) {
            try {
                vo.setAvatar(minioUtils.getAvatarPresignedUrl(user.getAvatar(), 7 * 24 * 3600));
            } catch (Exception e) {
                vo.setAvatar(null);
            }
        }
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setUsedSpace(user.getUsedSpace());
        vo.setTotalSpace(user.getTotalSpace());
        // 计算使用百分比
        if (user.getTotalSpace() != null && user.getTotalSpace() > 0) {
            double percent = (user.getUsedSpace() * 100.0) / user.getTotalSpace();
            vo.setUsedPercent(Math.round(percent * 100.0) / 100.0);
        } else {
            vo.setUsedPercent(0.0);
        }
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUsedSpace(Long userId, Long delta) {
        User user = getById(userId);
        if (user != null) {
            long newUsedSpace = user.getUsedSpace() + delta;
            if (newUsedSpace < 0) {
                newUsedSpace = 0;
            }
            user.setUsedSpace(newUsedSpace);
            updateById(user);
        }
    }

    /**
     * 邮箱脱敏
     * 例如：test@example.com -> tes***@example.com
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 3) {
            return email;
        }
        String prefix = email.substring(0, 3);
        String suffix = email.substring(atIndex);
        return prefix + "***" + suffix;
    }
}

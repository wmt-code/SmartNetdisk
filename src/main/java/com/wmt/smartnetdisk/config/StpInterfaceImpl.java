package com.wmt.smartnetdisk.config;

import cn.dev33.satoken.stp.StpInterface;
import com.wmt.smartnetdisk.entity.User;
import com.wmt.smartnetdisk.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限认证接口实现
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final IUserService userService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>();
        try {
            Long userId = Long.parseLong(loginId.toString());
            User user = userService.getById(userId);
            if (user != null && user.getRole() != null) {
                roles.add(user.getRole());
            }
        } catch (Exception e) {
            // ignore
        }
        return roles;
    }
}

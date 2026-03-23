package com.wmt.smartnetdisk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wmt.smartnetdisk.common.result.Result;
import com.wmt.smartnetdisk.entity.FileInfo;
import com.wmt.smartnetdisk.entity.User;
import com.wmt.smartnetdisk.service.IFileService;
import com.wmt.smartnetdisk.service.IUserService;
import com.wmt.smartnetdisk.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IUserService userService;
    private final IFileService fileService;

    /**
     * 系统统计概览
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 用户统计
        long totalUsers = userService.count();
        long activeUsers = userService.count(new LambdaQueryWrapper<User>().eq(User::getStatus, 1));

        // 文件统计
        long totalFiles = fileService.count(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getDeleted, 0));

        // 存储统计
        List<User> allUsers = userService.list();
        long totalUsedSpace = allUsers.stream().mapToLong(u -> u.getUsedSpace() != null ? u.getUsedSpace() : 0).sum();
        long totalAllocatedSpace = allUsers.stream().mapToLong(u -> u.getTotalSpace() != null ? u.getTotalSpace() : 0).sum();

        // 向量化文件统计
        long vectorizedFiles = fileService.count(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getDeleted, 0).eq(FileInfo::getIsVectorized, 1));

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("totalFiles", totalFiles);
        stats.put("totalUsedSpace", totalUsedSpace);
        stats.put("totalAllocatedSpace", totalAllocatedSpace);
        stats.put("vectorizedFiles", vectorizedFiles);

        // 文件类型分布
        List<FileInfo> allFiles = fileService.list(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getDeleted, 0));
        Map<String, Long> fileTypeDistribution = allFiles.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        f -> f.getFileType() != null ? f.getFileType() : "other",
                        java.util.stream.Collectors.counting()));
        stats.put("fileTypeDistribution", fileTypeDistribution);

        // 用户存储排行（Top 10）
        List<Map<String, Object>> userStorageRank = allUsers.stream()
                .sorted((a, b) -> Long.compare(
                        b.getUsedSpace() != null ? b.getUsedSpace() : 0,
                        a.getUsedSpace() != null ? a.getUsedSpace() : 0))
                .limit(10)
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("username", u.getUsername());
                    m.put("usedSpace", u.getUsedSpace() != null ? u.getUsedSpace() : 0);
                    m.put("totalSpace", u.getTotalSpace() != null ? u.getTotalSpace() : 0);
                    return m;
                }).toList();
        stats.put("userStorageRank", userStorageRank);

        // 最近7天每天上传文件数
        java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7);
        List<FileInfo> recentFiles = fileService.list(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getDeleted, 0).ge(FileInfo::getCreateTime, sevenDaysAgo));
        Map<String, Long> dailyUploads = recentFiles.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        f -> f.getCreateTime().toLocalDate().toString(),
                        java.util.stream.Collectors.counting()));
        // 补全 7 天
        List<Map<String, Object>> uploadTrend = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = java.time.LocalDate.now().minusDays(i).toString();
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.substring(5)); // MM-DD
            item.put("count", dailyUploads.getOrDefault(date, 0L));
            uploadTrend.add(item);
        }
        stats.put("uploadTrend", uploadTrend);

        return Result.success(stats);
    }

    /**
     * 用户列表（分页 + 搜索）
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getEmail, keyword));
        }

        wrapper.orderByDesc(User::getCreateTime);

        Page<User> page = new Page<>(pageNum, pageSize);
        Page<User> result = userService.page(page, wrapper);

        List<UserVO> voList = result.getRecords().stream()
                .map(userService::toVO)
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("records", voList);
        data.put("total", result.getTotal());
        data.put("pageNum", result.getCurrent());
        data.put("pageSize", result.getSize());

        return Result.success(data);
    }

    /**
     * 修改用户状态（启用/禁用）
     */
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable("id") Long userId, @RequestBody Map<String, Integer> body) {
        User user = userService.getById(userId);
        if (user == null) return Result.fail(404, "用户不存在");

        user.setStatus(body.getOrDefault("status", 1));
        userService.updateById(user);
        log.info("管理员修改用户状态: userId={}, status={}", userId, user.getStatus());
        return Result.success("操作成功", null);
    }

    /**
     * 修改用户存储配额
     */
    @PutMapping("/users/{id}/space")
    public Result<Void> updateUserSpace(@PathVariable("id") Long userId, @RequestBody Map<String, Long> body) {
        User user = userService.getById(userId);
        if (user == null) return Result.fail(404, "用户不存在");

        Long totalSpace = body.get("totalSpace");
        if (totalSpace != null && totalSpace > 0) {
            user.setTotalSpace(totalSpace);
            userService.updateById(user);
            log.info("管理员修改用户配额: userId={}, totalSpace={}", userId, totalSpace);
        }
        return Result.success("操作成功", null);
    }

    /**
     * 修改用户角色
     */
    @PutMapping("/users/{id}/role")
    public Result<Void> updateUserRole(@PathVariable("id") Long userId, @RequestBody Map<String, String> body) {
        User user = userService.getById(userId);
        if (user == null) return Result.fail(404, "用户不存在");

        String role = body.getOrDefault("role", "user");
        if (!"admin".equals(role) && !"user".equals(role)) {
            return Result.fail(400, "无效的角色");
        }

        user.setRole(role);
        userService.updateById(user);
        log.info("管理员修改用户角色: userId={}, role={}", userId, role);
        return Result.success("操作成功", null);
    }

    /**
     * 删除用户（软删除）
     */
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable("id") Long userId) {
        User user = userService.getById(userId);
        if (user == null) return Result.fail(404, "用户不存在");
        if ("admin".equals(user.getRole())) return Result.fail(400, "不能删除管理员账号");

        userService.removeById(userId);
        log.info("管理员删除用户: userId={}", userId);
        return Result.success("删除成功", null);
    }

    /**
     * 全局文件列表（跨用户）
     */
    @GetMapping("/files")
    public Result<Map<String, Object>> listAllFiles(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        log.info("管理员文件查询: keyword={}, fileType={}, ownerName={}, startDate={}, endDate={}", keyword, fileType, ownerName, startDate, endDate);

        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getDeleted, 0);

        // 文件名搜索
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(FileInfo::getFileName, keyword);
        }

        // 文件类型筛选
        if (fileType != null && !fileType.isBlank()) {
            wrapper.eq(FileInfo::getFileType, fileType);
        }

        // 所属用户筛选（按用户名查用户ID）
        if (ownerName != null && !ownerName.isBlank()) {
            List<Long> userIds = userService.list(new LambdaQueryWrapper<User>()
                    .like(User::getUsername, ownerName).eq(User::getDeleted, 0))
                    .stream().map(User::getId).toList();
            if (userIds.isEmpty()) {
                wrapper.eq(FileInfo::getUserId, -1); // 无匹配用户
            } else {
                wrapper.in(FileInfo::getUserId, userIds);
            }
        }

        // 日期范围筛选
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(FileInfo::getCreateTime, java.time.LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(FileInfo::getCreateTime, java.time.LocalDate.parse(endDate).atTime(23, 59, 59));
        }

        wrapper.orderByDesc(FileInfo::getCreateTime);

        Page<FileInfo> page = new Page<>(pageNum, pageSize);
        Page<FileInfo> result = fileService.page(page, wrapper);

        // Include user info for each file
        List<Map<String, Object>> records = result.getRecords().stream().map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("fileName", f.getFileName());
            map.put("fileSize", f.getFileSize());
            map.put("fileType", f.getFileType());
            map.put("userId", f.getUserId());
            map.put("createTime", f.getCreateTime());
            map.put("isVectorized", f.getIsVectorized());
            // Get username
            User owner = userService.getById(f.getUserId());
            map.put("ownerName", owner != null ? owner.getUsername() : "未知");
            return map;
        }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());

        return Result.success(data);
    }

    /**
     * 管理员删除文件
     */
    @DeleteMapping("/files/{id}")
    public Result<Void> deleteFile(@PathVariable("id") Long fileId) {
        FileInfo file = fileService.getById(fileId);
        if (file == null) return Result.fail(404, "文件不存在");

        file.setDeleted(1);
        fileService.updateById(file);
        log.info("管理员删除文件: fileId={}, fileName={}", fileId, file.getFileName());
        return Result.success("删除成功", null);
    }
}

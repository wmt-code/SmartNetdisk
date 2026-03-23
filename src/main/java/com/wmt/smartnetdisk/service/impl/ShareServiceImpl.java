package com.wmt.smartnetdisk.service.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmt.smartnetdisk.common.enums.ShareStatusEnum;
import com.wmt.smartnetdisk.common.exception.BusinessException;
import com.wmt.smartnetdisk.common.result.PageRequest;
import com.wmt.smartnetdisk.common.result.PageResult;
import com.wmt.smartnetdisk.common.result.ResultCode;
import com.wmt.smartnetdisk.dto.request.CreateShareDTO;
import com.wmt.smartnetdisk.entity.FileInfo;
import com.wmt.smartnetdisk.entity.Folder;
import com.wmt.smartnetdisk.entity.Share;
import com.wmt.smartnetdisk.entity.ShareItem;
import com.wmt.smartnetdisk.mapper.FileInfoMapper;
import com.wmt.smartnetdisk.mapper.ShareItemMapper;
import com.wmt.smartnetdisk.mapper.ShareMapper;
import com.wmt.smartnetdisk.entity.User;
import com.wmt.smartnetdisk.service.IFileService;
import com.wmt.smartnetdisk.service.IFolderService;
import com.wmt.smartnetdisk.service.IShareService;
import com.wmt.smartnetdisk.service.IUserService;
import com.wmt.smartnetdisk.config.KkFileViewConfig;
import com.wmt.smartnetdisk.utils.MinioUtils;
import com.wmt.smartnetdisk.vo.ShareVO;
import com.wmt.smartnetdisk.vo.SpaceVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 分享服务实现类 - 支持单文件、目录、批量分享
 *
 * @author wmt
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareServiceImpl extends ServiceImpl<ShareMapper, Share> implements IShareService {

    private final IFileService fileService;
    private final IFolderService folderService;
    private final ShareItemMapper shareItemMapper;
    private final FileInfoMapper fileInfoMapper;
    private final MinioUtils minioUtils;
    private final KkFileViewConfig kkFileViewConfig;
    private final com.wmt.smartnetdisk.service.INotificationService notificationService;
    private final IUserService userService;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    // 分享类型常量
    private static final int SHARE_TYPE_SINGLE = 0;
    private static final int SHARE_TYPE_FOLDER = 1;
    private static final int SHARE_TYPE_BATCH = 2;

    // 分享项类型常量
    private static final int ITEM_TYPE_FILE = 0;
    private static final int ITEM_TYPE_FOLDER = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareVO createShare(Long userId, CreateShareDTO createDTO) {
        int shareType = createDTO.getShareType() != null ? createDTO.getShareType() : SHARE_TYPE_SINGLE;

        return switch (shareType) {
            case SHARE_TYPE_FOLDER -> createFolderShare(userId, createDTO);
            case SHARE_TYPE_BATCH -> createBatchShare(userId, createDTO);
            default -> createSingleFileShare(userId, createDTO);
        };
    }

    /**
     * 创建单文件分享
     */
    private ShareVO createSingleFileShare(Long userId, CreateShareDTO createDTO) {
        // 验证文件存在且属于当前用户
        FileInfo fileInfo = fileService.getFileWithPermission(userId, createDTO.getFileId());

        Share share = buildShare(userId, createDTO);
        share.setShareType(SHARE_TYPE_SINGLE);
        share.setFileId(createDTO.getFileId());
        share.setTotalSize(fileInfo.getFileSize());
        share.setFileCount(1);

        save(share);

        // 创建分享项记录
        saveShareItem(share.getId(), ITEM_TYPE_FILE, createDTO.getFileId(), null);

        log.info("创建单文件分享成功: userId={}, fileId={}, shareCode={}", userId, createDTO.getFileId(), share.getShareCode());
        notificationService.createNotification(userId, "share", "分享创建成功", "已创建分享链接", share.getId());

        ShareVO vo = toVO(share, true);
        vo.setFileName(fileInfo.getFileName());
        vo.setFileSize(fileInfo.getFileSize());
        vo.setFileSizeStr(SpaceVO.formatSize(fileInfo.getFileSize()));
        vo.setFileType(getFileExtension(fileInfo.getFileName()));
        return vo;
    }

    /**
     * 创建目录分享
     */
    private ShareVO createFolderShare(Long userId, CreateShareDTO createDTO) {
        Long folderId = createDTO.getFolderId();
        if (folderId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目录分享必须指定文件夹ID");
        }

        // 验证文件夹存在且属于当前用户
        Folder folder = folderService.getById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FOLDER_NOT_FOUND);
        }

        // 计算文件夹内容统计
        FolderStats stats = calculateFolderStats(userId, folderId);

        Share share = buildShare(userId, createDTO);
        share.setShareType(SHARE_TYPE_FOLDER);
        share.setFolderId(folderId);
        share.setShareTitle(folder.getFolderName());
        share.setTotalSize(stats.totalSize);
        share.setFileCount(stats.fileCount);

        save(share);

        // 创建分享项记录
        saveShareItem(share.getId(), ITEM_TYPE_FOLDER, null, folderId);

        log.info("创建目录分享成功: userId={}, folderId={}, shareCode={}, fileCount={}",
                userId, folderId, share.getShareCode(), stats.fileCount);
        notificationService.createNotification(userId, "share", "分享创建成功", "已创建分享链接", share.getId());

        ShareVO vo = toVO(share, true);
        vo.setFolderName(folder.getFolderName());
        return vo;
    }

    /**
     * 创建批量分享
     */
    private ShareVO createBatchShare(Long userId, CreateShareDTO createDTO) {
        List<CreateShareDTO.ShareItemDTO> items = createDTO.getItems();
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "批量分享必须指定分享项");
        }

        // 计算总大小和文件数
        long totalSize = 0;
        int fileCount = 0;

        for (CreateShareDTO.ShareItemDTO item : items) {
            if (item.getItemType() == ITEM_TYPE_FILE) {
                FileInfo fileInfo = fileService.getFileWithPermission(userId, item.getFileId());
                totalSize += fileInfo.getFileSize();
                fileCount++;
            } else if (item.getItemType() == ITEM_TYPE_FOLDER) {
                FolderStats stats = calculateFolderStats(userId, item.getFolderId());
                totalSize += stats.totalSize;
                fileCount += stats.fileCount;
            }
        }

        Share share = buildShare(userId, createDTO);
        share.setShareType(SHARE_TYPE_BATCH);
        share.setShareTitle(createDTO.getShareTitle() != null ? createDTO.getShareTitle() : "批量分享");
        share.setTotalSize(totalSize);
        share.setFileCount(fileCount);

        save(share);

        // 保存分享项
        for (CreateShareDTO.ShareItemDTO item : items) {
            if (item.getItemType() == ITEM_TYPE_FILE) {
                saveShareItem(share.getId(), ITEM_TYPE_FILE, item.getFileId(), null);
            } else {
                saveShareItem(share.getId(), ITEM_TYPE_FOLDER, null, item.getFolderId());
            }
        }

        log.info("创建批量分享成功: userId={}, shareCode={}, itemCount={}, fileCount={}",
                userId, share.getShareCode(), items.size(), fileCount);
        notificationService.createNotification(userId, "share", "分享创建成功", "已创建分享链接", share.getId());

        return toVO(share, true);
    }

    /**
     * 构建分享基础对象
     */
    private Share buildShare(Long userId, CreateShareDTO createDTO) {
        String shareCode = generateShareCode();
        String password = createDTO.getPassword();
        if (password == null || password.isBlank()) {
            password = generatePassword();
        }

        LocalDateTime expireTime = null;
        if (createDTO.getExpireDays() != null && createDTO.getExpireDays() > 0) {
            expireTime = LocalDateTime.now().plusDays(createDTO.getExpireDays());
        }

        Share share = new Share();
        share.setUserId(userId);
        share.setShareCode(shareCode);
        share.setPassword(password);
        share.setExpireTime(expireTime);
        share.setViewCount(0);
        share.setDownloadCount(0);
        share.setMaxViewCount(createDTO.getMaxViewCount() != null ? createDTO.getMaxViewCount() : 0);
        share.setStatus(ShareStatusEnum.ACTIVE.getCode());

        return share;
    }

    /**
     * 保存分享项
     */
    private void saveShareItem(Long shareId, int itemType, Long fileId, Long folderId) {
        ShareItem item = new ShareItem();
        item.setShareId(shareId);
        item.setItemType(itemType);
        item.setFileId(fileId);
        item.setFolderId(folderId);
        shareItemMapper.insert(item);
    }

    /**
     * 计算文件夹统计信息（使用优化的递归CTE查询，单次SQL获取所有子文件夹统计）
     */
    private FolderStats calculateFolderStats(Long userId, Long folderId) {
        FolderStats stats = new FolderStats();

        // 使用递归CTE一次性查询所有子文件夹的文件统计
        java.util.Map<String, Object> result = fileInfoMapper.sumFileSizeRecursive(userId, folderId);
        if (result != null) {
            Object totalSizeObj = result.get("totalsize");
            Object fileCountObj = result.get("filecount");

            if (totalSizeObj instanceof Number) {
                stats.totalSize = ((Number) totalSizeObj).longValue();
            }
            if (fileCountObj instanceof Number) {
                stats.fileCount = ((Number) fileCountObj).intValue();
            }
        }

        return stats;
    }

    private static class FolderStats {
        long totalSize = 0;
        int fileCount = 0;
    }

    @Override
    public PageResult<ShareVO> listMyShares(Long userId, PageRequest pageRequest) {
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Share::getUserId, userId)
                .orderByDesc(Share::getCreateTime);

        Page<Share> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        Page<Share> result = baseMapper.selectPage(page, wrapper);

        List<ShareVO> voList = result.getRecords().stream()
                .map(share -> {
                    ShareVO vo = toVO(share, true);
                    populateShareDetails(vo, share);
                    return vo;
                })
                .toList();

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), voList);
    }

    /**
     * 填充分享者信息
     */
    private void populateSharerInfo(ShareVO vo, Long userId) {
        if (userId == null) return;
        User sharer = userService.getById(userId);
        if (sharer != null) {
            vo.setSharerUsername(sharer.getUsername());
            if (sharer.getAvatar() != null && !sharer.getAvatar().isBlank()) {
                try {
                    vo.setSharerAvatar(minioUtils.getAvatarPresignedUrl(sharer.getAvatar(), 7 * 24 * 3600));
                } catch (Exception e) {
                    vo.setSharerAvatar(null);
                }
            }
        }
    }

    /**
     * 填充分享详情
     */
    private void populateShareDetails(ShareVO vo, Share share) {
        int shareType = share.getShareType() != null ? share.getShareType() : SHARE_TYPE_SINGLE;

        switch (shareType) {
            case SHARE_TYPE_FOLDER -> {
                if (share.getFolderId() != null) {
                    Folder folder = folderService.getById(share.getFolderId());
                    if (folder != null) {
                        vo.setFolderName(folder.getFolderName());
                    }
                }
            }
            case SHARE_TYPE_BATCH -> {
                // 批量分享，标题已在 share.shareTitle 中
            }
            default -> {
                // 单文件分享
                if (share.getFileId() != null) {
                    FileInfo fileInfo = fileService.getById(share.getFileId());
                    if (fileInfo != null) {
                        vo.setFileName(fileInfo.getFileName());
                        vo.setFileSize(fileInfo.getFileSize());
                        vo.setFileSizeStr(SpaceVO.formatSize(fileInfo.getFileSize()));
                        vo.setFileType(getFileExtension(fileInfo.getFileName()));
                    }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelShare(Long userId, Long shareId) {
        Share share = getById(shareId);
        if (share == null || !share.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }
        share.setStatus(ShareStatusEnum.CANCELLED.getCode());
        updateById(share);
        log.info("取消分享成功: shareId={}", shareId);
    }

    @Override
    public ShareVO getShareByCode(String shareCode) {
        Share share = baseMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }

        checkShareValid(share);
        baseMapper.incrementViewCount(share.getId());

        ShareVO vo = toVO(share, false);
        populateShareDetails(vo, share);
        populateSharerInfo(vo, share.getUserId());
        return vo;
    }

    @Override
    public String verifyPassword(String shareCode, String password) {
        Share share = baseMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }

        checkShareValid(share);

        if (!share.getPassword().equals(password)) {
            throw new BusinessException(ResultCode.SHARE_PASSWORD_ERROR);
        }

        // 单文件分享返回下载链接
        if (share.getShareType() == null || share.getShareType() == SHARE_TYPE_SINGLE) {
            FileInfo fileInfo = fileService.getById(share.getFileId());
            if (fileInfo == null) {
                throw new BusinessException(ResultCode.FILE_NOT_FOUND);
            }
            return minioUtils.getDownloadUrl(fileInfo.getStoragePath(), fileInfo.getFileName(), 3600);
        }

        // 批量/目录分享返回空
        return "";
    }

    @Override
    public ShareVO verifyPasswordAndGetInfo(String shareCode, String password) {
        Share share = baseMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }

        checkShareValid(share);

        if (!share.getPassword().equals(password)) {
            throw new BusinessException(ResultCode.SHARE_PASSWORD_ERROR);
        }

        ShareVO vo = toVO(share, false);
        populateShareDetails(vo, share);
        populateSharerInfo(vo, share.getUserId());
        return vo;
    }

    @Override
    public String getDownloadUrl(String shareCode, String password) {
        String url = verifyPassword(shareCode, password);

        Share share = baseMapper.selectByShareCode(shareCode);
        if (share != null) {
            baseMapper.incrementDownloadCount(share.getId());
        }

        return url;
    }

    @Override
    public void downloadShareStream(String shareCode, String password, HttpServletResponse response) {
        Share share = baseMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }

        checkShareValid(share);

        if (password == null || !share.getPassword().equals(password)) {
            throw new BusinessException(ResultCode.SHARE_PASSWORD_ERROR);
        }

        // 只支持单文件下载
        if (share.getShareType() != null && share.getShareType() != SHARE_TYPE_SINGLE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "批量分享请使用单独下载接口");
        }

        FileInfo fileInfo = fileService.getById(share.getFileId());
        if (fileInfo == null) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }

        try (InputStream inputStream = minioUtils.downloadFile(fileInfo.getStoragePath());
                OutputStream outputStream = response.getOutputStream()) {

            String fileName = fileInfo.getFileName();
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileInfo.getFileSize()));

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();

            baseMapper.incrementDownloadCount(share.getId());
            log.info("分享文件下载成功: shareCode={}, fileName={}", shareCode, fileName);
        } catch (Exception e) {
            log.error("分享文件下载失败: shareCode={}", shareCode, e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public List<ShareVO.ShareItemVO> getShareItems(String shareCode, String password) {
        Share share = baseMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }

        checkShareValid(share);

        // 验证密码
        if (share.getPassword() != null && !share.getPassword().isEmpty()) {
            if (password == null || !share.getPassword().equals(password)) {
                throw new BusinessException(ResultCode.SHARE_PASSWORD_ERROR);
            }
        }

        List<ShareVO.ShareItemVO> result = new ArrayList<>();
        int shareType = share.getShareType() != null ? share.getShareType() : SHARE_TYPE_SINGLE;

        if (shareType == SHARE_TYPE_SINGLE) {
            // 单文件分享
            FileInfo file = fileService.getById(share.getFileId());
            if (file != null) {
                result.add(buildFileItemVO(file));
            }
        } else if (shareType == SHARE_TYPE_FOLDER) {
            // 目录分享 - 返回文件夹第一级内容
            result.addAll(getFolderContents(share.getUserId(), share.getFolderId()));
        } else {
            // 批量分享
            List<ShareItem> items = shareItemMapper.selectByShareId(share.getId());
            for (ShareItem item : items) {
                if (item.getItemType() == ITEM_TYPE_FILE) {
                    FileInfo file = fileService.getById(item.getFileId());
                    if (file != null) {
                        result.add(buildFileItemVO(file));
                    }
                } else {
                    Folder folder = folderService.getById(item.getFolderId());
                    if (folder != null) {
                        result.add(buildFolderItemVO(share.getUserId(), folder));
                    }
                }
            }
        }

        return result;
    }

    /**
     * 获取文件夹内容
     */
    private List<ShareVO.ShareItemVO> getFolderContents(Long userId, Long folderId) {
        List<ShareVO.ShareItemVO> result = new ArrayList<>();

        // 获取子文件夹
        List<Folder> folders = folderService.list(new LambdaQueryWrapper<Folder>()
                .eq(Folder::getUserId, userId)
                .eq(Folder::getParentId, folderId)
                .eq(Folder::getDeleted, 0)
                .orderByAsc(Folder::getFolderName));

        for (Folder folder : folders) {
            result.add(buildFolderItemVO(userId, folder));
        }

        // 获取文件
        List<FileInfo> files = fileService.list(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFolderId, folderId)
                .eq(FileInfo::getDeleted, 0)
                .orderByAsc(FileInfo::getFileName));

        for (FileInfo file : files) {
            result.add(buildFileItemVO(file));
        }

        return result;
    }

    /**
     * 构建文件项VO
     */
    private ShareVO.ShareItemVO buildFileItemVO(FileInfo file) {
        ShareVO.ShareItemVO vo = new ShareVO.ShareItemVO();
        vo.setItemType(ITEM_TYPE_FILE);
        vo.setFileId(file.getId());
        vo.setName(file.getFileName());
        vo.setSize(file.getFileSize());
        vo.setSizeStr(SpaceVO.formatSize(file.getFileSize()));
        vo.setFileType(getFileExtension(file.getFileName()));
        return vo;
    }

    /**
     * 构建文件夹项VO
     */
    private ShareVO.ShareItemVO buildFolderItemVO(Long userId, Folder folder) {
        ShareVO.ShareItemVO vo = new ShareVO.ShareItemVO();
        vo.setItemType(ITEM_TYPE_FOLDER);
        vo.setFolderId(folder.getId());
        vo.setName(folder.getFolderName());

        // 为了性能，不计算子项数量，设为0
        vo.setSize(0L);
        vo.setSizeStr("-");
        vo.setChildCount(0);

        return vo;
    }

    @Override
    public List<ShareVO.ShareItemVO> browseFolderContents(String shareCode, String password, Long folderId) {
        Share share = baseMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }

        checkShareValid(share);

        // 验证密码
        if (share.getPassword() != null && !share.getPassword().isEmpty()) {
            if (password == null || !share.getPassword().equals(password)) {
                throw new BusinessException(ResultCode.SHARE_PASSWORD_ERROR);
            }
        }

        int shareType = share.getShareType() != null ? share.getShareType() : SHARE_TYPE_SINGLE;

        // 单文件分享不支持浏览
        if (shareType == SHARE_TYPE_SINGLE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "单文件分享不支持浏览");
        }

        // 验证 folderId 是否在分享范围内
        Long rootFolderId = share.getFolderId();
        if (folderId == null || folderId == 0) {
            folderId = rootFolderId;
        } else {
            // 检查 folderId 是否是分享文件夹的子文件夹
            if (!isFolderInShareScope(share, folderId)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "无权访问该文件夹");
            }
        }

        return getFolderContents(share.getUserId(), folderId);
    }

    /**
     * 检查文件夹是否在分享范围内
     */
    private boolean isFolderInShareScope(Share share, Long folderId) {
        int shareType = share.getShareType() != null ? share.getShareType() : SHARE_TYPE_SINGLE;

        if (shareType == SHARE_TYPE_FOLDER) {
            // 目录分享：检查是否是分享文件夹或其子文件夹
            return isSubfolder(share.getFolderId(), folderId, share.getUserId());
        } else if (shareType == SHARE_TYPE_BATCH) {
            // 批量分享：检查是否是批量分享中的文件夹或其子文件夹
            List<ShareItem> items = shareItemMapper.selectByShareId(share.getId());
            for (ShareItem item : items) {
                if (item.getItemType() == ITEM_TYPE_FOLDER) {
                    if (item.getFolderId().equals(folderId)
                            || isSubfolder(item.getFolderId(), folderId, share.getUserId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查 targetId 是否是 parentId 的子文件夹
     */
    private boolean isSubfolder(Long parentId, Long targetId, Long userId) {
        if (parentId.equals(targetId)) {
            return true;
        }
        // 逐级向上查找父文件夹
        Folder folder = folderService.getById(targetId);
        while (folder != null && folder.getUserId().equals(userId)) {
            if (folder.getParentId().equals(parentId)) {
                return true;
            }
            if (folder.getParentId() == 0) {
                break;
            }
            folder = folderService.getById(folder.getParentId());
        }
        return false;
    }

    @Override
    public String getFileDownloadUrl(String shareCode, String password, Long fileId) {
        Share share = baseMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }

        checkShareValid(share);

        // 验证密码
        if (share.getPassword() != null && !share.getPassword().isEmpty()) {
            if (password == null || !share.getPassword().equals(password)) {
                throw new BusinessException(ResultCode.SHARE_PASSWORD_ERROR);
            }
        }

        // 验证文件是否在分享范围内
        if (!isFileInShareScope(share, fileId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不在分享范围内");
        }

        FileInfo file = fileService.getById(fileId);
        if (file == null) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }

        baseMapper.incrementDownloadCount(share.getId());
        return minioUtils.getDownloadUrl(file.getStoragePath(), file.getFileName(), 3600);
    }

    /**
     * 检查文件是否在分享范围内
     */
    private boolean isFileInShareScope(Share share, Long fileId) {
        int shareType = share.getShareType() != null ? share.getShareType() : SHARE_TYPE_SINGLE;

        if (shareType == SHARE_TYPE_SINGLE) {
            return share.getFileId().equals(fileId);
        } else if (shareType == SHARE_TYPE_FOLDER) {
            // 检查文件是否在分享文件夹内
            FileInfo file = fileService.getById(fileId);
            if (file == null)
                return false;
            return isSubfolder(share.getFolderId(), file.getFolderId(), share.getUserId())
                    || file.getFolderId().equals(share.getFolderId());
        } else if (shareType == SHARE_TYPE_BATCH) {
            // 检查是否在批量分享列表中
            List<ShareItem> items = shareItemMapper.selectByShareId(share.getId());
            for (ShareItem item : items) {
                if (item.getItemType() == ITEM_TYPE_FILE && item.getFileId().equals(fileId)) {
                    return true;
                }
                if (item.getItemType() == ITEM_TYPE_FOLDER) {
                    FileInfo file = fileService.getById(fileId);
                    if (file != null && (file.getFolderId().equals(item.getFolderId())
                            || isSubfolder(item.getFolderId(), file.getFolderId(), share.getUserId()))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void downloadFileStream(String shareCode, String password, Long fileId, HttpServletResponse response) {
        Share share = baseMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }

        checkShareValid(share);

        // 验证密码
        if (share.getPassword() != null && !share.getPassword().isEmpty()) {
            if (password == null || !share.getPassword().equals(password)) {
                throw new BusinessException(ResultCode.SHARE_PASSWORD_ERROR);
            }
        }

        // 验证文件是否在分享范围内
        if (!isFileInShareScope(share, fileId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不在分享范围内");
        }

        FileInfo fileInfo = fileService.getById(fileId);
        if (fileInfo == null) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }

        try (InputStream inputStream = minioUtils.downloadFile(fileInfo.getStoragePath());
                OutputStream outputStream = response.getOutputStream()) {

            String fileName = fileInfo.getFileName();
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileInfo.getFileSize()));

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();

            baseMapper.incrementDownloadCount(share.getId());
            log.info("分享文件下载成功: shareCode={}, fileId={}, fileName={}", shareCode, fileId, fileName);
        } catch (Exception e) {
            log.error("分享文件下载失败: shareCode={}, fileId={}", shareCode, fileId, e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public String getFilePreviewUrl(String shareCode, String password, Long fileId) {
        Share share = baseMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }

        checkShareValid(share);

        // 验证密码
        if (share.getPassword() != null && !share.getPassword().isEmpty()) {
            if (password == null || !share.getPassword().equals(password)) {
                throw new BusinessException(ResultCode.SHARE_PASSWORD_ERROR);
            }
        }

        // 验证文件是否在分享范围内
        if (!isFileInShareScope(share, fileId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不在分享范围内");
        }

        FileInfo file = fileService.getById(fileId);
        if (file == null) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }

        String fileUrl;
        if (kkFileViewConfig.getCallbackUrl() != null && !kkFileViewConfig.getCallbackUrl().isBlank()) {
            // kkFileView 在 Docker 内，通过分享 stream 接口代理文件
            String encodedName = java.net.URLEncoder.encode(file.getFileName(), java.nio.charset.StandardCharsets.UTF_8);
            String encodedPwd = password != null ?
                    "&password=" + java.net.URLEncoder.encode(password, java.nio.charset.StandardCharsets.UTF_8) : "";
            fileUrl = kkFileViewConfig.getCallbackUrl() + "/s/" + shareCode + "/stream/" + fileId
                    + "?" + encodedPwd + "&fullfilename=" + encodedName;
        } else {
            String presignedUrl = minioUtils.getPreviewUrl(file.getStoragePath(), file.getFileName(), 7200);
            fileUrl = presignedUrl;
        }
        return kkFileViewConfig.getPreviewUrl(fileUrl);
    }

    @Override
    public void streamFile(String shareCode, String password, Long fileId,
            String rangeHeader, HttpServletResponse response) {
        Share share = baseMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }

        checkShareValid(share);

        // 验证密码
        if (share.getPassword() != null && !share.getPassword().isEmpty()) {
            if (password == null || !share.getPassword().equals(password)) {
                throw new BusinessException(ResultCode.SHARE_PASSWORD_ERROR);
            }
        }

        // 验证文件是否在分享范围内
        if (!isFileInShareScope(share, fileId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不在分享范围内");
        }

        FileInfo fileInfo = fileService.getById(fileId);
        if (fileInfo == null) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }

        long fileSize = fileInfo.getFileSize();
        long start = 0;
        long end = fileSize - 1;

        try {
            // 解析 Range 请求头
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
                end = Math.min(end, fileSize - 1);
            }

            long contentLength = end - start + 1;

            // 设置响应状态和头
            response.setStatus(rangeHeader != null ?
                    HttpServletResponse.SC_PARTIAL_CONTENT : HttpServletResponse.SC_OK);

            String mimeType = fileInfo.getMimeType();
            if (mimeType == null || mimeType.isBlank()
                    || mimeType.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
                mimeType = inferMimeType(fileInfo.getFileExt());
            }
            response.setContentType(mimeType);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                    "Content-Range, Accept-Ranges, Content-Length, Content-Type");
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            response.setHeader(HttpHeaders.CONTENT_RANGE,
                    String.format("bytes %d-%d/%d", start, end, fileSize));
            response.setContentLengthLong(contentLength);

            try (InputStream inputStream = minioUtils.downloadFileRange(
                    fileInfo.getStoragePath(), start, contentLength);
                    OutputStream outputStream = response.getOutputStream()) {

                byte[] buffer = new byte[8192];
                long bytesRemaining = contentLength;
                int bytesRead;

                while (bytesRemaining > 0 &&
                        (bytesRead = inputStream.read(buffer, 0,
                                (int) Math.min(buffer.length, bytesRemaining))) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    bytesRemaining -= bytesRead;
                }
                outputStream.flush();
            }
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null &&
                    (message.contains("Connection reset by peer") ||
                            message.contains("Broken pipe") ||
                            message.contains("ClientAbortException"))) {
                log.debug("分享流式传输客户端中止: shareCode={}, fileId={}", shareCode, fileId);
            } else {
                log.error("分享流式传输失败: shareCode={}, fileId={}", shareCode, fileId, e);
            }
        }
    }

    /**
     * 根据文件扩展名推断 MIME 类型
     */
    private String inferMimeType(String fileExt) {
        if (fileExt == null || fileExt.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String ext = fileExt.toLowerCase();
        return switch (ext) {
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "ogg" -> "video/ogg";
            case "mov" -> "video/quicktime";
            case "avi" -> "video/x-msvideo";
            case "mkv" -> "video/x-matroska";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "aac" -> "audio/aac";
            case "flac" -> "audio/flac";
            case "m4a" -> "audio/mp4";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "bmp" -> "image/bmp";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "json" -> "application/json";
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    @Override
    public ShareVO toVO(Share share, boolean includePassword) {
        if (share == null) {
            return null;
        }
        ShareVO vo = new ShareVO();
        vo.setId(share.getId());
        vo.setShareType(share.getShareType() != null ? share.getShareType() : SHARE_TYPE_SINGLE);
        vo.setFileId(share.getFileId());
        vo.setFolderId(share.getFolderId());
        vo.setShareTitle(share.getShareTitle());
        vo.setTotalSize(share.getTotalSize());
        vo.setFileSizeStr(share.getTotalSize() != null ? SpaceVO.formatSize(share.getTotalSize()) : null);
        vo.setFileCount(share.getFileCount());
        vo.setShareCode(share.getShareCode());
        vo.setShareUrl(contextPath + "/s/" + share.getShareCode());
        vo.setHasPassword(share.getPassword() != null && !share.getPassword().isEmpty());
        if (includePassword) {
            vo.setPassword(share.getPassword());
        }
        vo.setExpireTime(share.getExpireTime());
        vo.setExpired(share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now()));
        vo.setViewCount(share.getViewCount());
        vo.setDownloadCount(share.getDownloadCount());
        vo.setMaxViewCount(share.getMaxViewCount());
        vo.setStatus(share.getStatus());
        vo.setCreateTime(share.getCreateTime());
        return vo;
    }

    /**
     * 检查分享是否有效
     */
    private void checkShareValid(Share share) {
        if (share.getStatus() == ShareStatusEnum.CANCELLED.getCode()) {
            throw new BusinessException(ResultCode.SHARE_CANCELLED);
        }

        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.SHARE_EXPIRED);
        }

        if (share.getMaxViewCount() > 0 && share.getViewCount() >= share.getMaxViewCount()) {
            throw new BusinessException(ResultCode.SHARE_EXPIRED, "分享已达到最大访问次数");
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null)
            return null;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return null;
    }

    /**
     * 生成分享码（6位字母数字）
     */
    private String generateShareCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    /**
     * 生成提取码（4位字母数字）
     */
    private String generatePassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 4);
    }
}

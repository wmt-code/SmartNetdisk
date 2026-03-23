package com.wmt.smartnetdisk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmt.smartnetdisk.common.enums.FileTypeEnum;
import com.wmt.smartnetdisk.common.exception.BusinessException;
import com.wmt.smartnetdisk.common.result.PageResult;
import com.wmt.smartnetdisk.common.result.ResultCode;
import com.wmt.smartnetdisk.dto.request.FastUploadDTO;
import com.wmt.smartnetdisk.dto.request.FileListDTO;
import com.wmt.smartnetdisk.entity.FileInfo;
import com.wmt.smartnetdisk.entity.User;
import com.wmt.smartnetdisk.mapper.FileInfoMapper;
import com.wmt.smartnetdisk.service.IAiService;
import com.wmt.smartnetdisk.service.IFileService;
import com.wmt.smartnetdisk.service.IFolderService;
import com.wmt.smartnetdisk.service.IUserService;
import com.wmt.smartnetdisk.utils.MinioUtils;
import com.wmt.smartnetdisk.vo.FileVO;
import com.wmt.smartnetdisk.vo.FolderVO;
import com.wmt.smartnetdisk.vo.SpaceVO;
import com.wmt.smartnetdisk.vo.UploadResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件服务实现类
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements IFileService {

    /** 自动向量化的最大文件大小：50KB */
    private static final long AUTO_VECTORIZE_MAX_SIZE = 50 * 1024;

    private final IUserService userService;
    private final MinioUtils minioUtils;
    private final com.wmt.smartnetdisk.config.KkFileViewConfig kkFileViewConfig;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private IAiService aiService;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private IFolderService folderService;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private com.wmt.smartnetdisk.service.INotificationService notificationService;

    @Override
    public PageResult<FileVO> listFiles(Long userId, FileListDTO listDTO) {
        // 无过滤条件时，使用优化的联合查询（单次查询）
        boolean hasFileTypeFilter = listDTO.getFileType() != null && !listDTO.getFileType().isBlank();
        boolean hasKeywordFilter = listDTO.getKeyword() != null && !listDTO.getKeyword().isBlank();

        if (!hasFileTypeFilter && !hasKeywordFilter) {
            return listFilesOptimized(userId, listDTO);
        }

        // 有过滤条件时，只查询文件（不含文件夹）
        return listFilesFiltered(userId, listDTO);
    }

    /**
     * 优化的文件列表查询（无过滤条件时使用）
     * 使用 UNION ALL 单次查询文件夹和文件
     */
    private PageResult<FileVO> listFilesOptimized(Long userId, FileListDTO listDTO) {
        long offset = (listDTO.getPageNum() - 1) * listDTO.getPageSize();
        int limit = listDTO.getPageSize();

        // 单次查询获取文件夹+文件（已分页）
        List<java.util.Map<String, Object>> rows = baseMapper.listFilesAndFolders(
                userId, listDTO.getFolderId(), offset, limit);

        // 获取总数
        long total = baseMapper.countFilesAndFolders(userId, listDTO.getFolderId());

        // 转换为 FileVO
        List<FileVO> resultList = rows.stream().map(this::mapToFileVO).toList();

        return PageResult.of((long) listDTO.getPageNum(), (long) listDTO.getPageSize(), total, resultList);
    }

    /**
     * 带过滤条件的文件查询（文件类型/关键词）
     */
    private PageResult<FileVO> listFilesFiltered(Long userId, FileListDTO listDTO) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getDeleted, 0)
                .eq(FileInfo::getFolderId, listDTO.getFolderId());

        // 文件类型过滤
        if (listDTO.getFileType() != null && !listDTO.getFileType().isBlank()) {
            wrapper.eq(FileInfo::getFileType, listDTO.getFileType());
        }

        // 关键词搜索（同时匹配文件名和AI摘要）
        if (listDTO.getKeyword() != null && !listDTO.getKeyword().isBlank()) {
            String keyword = listDTO.getKeyword();
            wrapper.and(w -> w
                .like(FileInfo::getFileName, keyword)
                .or()
                .like(FileInfo::getAiSummary, keyword)
            );
        }

        // 排序
        if (listDTO.getOrderBy() != null && !listDTO.getOrderBy().isBlank()) {
            if (Boolean.TRUE.equals(listDTO.getIsAsc())) {
                wrapper.orderByAsc(FileInfo::getCreateTime);
            } else {
                wrapper.orderByDesc(FileInfo::getCreateTime);
            }
        } else {
            wrapper.orderByDesc(FileInfo::getCreateTime);
        }

        Page<FileInfo> page = new Page<>(listDTO.getPageNum(), listDTO.getPageSize());
        Page<FileInfo> result = baseMapper.selectPage(page, wrapper);

        List<FileVO> fileVoList = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), fileVoList);
    }

    /**
     * 将 Map 转换为 FileVO（用于联合查询结果）
     */
    private FileVO mapToFileVO(java.util.Map<String, Object> row) {
        FileVO vo = new FileVO();
        vo.setId(((Number) row.get("id")).longValue());
        vo.setFileName((String) row.get("file_name"));
        vo.setFileSize(((Number) row.get("file_size")).longValue());
        vo.setFileSizeStr("folder".equals(row.get("file_type")) ? "-" : SpaceVO.formatSize(vo.getFileSize()));
        vo.setFileType((String) row.get("file_type"));
        vo.setFileExt((String) row.get("file_ext"));
        vo.setThumbnailPath((String) row.get("thumbnail_path"));
        Object isVectorized = row.get("is_vectorized");
        vo.setIsVectorized(isVectorized != null && ((Number) isVectorized).intValue() == 1);
        vo.setFolderId(((Number) row.get("folder_id")).longValue());
        // 处理时间类型（SQL返回Timestamp需要转换）
        vo.setCreateTime(convertToLocalDateTime(row.get("create_time")));
        vo.setUpdateTime(convertToLocalDateTime(row.get("update_time")));
        vo.setAiSummary((String) row.get("ai_summary"));
        return vo;
    }

    /**
     * 将数据库时间对象转换为 LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Object timeObj) {
        if (timeObj == null)
            return null;
        if (timeObj instanceof LocalDateTime ldt)
            return ldt;
        if (timeObj instanceof java.sql.Timestamp ts)
            return ts.toLocalDateTime();
        return null;
    }

    /**
     * 将 FolderVO 转换为 FileVO（用于统一列表显示）
     * 注意：为了性能，文件夹大小不在列表中计算（参照百度/夸克网盘模式）
     */
    private FileVO folderToFileVO(FolderVO folder, Long userId) {
        FileVO vo = new FileVO();
        vo.setId(folder.getId());
        vo.setFileName(folder.getFolderName());
        // 文件夹大小设为0，前端显示为"-"（避免递归查询性能问题）
        vo.setFileSize(0L);
        vo.setFileSizeStr("-");
        vo.setFileType("folder");
        vo.setFileExt("");
        vo.setThumbnailPath(null);
        vo.setIsVectorized(false);
        vo.setFolderId(folder.getParentId());
        vo.setCreateTime(folder.getCreateTime());
        vo.setUpdateTime(folder.getCreateTime()); // FolderVO 没有 updateTime，使用 createTime
        return vo;
    }

    @Override
    public PageResult<FileVO> listRecentFiles(Long userId, FileListDTO listDTO) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
               .eq(FileInfo::getDeleted, 0)
               .isNotNull(FileInfo::getLastAccessTime)
               .ne(FileInfo::getFileType, "folder");  // 不显示文件夹

        // 文件类型过滤
        if (listDTO.getFileType() != null && !listDTO.getFileType().isBlank()) {
            wrapper.eq(FileInfo::getFileType, listDTO.getFileType());
        }

        // 关键词搜索（同时匹配文件名和AI摘要）
        if (listDTO.getKeyword() != null && !listDTO.getKeyword().isBlank()) {
            String keyword = listDTO.getKeyword();
            wrapper.and(w -> w
                .like(FileInfo::getFileName, keyword)
                .or()
                .like(FileInfo::getAiSummary, keyword)
            );
        }

        // 按最近访问时间倒序
        wrapper.orderByDesc(FileInfo::getLastAccessTime);

        int pageNum = listDTO.getPageNum() != null ? listDTO.getPageNum() : 1;
        int pageSize = listDTO.getPageSize() != null ? listDTO.getPageSize() : 20;
        Page<FileInfo> page = new Page<>(pageNum, pageSize);
        Page<FileInfo> result = this.page(page, wrapper);

        List<FileVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), voList);
    }

    @Override
    public Long calculateFolderSize(Long userId, Long folderId) {
        // 使用 PostgreSQL 递归 CTE 一次性计算所有子文件夹的文件大小
        java.util.Map<String, Object> result = baseMapper.sumFileSizeRecursive(userId, folderId);
        if (result != null) {
            Object totalSizeObj = result.get("totalsize");
            if (totalSizeObj instanceof Number) {
                return ((Number) totalSizeObj).longValue();
            }
        }
        return 0L;
    }

    @Override
    public FileInfo getByMd5(String fileMd5) {
        return baseMapper.selectByMd5(fileMd5);
    }

    @Override
    public FileVO getFileDetail(Long userId, Long fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null || !fileInfo.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        return toVO(fileInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameFile(Long userId, Long fileId, String newName) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null || !fileInfo.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        fileInfo.setFileName(newName);
        updateById(fileInfo);
        log.info("文件重命名成功: fileId={}, newName={}", fileId, newName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveFile(Long userId, Long fileId, Long targetFolderId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null || !fileInfo.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        fileInfo.setFolderId(targetFolderId);
        updateById(fileInfo);
        log.info("文件移动成功: fileId={}, targetFolderId={}", fileId, targetFolderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMoveFiles(Long userId, List<Long> fileIds, Long targetFolderId) {
        for (Long fileId : fileIds) {
            moveFile(userId, fileId, targetFolderId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileVO copyFile(Long userId, Long fileId, Long targetFolderId) {
        // 获取原文件信息
        FileInfo sourceFile = getById(fileId);
        if (sourceFile == null || !sourceFile.getUserId().equals(userId) || sourceFile.getDeleted() == 1) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }

        // 检查用户空间是否足够
        User user = userService.getById(userId);
        if (user.getUsedSpace() + sourceFile.getFileSize() > user.getTotalSpace()) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEED, "存储空间不足");
        }

        // 生成新文件名（如果目标文件夹有同名文件则添加副本后缀）
        String newFileName = generateCopyFileName(userId, targetFolderId, sourceFile.getFileName());

        // 创建文件副本记录（共享同一存储路径，不复制实际文件）
        FileInfo newFile = new FileInfo();
        newFile.setUserId(userId);
        newFile.setFolderId(targetFolderId);
        newFile.setFileName(newFileName);
        newFile.setFileMd5(sourceFile.getFileMd5());
        newFile.setFileSize(sourceFile.getFileSize());
        newFile.setFileType(sourceFile.getFileType());
        newFile.setFileExt(sourceFile.getFileExt());
        newFile.setMimeType(sourceFile.getMimeType());
        newFile.setStoragePath(sourceFile.getStoragePath()); // 共享存储路径
        newFile.setThumbnailPath(sourceFile.getThumbnailPath());
        newFile.setIsVectorized(0); // 副本不继承向量化状态
        newFile.setStatus(1);
        save(newFile);

        // 更新用户已用空间（副本也占用空间配额）
        userService.updateUsedSpace(userId, sourceFile.getFileSize());

        // 如果文件类型支持向量化，异步触发向量化
        if (aiService.isFileVectorizable(newFile.getFileExt()) && newFile.getFileSize() <= AUTO_VECTORIZE_MAX_SIZE) {
            final Long uid = userId;
            final Long fid = newFile.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    aiService.vectorizeDocumentAsync(uid, fid);
                }
            });
        }

        log.info("文件复制成功: userId={}, sourceFileId={}, newFileId={}, targetFolderId={}",
                userId, fileId, newFile.getId(), targetFolderId);
        return toVO(newFile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FileVO> batchCopyFiles(Long userId, List<Long> fileIds, Long targetFolderId) {
        return fileIds.stream()
                .map(fileId -> copyFile(userId, fileId, targetFolderId))
                .toList();
    }

    /**
     * 生成复制文件名
     * <p>
     * 如果目标文件夹已存在同名文件，则添加 "副本"、"副本(2)" 等后缀
     * </p>
     */
    private String generateCopyFileName(Long userId, Long folderId, String originalFileName) {
        // 检查目标文件夹是否有同名文件
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFolderId, folderId)
                .eq(FileInfo::getDeleted, 0)
                .eq(FileInfo::getFileName, originalFileName);

        if (count(wrapper) == 0) {
            return originalFileName;
        }

        // 分离文件名和扩展名
        String baseName;
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf(".");
        if (dotIndex > 0) {
            baseName = originalFileName.substring(0, dotIndex);
            extension = originalFileName.substring(dotIndex);
        } else {
            baseName = originalFileName;
        }

        // 尝试生成不重复的文件名
        String newFileName = baseName + " - 副本" + extension;
        int copyIndex = 2;

        while (true) {
            LambdaQueryWrapper<FileInfo> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(FileInfo::getUserId, userId)
                    .eq(FileInfo::getFolderId, folderId)
                    .eq(FileInfo::getDeleted, 0)
                    .eq(FileInfo::getFileName, newFileName);

            if (count(checkWrapper) == 0) {
                return newFileName;
            }

            newFileName = baseName + " - 副本(" + copyIndex + ")" + extension;
            copyIndex++;

            // 防止无限循环
            if (copyIndex > 100) {
                throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "文件名重复过多，请手动重命名");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long userId, Long fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null || !fileInfo.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        // 检查文件是否已删除
        if (fileInfo.getDeleted() == 1) {
            log.warn("文件已在回收站中: fileId={}", fileId);
            return;
        }
        // 软删除（移入回收站）
        fileInfo.setDeleted(1);
        fileInfo.setDeleteTime(LocalDateTime.now());
        boolean updated = updateById(fileInfo);
        if (!updated) {
            log.error("文件删除失败，数据库更新失败: fileId={}", fileId);
            throw new BusinessException(ResultCode.DATA_UPDATE_FAIL, "文件删除失败");
        }
        log.info("文件移入回收站成功: fileId={}, fileName={}", fileId, fileInfo.getFileName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteFiles(Long userId, List<Long> fileIds) {
        for (Long fileId : fileIds) {
            deleteFile(userId, fileId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permanentDeleteFile(Long userId, Long fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null || !fileInfo.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        // 删除 MinIO 存储的文件
        try {
            minioUtils.deleteFile(fileInfo.getStoragePath());
        } catch (Exception e) {
            log.warn("MinIO 文件删除失败: {}", fileInfo.getStoragePath(), e);
        }
        // 更新用户已用空间
        userService.updateUsedSpace(userId, -fileInfo.getFileSize());
        // 彻底删除
        removeById(fileId);
        log.info("文件彻底删除: fileId={}", fileId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreFile(Long userId, Long fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null || !fileInfo.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        fileInfo.setDeleted(0);
        fileInfo.setDeleteTime(null);
        updateById(fileInfo);
        log.info("文件恢复成功: fileId={}", fileId);
    }

    @Override
    public PageResult<FileVO> listRecycledFiles(Long userId, FileListDTO listDTO) {
        long offset = (listDTO.getPageNum() - 1) * listDTO.getPageSize();
        int limit = listDTO.getPageSize();

        // 联合查询已删除的文件夹和文件
        List<java.util.Map<String, Object>> rows = baseMapper.listRecycledFilesAndFolders(
                userId, offset, limit);

        // 获取总数
        long total = baseMapper.countRecycledFilesAndFolders(userId);

        // 转换为 FileVO
        List<FileVO> resultList = rows.stream().map(this::mapToFileVO).toList();

        return PageResult.of((long) listDTO.getPageNum(), (long) listDTO.getPageSize(), total, resultList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearRecycleBin(Long userId) {
        // 1. 获取所有已删除的文件（deleted=1）
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getDeleted, 1);
        List<FileInfo> recycledFiles = list(wrapper);

        // 2. 彻底删除文件
        for (FileInfo file : recycledFiles) {
            permanentDeleteFile(userId, file.getId());
        }

        // 3. 清空文件夹回收站
        folderService.clearRecycleBin(userId);

        log.info("回收站清空成功: userId={}, fileCount={}", userId, recycledFiles.size());
    }

    @Override
    public FileVO toVO(FileInfo fileInfo) {
        if (fileInfo == null) {
            return null;
        }
        FileVO vo = new FileVO();
        vo.setId(fileInfo.getId());
        vo.setFileName(fileInfo.getFileName());
        vo.setFileSize(fileInfo.getFileSize());
        vo.setFileSizeStr(SpaceVO.formatSize(fileInfo.getFileSize()));
        vo.setFileType(fileInfo.getFileType());
        vo.setFileExt(fileInfo.getFileExt());
        vo.setThumbnailPath(fileInfo.getThumbnailPath());
        vo.setIsVectorized(fileInfo.getIsVectorized() == 1);
        vo.setFolderId(fileInfo.getFolderId());
        vo.setCreateTime(fileInfo.getCreateTime());
        vo.setUpdateTime(fileInfo.getUpdateTime());
        vo.setAiSummary(fileInfo.getAiSummary());
        return vo;
    }

    // ==================== 文件上传相关 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultVO checkFastUpload(Long userId, FastUploadDTO fastUploadDTO) {
        // 根据 MD5 查找已存在的文件
        FileInfo existFile = getByMd5(fastUploadDTO.getFileMd5());
        if (existFile == null) {
            // 不存在相同文件，需要上传
            return null;
        }

        // 检查用户空间是否足够
        User user = userService.getById(userId);
        if (user.getUsedSpace() + fastUploadDTO.getFileSize() > user.getTotalSpace()) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEED, "存储空间不足");
        }

        // 创建新的文件记录（秒传）
        FileInfo newFile = new FileInfo();
        newFile.setUserId(userId);
        newFile.setFolderId(fastUploadDTO.getFolderId());
        newFile.setFileName(fastUploadDTO.getFileName());
        newFile.setFileMd5(existFile.getFileMd5());
        newFile.setFileSize(existFile.getFileSize());
        newFile.setFileType(existFile.getFileType());
        newFile.setFileExt(existFile.getFileExt());
        newFile.setMimeType(existFile.getMimeType());
        newFile.setStoragePath(existFile.getStoragePath()); // 复用存储路径
        newFile.setStatus(1);
        save(newFile);

        // 更新用户已用空间
        userService.updateUsedSpace(userId, fastUploadDTO.getFileSize());

        log.info("秒传成功: userId={}, fileName={}", userId, fastUploadDTO.getFileName());

        // 如果文件类型支持向量化，异步触发向量化
        String fileExt = "";
        int dotIndex = fastUploadDTO.getFileName().lastIndexOf(".");
        if (dotIndex >= 0) {
            fileExt = fastUploadDTO.getFileName().substring(dotIndex + 1);
        }
        if (aiService.isFileVectorizable(fileExt) && newFile.getFileSize() <= AUTO_VECTORIZE_MAX_SIZE) {
            final Long uid = userId;
            final Long fid = newFile.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    aiService.vectorizeDocumentAsync(uid, fid);
                }
            });
            log.info("已触发异步向量化(秒传): fileId={}, fileExt={}", newFile.getId(), fileExt);
        }

        notificationService.createNotification(userId, "upload", "文件上传成功", "文件 " + fastUploadDTO.getFileName() + " 已上传", newFile.getId());

        return UploadResultVO.fastUpload(newFile.getId(), newFile.getFileName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultVO uploadFile(Long userId, MultipartFile file, Long folderId) {
        long startTime = System.currentTimeMillis();
        String originalFilename = file.getOriginalFilename();

        // 检查用户空间是否足够
        User user = userService.getById(userId);
        if (user.getUsedSpace() + file.getSize() > user.getTotalSpace()) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEED, "存储空间不足");
        }

        // 获取文件信息
        String fileExt = minioUtils.getFileExtension(originalFilename);
        String fileType = FileTypeEnum.getByExtension(fileExt).getCode();

        // 一次读取完成 MD5 计算和 MinIO 上传（优化：避免读取文件两次）
        log.info("开始上传文件(优化模式): fileName={}, size={}", originalFilename, file.getSize());
        String[] uploadResult = minioUtils.uploadFileWithMd5(file, userId);
        String storagePath = uploadResult[0];
        String fileMd5 = uploadResult[1];
        long uploadEndTime = System.currentTimeMillis();
        log.info("文件上传和MD5计算完成: fileName={}, md5={}, 耗时={}ms", originalFilename, fileMd5, uploadEndTime - startTime);

        // 保存文件记录
        FileInfo newFile = new FileInfo();
        newFile.setUserId(userId);
        newFile.setFolderId(folderId);
        newFile.setFileName(originalFilename);
        newFile.setFileMd5(fileMd5);
        newFile.setFileSize(file.getSize());
        newFile.setFileType(fileType);
        newFile.setFileExt(fileExt);
        newFile.setMimeType(file.getContentType());
        newFile.setStoragePath(storagePath);
        newFile.setStatus(1);
        save(newFile);

        // 更新用户已用空间
        userService.updateUsedSpace(userId, file.getSize());

        long endTime = System.currentTimeMillis();
        log.info("文件上传成功: userId={}, fileName={}, size={}, 总耗时={}ms", userId, originalFilename, file.getSize(),
                endTime - startTime);

        // 小文件自动向量化（≤50KB），大文件需用户手动触发
        if (aiService.isFileVectorizable(fileExt) && newFile.getFileSize() <= AUTO_VECTORIZE_MAX_SIZE) {
            final Long uid = userId;
            final Long fid = newFile.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    aiService.vectorizeDocumentAsync(uid, fid);
                }
            });
            log.info("已触发异步向量化: fileId={}, fileExt={}", newFile.getId(), fileExt);
        }

        notificationService.createNotification(userId, "upload", "文件上传成功", "文件 " + originalFilename + " 已上传", newFile.getId());

        return UploadResultVO.normalUpload(newFile.getId(), originalFilename, file.getSize(), fileMd5);
    }

    @Override
    public String getDownloadUrl(Long userId, Long fileId, int expiry) {
        FileInfo fileInfo = getFileWithPermission(userId, fileId);
        return minioUtils.getDownloadUrl(fileInfo.getStoragePath(), fileInfo.getFileName(), expiry);
    }

    @Override
    public String getPreviewUrl(Long userId, Long fileId, int expiry) {
        FileInfo fileInfo = getFileWithPermission(userId, fileId);
        return minioUtils.getPreviewUrl(fileInfo.getStoragePath(), fileInfo.getMimeType(), expiry);
    }

    @Override
    public String getKkFileViewPreviewUrl(Long userId, Long fileId, int expiry) {
        FileInfo fileInfo = getFileWithPermission(userId, fileId);

        String fileUrl;
        if (kkFileViewConfig.getCallbackUrl() != null && !kkFileViewConfig.getCallbackUrl().isBlank()) {
            // kkFileView 在 Docker 内，通过后端 stream 接口代理文件
            String token = cn.dev33.satoken.stp.StpUtil.getTokenValue();
            String encodedName = java.net.URLEncoder.encode(fileInfo.getFileName(), java.nio.charset.StandardCharsets.UTF_8);
            fileUrl = kkFileViewConfig.getCallbackUrl() + "/file/" + fileId + "/stream?satoken=" + token
                    + "&fullfilename=" + encodedName;
        } else {
            // kkFileView 可直接访问 MinIO，使用 presigned URL
            fileUrl = minioUtils.getPresignedUrl(fileInfo.getStoragePath(), expiry);
        }

        return kkFileViewConfig.getPreviewUrl(fileUrl);
    }

    @Override
    public FileInfo getFileWithPermission(Long userId, Long fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null || !fileInfo.getUserId().equals(userId) || fileInfo.getDeleted() == 1) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        return fileInfo;
    }

    /**
     * 支持在线编辑的文件扩展名
     */
    private static final java.util.Set<String> EDITABLE_EXTENSIONS = java.util.Set.of(
            // 文本文件
            "txt", "md", "markdown", "log",
            // 配置文件
            "json", "xml", "yml", "yaml", "toml", "ini", "conf", "cfg", "properties",
            // Web 相关
            "html", "htm", "css", "scss", "sass", "less", "js", "ts", "jsx", "tsx", "vue", "svelte",
            // 编程语言
            "java", "py", "go", "rs", "c", "cpp", "h", "hpp", "cs", "rb", "php", "swift", "kt", "kts",
            "scala", "groovy", "r", "lua", "pl", "pm", "sh", "bash", "zsh", "fish", "bat", "cmd", "ps1",
            // 数据库
            "sql",
            // 其他
            "gitignore", "dockerignore", "editorconfig", "env");

    /**
     * 最大可编辑文件大小：100MB（支持分块加载）
     */
    private static final long MAX_EDITABLE_SIZE = 100 * 1024 * 1024;

    /**
     * 默认分块大小：1MB
     */
    private static final long DEFAULT_CHUNK_SIZE = 1024 * 1024;

    /**
     * 最大分块大小：5MB
     */
    private static final long MAX_CHUNK_SIZE = 5 * 1024 * 1024;

    @Override
    public java.util.Map<String, Object> getFileContent(Long userId, Long fileId, Long offset, Long limit) {
        FileInfo fileInfo = getFileWithPermission(userId, fileId);

        // 检查文件扩展名是否支持编辑
        String ext = fileInfo.getFileExt() != null ? fileInfo.getFileExt().toLowerCase() : "";
        if (!EDITABLE_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持编辑此类型文件");
        }

        // 检查文件大小
        if (fileInfo.getFileSize() > MAX_EDITABLE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件过大，无法在线编辑（最大100MB）");
        }

        // 处理默认值
        long actualOffset = (offset != null && offset > 0) ? offset : 0;
        long actualLimit = (limit != null && limit > 0) ? Math.min(limit, MAX_CHUNK_SIZE) : DEFAULT_CHUNK_SIZE;

        // 检查偏移量是否超出文件大小
        long fileSize = fileInfo.getFileSize();
        if (actualOffset >= fileSize) {
            actualOffset = fileSize;
            actualLimit = 0;
        }

        // 计算实际读取的字节数（不能超过文件末尾）
        long bytesToRead = Math.min(actualLimit, fileSize - actualOffset);
        boolean hasMore = (actualOffset + bytesToRead) < fileSize;

        // 读取文件内容（分块）
        String content = "";
        if (bytesToRead > 0) {
            try (java.io.InputStream inputStream = minioUtils.downloadFile(fileInfo.getStoragePath())) {
                // 跳过前面的字节
                long skipped = inputStream.skip(actualOffset);
                if (skipped != actualOffset) {
                    log.warn("文件跳转字节数不匹配: expected={}, actual={}", actualOffset, skipped);
                }

                // 读取指定大小的内容
                byte[] buffer = new byte[(int) bytesToRead];
                int totalRead = 0;
                int bytesRead;
                while (totalRead < bytesToRead
                        && (bytesRead = inputStream.read(buffer, totalRead, (int) (bytesToRead - totalRead))) != -1) {
                    totalRead += bytesRead;
                }

                content = new String(buffer, 0, totalRead, java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                log.error("读取文件内容失败: fileId={}, offset={}, limit={}", fileId, actualOffset, actualLimit, e);
                throw new BusinessException(ResultCode.FILE_NOT_FOUND, "读取文件失败");
            }
        }

        // 构建返回结果
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("content", content);
        result.put("fileName", fileInfo.getFileName());
        result.put("fileExt", fileInfo.getFileExt());
        result.put("fileSize", fileSize);
        result.put("mimeType", fileInfo.getMimeType());
        result.put("offset", actualOffset);
        result.put("limit", actualLimit);
        result.put("bytesRead", bytesToRead);
        result.put("hasMore", hasMore);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFileContent(Long userId, Long fileId, String content) {
        FileInfo fileInfo = getFileWithPermission(userId, fileId);

        // 检查文件扩展名是否支持编辑
        String ext = fileInfo.getFileExt() != null ? fileInfo.getFileExt().toLowerCase() : "";
        if (!EDITABLE_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持编辑此类型文件");
        }

        // 将内容转换为字节数组
        byte[] contentBytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long newSize = contentBytes.length;
        long oldSize = fileInfo.getFileSize();
        long sizeDiff = newSize - oldSize;

        // 检查用户空间是否足够
        if (sizeDiff > 0) {
            User user = userService.getById(userId);
            if (user.getUsedSpace() + sizeDiff > user.getTotalSpace()) {
                throw new BusinessException(ResultCode.FILE_SIZE_EXCEED, "存储空间不足");
            }
        }

        // 上传新内容到 MinIO（覆盖原文件）
        String mimeType = fileInfo.getMimeType();
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "text/plain; charset=utf-8";
        }
        minioUtils.uploadContent(contentBytes, fileInfo.getStoragePath(), mimeType);

        // 更新文件记录
        fileInfo.setFileSize(newSize);
        // 计算新的 MD5
        String newMd5 = calculateMd5(contentBytes);
        fileInfo.setFileMd5(newMd5);
        updateById(fileInfo);

        // 更新用户已用空间
        if (sizeDiff != 0) {
            userService.updateUsedSpace(userId, sizeDiff);
        }

        log.info("文件内容保存成功: fileId={}, oldSize={}, newSize={}", fileId, oldSize, newSize);
    }

    /**
     * 计算字节数组的 MD5 值
     */
    private String calculateMd5(byte[] content) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new BusinessException(ResultCode.DATA_UPDATE_FAIL, "MD5计算失败");
        }
    }
}

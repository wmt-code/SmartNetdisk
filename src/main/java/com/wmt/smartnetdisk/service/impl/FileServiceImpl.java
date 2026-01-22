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
import com.wmt.smartnetdisk.service.IFileService;
import com.wmt.smartnetdisk.service.IUserService;
import com.wmt.smartnetdisk.utils.Md5Utils;
import com.wmt.smartnetdisk.utils.MinioUtils;
import com.wmt.smartnetdisk.vo.FileVO;
import com.wmt.smartnetdisk.vo.SpaceVO;
import com.wmt.smartnetdisk.vo.UploadResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private final IUserService userService;
    private final MinioUtils minioUtils;

    @Override
    public PageResult<FileVO> listFiles(Long userId, FileListDTO listDTO) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getDeleted, 0)
                .eq(FileInfo::getFolderId, listDTO.getFolderId());

        // 文件类型过滤
        if (listDTO.getFileType() != null && !listDTO.getFileType().isBlank()) {
            wrapper.eq(FileInfo::getFileType, listDTO.getFileType());
        }

        // 关键词搜索
        if (listDTO.getKeyword() != null && !listDTO.getKeyword().isBlank()) {
            wrapper.like(FileInfo::getFileName, listDTO.getKeyword());
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

        List<FileVO> voList = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), voList);
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
        // 软删除（移入回收站）
        fileInfo.setDeleted(1);
        fileInfo.setDeleteTime(LocalDateTime.now());
        updateById(fileInfo);
        log.info("文件移入回收站: fileId={}", fileId);
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
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getDeleted, 1)
                .orderByDesc(FileInfo::getDeleteTime);

        Page<FileInfo> page = new Page<>(listDTO.getPageNum(), listDTO.getPageSize());
        Page<FileInfo> result = baseMapper.selectPage(page, wrapper);

        List<FileVO> voList = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), voList);
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
        return UploadResultVO.fastUpload(newFile.getId(), newFile.getFileName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultVO uploadFile(Long userId, MultipartFile file, Long folderId) {
        // 检查用户空间是否足够
        User user = userService.getById(userId);
        if (user.getUsedSpace() + file.getSize() > user.getTotalSpace()) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEED, "存储空间不足");
        }

        // 计算文件 MD5
        String fileMd5 = Md5Utils.calculateMd5(file);
        if (fileMd5 == null) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAIL, "文件校验失败");
        }

        // 获取文件信息
        String originalFilename = file.getOriginalFilename();
        String fileExt = minioUtils.getFileExtension(originalFilename);
        String fileType = FileTypeEnum.getByExtension(fileExt).getCode();

        // 上传到 MinIO
        String storagePath = minioUtils.uploadFile(file, userId, fileMd5);

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

        log.info("文件上传成功: userId={}, fileName={}, size={}", userId, originalFilename, file.getSize());
        return UploadResultVO.normalUpload(newFile.getId(), originalFilename, file.getSize(), fileMd5);
    }

    @Override
    public String getFileUrl(Long userId, Long fileId, int expiry) {
        FileInfo fileInfo = getFileWithPermission(userId, fileId);
        return minioUtils.getPresignedUrl(fileInfo.getStoragePath(), expiry);
    }

    @Override
    public FileInfo getFileWithPermission(Long userId, Long fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null || !fileInfo.getUserId().equals(userId) || fileInfo.getDeleted() == 1) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        return fileInfo;
    }
}

package com.wmt.smartnetdisk.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmt.smartnetdisk.common.constant.RedisKeyConstants;
import com.wmt.smartnetdisk.common.enums.FileTypeEnum;
import com.wmt.smartnetdisk.common.enums.UploadStatusEnum;
import com.wmt.smartnetdisk.common.exception.BusinessException;
import com.wmt.smartnetdisk.common.result.ResultCode;
import com.wmt.smartnetdisk.dto.request.ChunkMergeDTO;
import com.wmt.smartnetdisk.dto.request.ChunkUploadDTO;
import com.wmt.smartnetdisk.dto.request.FastUploadDTO;
import com.wmt.smartnetdisk.entity.FileChunk;
import com.wmt.smartnetdisk.entity.FileInfo;
import com.wmt.smartnetdisk.mapper.FileChunkMapper;
import com.wmt.smartnetdisk.service.IAiService;
import com.wmt.smartnetdisk.service.IFileChunkService;
import com.wmt.smartnetdisk.service.IFileService;
import com.wmt.smartnetdisk.service.IUserService;
import com.wmt.smartnetdisk.utils.MinioUtils;
import com.wmt.smartnetdisk.vo.ChunkCheckResultVO;
import com.wmt.smartnetdisk.vo.UploadResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 文件分片服务实现类
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileChunkServiceImpl extends ServiceImpl<FileChunkMapper, FileChunk> implements IFileChunkService {

    private final FileChunkMapper fileChunkMapper;
    private final MinioUtils minioUtils;
    private final StringRedisTemplate redisTemplate;
    private final IUserService userService;
    @Lazy
    private final IFileService fileService;
    private final com.wmt.smartnetdisk.service.INotificationService notificationService;
    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private IAiService aiService;

    @Override
    public ChunkCheckResultVO checkChunks(Long userId, FastUploadDTO fastUploadDTO) {
        String fileMd5 = fastUploadDTO.getFileMd5();

        // 1. 先检查是否可以秒传（文件已存在）
        FileInfo existingFile = fileService.getByMd5(fileMd5);
        if (existingFile != null) {
            // 秒传：复制文件记录
            FileInfo newFile = new FileInfo();
            newFile.setUserId(userId);
            newFile.setFolderId(fastUploadDTO.getFolderId() != null ? fastUploadDTO.getFolderId() : 0L);
            newFile.setFileName(fastUploadDTO.getFileName());
            newFile.setFileMd5(fileMd5);
            newFile.setFileSize(fastUploadDTO.getFileSize());
            newFile.setFileExt(minioUtils.getFileExtension(fastUploadDTO.getFileName()));
            newFile.setFileType(FileTypeEnum.getByExtension(newFile.getFileExt()).name().toLowerCase());
            newFile.setStoragePath(existingFile.getStoragePath());
            newFile.setStatus(UploadStatusEnum.COMPLETED.getCode());
            newFile.setCreateTime(LocalDateTime.now());
            newFile.setUpdateTime(LocalDateTime.now());

            fileService.save(newFile);
            userService.updateUsedSpace(userId, fastUploadDTO.getFileSize());

            log.info("分片秒传成功: userId={}, fileName={}, md5={}", userId, fastUploadDTO.getFileName(), fileMd5);

            UploadResultVO result = new UploadResultVO();
            result.setFileId(newFile.getId());
            result.setFileName(newFile.getFileName());
            result.setFileSize(newFile.getFileSize());
            result.setFastUpload(true);

            return ChunkCheckResultVO.fastUploaded(result);
        }

        // 2. 查询已上传的分片（断点续传）
        List<Integer> uploadedChunks = getUploadedChunkIndexes(fileMd5);
        log.info("分片检查: fileMd5={}, uploadedChunks={}", fileMd5, uploadedChunks.size());

        return ChunkCheckResultVO.needUpload(uploadedChunks);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadChunk(Long userId, ChunkUploadDTO uploadDTO, MultipartFile chunkFile) {
        String fileMd5 = uploadDTO.getFileMd5();
        Integer chunkIndex = uploadDTO.getChunkIndex();

        // 检查分片是否已存在（避免重复上传）
        FileChunk existingChunk = fileChunkMapper.selectByMd5AndIndex(fileMd5, chunkIndex);
        if (existingChunk != null) {
            log.info("分片已存在，跳过上传: fileMd5={}, chunkIndex={}", fileMd5, chunkIndex);
            return;
        }

        // 生成分片存储路径: chunks/{fileMd5}/{chunkIndex}
        String chunkPath = String.format("chunks/%s/%d", fileMd5, chunkIndex);

        try (InputStream inputStream = chunkFile.getInputStream()) {
            // 上传分片到 MinIO
            minioUtils.uploadFile(inputStream, chunkPath, "application/octet-stream", chunkFile.getSize());

            // 保存分片记录
            FileChunk chunk = new FileChunk();
            chunk.setFileMd5(fileMd5);
            chunk.setChunkIndex(chunkIndex);
            chunk.setTotalChunks(uploadDTO.getTotalChunks());
            chunk.setChunkSize(uploadDTO.getChunkSize());
            chunk.setChunkPath(chunkPath);
            chunk.setStatus(1); // 已完成
            chunk.setCreateTime(LocalDateTime.now());

            this.save(chunk);

            // 更新 Redis 进度
            String redisKey = RedisKeyConstants.FILE_CHUNK_PROGRESS + fileMd5;
            redisTemplate.opsForSet().add(redisKey, String.valueOf(chunkIndex));
            redisTemplate.expire(redisKey, RedisKeyConstants.CHUNK_PROGRESS_EXPIRE, TimeUnit.SECONDS);

            log.info("分片上传成功: fileMd5={}, chunkIndex={}/{}", fileMd5, chunkIndex, uploadDTO.getTotalChunks());

        } catch (Exception e) {
            log.error("分片上传失败: fileMd5={}, chunkIndex={}", fileMd5, chunkIndex, e);
            throw new BusinessException(ResultCode.CHUNK_UPLOAD_FAIL);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultVO mergeChunks(Long userId, ChunkMergeDTO mergeDTO) {
        String fileMd5 = mergeDTO.getFileMd5();
        int totalChunks = mergeDTO.getTotalChunks();

        // 1. 验证所有分片是否已上传
        int uploadedCount = fileChunkMapper.countUploadedChunks(fileMd5);
        if (uploadedCount < totalChunks) {
            log.warn("分片不完整: fileMd5={}, expected={}, actual={}", fileMd5, totalChunks, uploadedCount);
            throw new BusinessException(ResultCode.CHUNK_MERGE_FAIL, "分片不完整，请继续上传");
        }

        // 2. 获取所有分片路径
        List<FileChunk> chunks = fileChunkMapper.selectByFileMd5(fileMd5);
        List<String> chunkPaths = new ArrayList<>();
        for (FileChunk chunk : chunks) {
            chunkPaths.add(chunk.getChunkPath());
        }

        // 3. 合并分片
        String fileExt = minioUtils.getFileExtension(mergeDTO.getFileName());
        String storagePath = minioUtils.generateStoragePath(userId, fileMd5, fileExt);

        try {
            minioUtils.mergeChunks(chunkPaths, storagePath);
            log.info("分片合并成功: fileMd5={}, storagePath={}", fileMd5, storagePath);
        } catch (Exception e) {
            log.error("分片合并失败: fileMd5={}", fileMd5, e);
            throw new BusinessException(ResultCode.CHUNK_MERGE_FAIL);
        }

        // 4. 保存文件记录
        FileInfo fileInfo = new FileInfo();
        fileInfo.setUserId(userId);
        fileInfo.setFolderId(mergeDTO.getFolderId() != null ? mergeDTO.getFolderId() : 0L);
        fileInfo.setFileName(mergeDTO.getFileName());
        fileInfo.setFileMd5(fileMd5);
        fileInfo.setFileSize(mergeDTO.getTotalSize());
        fileInfo.setFileExt(fileExt);
        fileInfo.setFileType(FileTypeEnum.getByExtension(fileExt).name().toLowerCase());
        fileInfo.setStoragePath(storagePath);
        fileInfo.setStatus(UploadStatusEnum.COMPLETED.getCode());
        fileInfo.setCreateTime(LocalDateTime.now());
        fileInfo.setUpdateTime(LocalDateTime.now());

        fileService.save(fileInfo);

        // 5. 更新用户空间
        userService.updateUsedSpace(userId, mergeDTO.getTotalSize());

        // 6. 清理分片
        cleanupChunks(fileMd5);

        log.info("分片上传完成: userId={}, fileName={}, size={}", userId, mergeDTO.getFileName(), mergeDTO.getTotalSize());

        notificationService.createNotification(userId, "upload", "文件上传成功", "文件 " + mergeDTO.getFileName() + " 已上传", fileInfo.getId());

        // 7. 小文件自动向量化（≤50KB），事务提交后触发
        try {
            if (aiService.isFileVectorizable(fileInfo.getFileExt()) && fileInfo.getFileSize() <= 50 * 1024) {
                final Long uid = userId;
                final Long fid = fileInfo.getId();
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            aiService.vectorizeDocumentAsync(uid, fid);
                        }
                    });
                log.info("已注册事务提交后向量化: fileId={}, ext={}, size={}", fileInfo.getId(), fileInfo.getFileExt(), fileInfo.getFileSize());
            }
        } catch (Exception e) {
            log.warn("自动向量化触发失败（不影响上传）: {}", e.getMessage());
        }

        // 8. 返回结果
        UploadResultVO result = new UploadResultVO();
        result.setFileId(fileInfo.getId());
        result.setFileName(fileInfo.getFileName());
        result.setFileSize(fileInfo.getFileSize());
        result.setFastUpload(false);

        return result;
    }

    @Override
    public List<Integer> getUploadedChunkIndexes(String fileMd5) {
        // 优先从 Redis 获取
        String redisKey = RedisKeyConstants.FILE_CHUNK_PROGRESS + fileMd5;
        var members = redisTemplate.opsForSet().members(redisKey);

        if (members != null && !members.isEmpty()) {
            return members.stream()
                    .map(Integer::parseInt)
                    .sorted()
                    .toList();
        }

        // Redis 中没有则从数据库查询
        return fileChunkMapper.selectUploadedChunkIndexes(fileMd5);
    }

    @Override
    public void cleanupChunks(String fileMd5) {
        try {
            // 1. 获取所有分片路径
            List<FileChunk> chunks = fileChunkMapper.selectByFileMd5(fileMd5);

            // 2. 删除 MinIO 中的分片文件
            for (FileChunk chunk : chunks) {
                try {
                    minioUtils.deleteFile(chunk.getChunkPath());
                } catch (Exception e) {
                    log.warn("删除分片文件失败: {}", chunk.getChunkPath(), e);
                }
            }

            // 3. 删除数据库记录
            fileChunkMapper.deleteByFileMd5(fileMd5);

            // 4. 删除 Redis 进度
            String redisKey = RedisKeyConstants.FILE_CHUNK_PROGRESS + fileMd5;
            redisTemplate.delete(redisKey);

            log.info("分片清理完成: fileMd5={}", fileMd5);

        } catch (Exception e) {
            log.error("分片清理失败: fileMd5={}", fileMd5, e);
        }
    }
}

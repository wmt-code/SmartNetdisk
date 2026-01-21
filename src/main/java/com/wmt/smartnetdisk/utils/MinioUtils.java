package com.wmt.smartnetdisk.utils;

import com.wmt.smartnetdisk.common.exception.BusinessException;
import com.wmt.smartnetdisk.common.result.ResultCode;
import com.wmt.smartnetdisk.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 工具类
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtils {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 检查存储桶是否存在，不存在则创建
     *
     * @param bucketName 存储桶名称
     */
    public void ensureBucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建存储桶成功: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("检查/创建存储桶失败: {}", bucketName, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAIL, "存储服务异常");
        }
    }

    /**
     * 上传文件
     *
     * @param file    文件
     * @param userId  用户ID
     * @param fileMd5 文件MD5
     * @return 存储路径
     */
    public String uploadFile(MultipartFile file, Long userId, String fileMd5) {
        ensureBucketExists(minioConfig.getBucketName());

        String originalFilename = file.getOriginalFilename();
        String ext = getFileExtension(originalFilename);
        String storagePath = generateStoragePath(userId, fileMd5, ext);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storagePath)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            log.info("文件上传成功: {}", storagePath);
            return storagePath;
        } catch (Exception e) {
            log.error("文件上传失败: {}", originalFilename, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAIL);
        }
    }

    /**
     * 上传文件（指定路径）
     *
     * @param inputStream 输入流
     * @param storagePath 存储路径
     * @param contentType 内容类型
     * @param size        文件大小
     */
    public void uploadFile(InputStream inputStream, String storagePath, String contentType, long size) {
        ensureBucketExists(minioConfig.getBucketName());

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storagePath)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());
            log.info("文件上传成功: {}", storagePath);
        } catch (Exception e) {
            log.error("文件上传失败: {}", storagePath, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAIL);
        }
    }

    /**
     * 下载文件
     *
     * @param storagePath 存储路径
     * @return 输入流
     */
    public InputStream downloadFile(String storagePath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storagePath)
                            .build());
        } catch (Exception e) {
            log.error("文件下载失败: {}", storagePath, e);
            throw new BusinessException(ResultCode.FILE_NOT_FOUND, "文件下载失败");
        }
    }

    /**
     * 获取文件预览/下载 URL（带签名，有效期）
     *
     * @param storagePath 存储路径
     * @param expiry      有效期（秒）
     * @return 预签名 URL
     */
    public String getPresignedUrl(String storagePath, int expiry) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storagePath)
                            .method(Method.GET)
                            .expiry(expiry, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            log.error("获取预签名URL失败: {}", storagePath, e);
            throw new BusinessException(ResultCode.FILE_NOT_FOUND, "获取文件链接失败");
        }
    }

    /**
     * 删除文件
     *
     * @param storagePath 存储路径
     */
    public void deleteFile(String storagePath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storagePath)
                            .build());
            log.info("文件删除成功: {}", storagePath);
        } catch (Exception e) {
            log.error("文件删除失败: {}", storagePath, e);
            throw new BusinessException(ResultCode.FILE_DELETE_FAIL);
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param storagePath 存储路径
     * @return 是否存在
     */
    public boolean fileExists(String storagePath) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storagePath)
                            .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成存储路径
     * 格式: {userId}/{year}/{month}/{day}/{md5前2位}/{md5}.{ext}
     *
     * @param userId  用户ID
     * @param fileMd5 文件MD5
     * @param ext     文件扩展名
     * @return 存储路径
     */
    public String generateStoragePath(Long userId, String fileMd5, String ext) {
        LocalDate now = LocalDate.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String md5Prefix = fileMd5.substring(0, 2);
        return String.format("%d/%s/%s/%s.%s", userId, datePath, md5Prefix, fileMd5, ext);
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名（小写）
     */
    public String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 生成唯一文件名
     *
     * @param originalFilename 原始文件名
     * @return 唯一文件名
     */
    public String generateUniqueFilename(String originalFilename) {
        String ext = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + (ext.isEmpty() ? "" : "." + ext);
    }
}

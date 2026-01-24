package com.wmt.smartnetdisk.utils;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.wmt.smartnetdisk.common.exception.BusinessException;
import com.wmt.smartnetdisk.common.result.ResultCode;
import com.wmt.smartnetdisk.config.MinioConfig;

import io.minio.BucketExistsArgs;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
     * 上传文件并同时计算 MD5（一次读取完成两个操作，提升大文件上传速度）
     *
     * @param file   文件
     * @param userId 用户ID
     * @return 上传结果数组：[0] = 存储路径, [1] = MD5值
     */
    public String[] uploadFileWithMd5(MultipartFile file, Long userId) {
        ensureBucketExists(minioConfig.getBucketName());

        String originalFilename = file.getOriginalFilename();
        String ext = getFileExtension(originalFilename);

        try (InputStream rawStream = file.getInputStream();
                DigestInputStream digestStream = new DigestInputStream(rawStream)) {

            // 先用临时路径名上传，上传完成后获取MD5再移动
            String tempPath = String.format("temp/%d/%s.%s", userId, UUID.randomUUID().toString().replace("-", ""),
                    ext);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(tempPath)
                            .stream(digestStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            // 上传完成后获取 MD5
            String fileMd5 = digestStream.getMd5();
            String finalPath = generateStoragePath(userId, fileMd5, ext);

            // 将临时文件复制到最终路径
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(finalPath)
                            .source(CopySource.builder()
                                    .bucket(minioConfig.getBucketName())
                                    .object(tempPath)
                                    .build())
                            .build());

            // 删除临时文件
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(tempPath)
                            .build());

            log.info("文件上传成功(带MD5): path={}, md5={}", finalPath, fileMd5);
            return new String[] { finalPath, fileMd5 };

        } catch (NoSuchAlgorithmException e) {
            log.error("MD5算法不可用", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAIL, "文件校验失败");
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
     * 获取文件下载 URL（带文件名，触发浏览器下载）
     * <p>
     * 通过设置 response-content-disposition 参数强制浏览器下载
     * 使用 RFC 5987 编码处理中文文件名
     * </p>
     *
     * @param storagePath 存储路径
     * @param fileName    原始文件名
     * @param expiry      有效期（秒）
     * @return 预签名下载 URL
     */
    public String getDownloadUrl(String storagePath, String fileName, int expiry) {
        try {
            // 使用 RFC 5987 编码处理文件名（支持中文）
            String encodedFileName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");

            // 为 ASCII 不兼容的浏览器提供安全的 ASCII 备用文件名
            // 替换非 ASCII 字符为下划线，保留扩展名
            String asciiFileName = fileName.replaceAll("[^\\x00-\\x7F]", "_");
            if (asciiFileName.isEmpty() || asciiFileName.equals("_")) {
                // 如果文件名全是非 ASCII，使用默认名称
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    asciiFileName = "download" + fileName.substring(dotIndex);
                } else {
                    asciiFileName = "download";
                }
            }

            // Content-Disposition: attachment; filename="ascii_name";
            // filename*=UTF-8''encoded_name
            String contentDisposition = "attachment; filename=\"" + asciiFileName + "\"; filename*=UTF-8''"
                    + encodedFileName;

            java.util.Map<String, String> extraQueryParams = new java.util.HashMap<>();
            extraQueryParams.put("response-content-disposition", contentDisposition);

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storagePath)
                            .method(Method.GET)
                            .expiry(expiry, TimeUnit.SECONDS)
                            .extraQueryParams(extraQueryParams)
                            .build());
        } catch (Exception e) {
            log.error("获取下载URL失败: {}", storagePath, e);
            throw new BusinessException(ResultCode.FILE_NOT_FOUND, "获取下载链接失败");
        }
    }

    /**
     * 获取文件预览 URL（内联显示）
     * <p>
     * 通过设置 response-content-disposition 为 inline 让浏览器直接显示
     * 同时设置正确的 Content-Type
     * </p>
     *
     * @param storagePath 存储路径
     * @param mimeType    MIME 类型
     * @param expiry      有效期（秒）
     * @return 预签名预览 URL
     */
    public String getPreviewUrl(String storagePath, String mimeType, int expiry) {
        try {
            java.util.Map<String, String> extraQueryParams = new java.util.HashMap<>();
            extraQueryParams.put("response-content-disposition", "inline");
            if (mimeType != null && !mimeType.isBlank()) {
                // 为文本类型添加 UTF-8 编码，解决中文乱码问题
                String contentType = mimeType;
                if (mimeType.startsWith("text/") && !mimeType.contains("charset")) {
                    contentType = mimeType + "; charset=utf-8";
                }
                extraQueryParams.put("response-content-type", contentType);
            }

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storagePath)
                            .method(Method.GET)
                            .expiry(expiry, TimeUnit.SECONDS)
                            .extraQueryParams(extraQueryParams)
                            .build());
        } catch (Exception e) {
            log.error("获取预览URL失败: {}", storagePath, e);
            throw new BusinessException(ResultCode.FILE_NOT_FOUND, "获取预览链接失败");
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

    /**
     * 上传头像
     *
     * @param file   头像文件
     * @param userId 用户ID
     * @return 头像存储路径
     */
    public String uploadAvatar(MultipartFile file, Long userId) {
        String avatarBucket = "avatars";
        ensureBucketExists(avatarBucket);

        String originalFilename = file.getOriginalFilename();
        String ext = getFileExtension(originalFilename);
        // 生成头像存储路径: avatars/{userId}/{uuid}.{ext}
        String storagePath = String.format("%d/%s.%s", userId, UUID.randomUUID().toString().replace("-", ""), ext);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(avatarBucket)
                            .object(storagePath)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            log.info("头像上传成功: userId={}, path={}", userId, storagePath);
            return storagePath;
        } catch (Exception e) {
            log.error("头像上传失败: userId={}", userId, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAIL, "头像上传失败");
        }
    }

    /**
     * 获取头像预签名 URL
     *
     * @param storagePath 存储路径
     * @param expiry      有效期（秒）
     * @return 预签名 URL
     */
    public String getAvatarPresignedUrl(String storagePath, int expiry) {
        String avatarBucket = "avatars";
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(avatarBucket)
                            .object(storagePath)
                            .method(Method.GET)
                            .expiry(expiry, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            log.error("获取头像URL失败: {}", storagePath, e);
            throw new BusinessException(ResultCode.FILE_NOT_FOUND, "获取头像链接失败");
        }
    }

    /**
     * 删除旧头像
     *
     * @param storagePath 存储路径
     */
    public void deleteAvatar(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        String avatarBucket = "avatars";
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(avatarBucket)
                            .object(storagePath)
                            .build());
            log.info("旧头像删除成功: {}", storagePath);
        } catch (Exception e) {
            log.warn("旧头像删除失败(可忽略): {}", storagePath, e);
            // 删除失败不抛异常，避免影响上传流程
        }
    }

    /**
     * 合并分片文件
     *
     * @param chunkPaths 分片路径列表（按顺序）
     * @param targetPath 目标文件路径
     */
    public void mergeChunks(java.util.List<String> chunkPaths, String targetPath) {
        ensureBucketExists(minioConfig.getBucketName());

        try {
            // 构建合并源列表
            java.util.List<ComposeSource> sources = chunkPaths.stream()
                    .map(path -> ComposeSource.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(path)
                            .build())
                    .toList();

            // 执行合并
            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(targetPath)
                            .sources(sources)
                            .build());

            log.info("分片合并成功: targetPath={}, chunks={}", targetPath, chunkPaths.size());

        } catch (Exception e) {
            log.error("分片合并失败: targetPath={}", targetPath, e);
            throw new BusinessException(ResultCode.CHUNK_MERGE_FAIL, "分片合并失败");
        }
    }

    /**
     * 上传文件内容（字节数组）
     *
     * @param content     文件内容
     * @param storagePath 存储路径
     * @param contentType 内容类型
     */
    public void uploadContent(byte[] content, String storagePath, String contentType) {
        ensureBucketExists(minioConfig.getBucketName());

        try (java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(content)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storagePath)
                            .stream(inputStream, content.length, -1)
                            .contentType(contentType)
                            .build());
            log.info("文件内容上传成功: path={}, size={}", storagePath, content.length);
        } catch (Exception e) {
            log.error("文件内容上传失败: {}", storagePath, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAIL, "保存文件失败");
        }
    }
}

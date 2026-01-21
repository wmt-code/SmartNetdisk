package com.wmt.smartnetdisk.common.constant;

import java.util.Set;

/**
 * 文件相关常量定义
 *
 * @author wmt
 * @since 1.0.0
 */
public final class FileConstants {

    private FileConstants() {
        throw new IllegalStateException("常量类不允许实例化");
    }

    // ==================== 分片上传配置 ====================
    /**
     * 默认分片大小：5MB
     */
    public static final long DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024L;

    /**
     * 最小分片大小：1MB
     */
    public static final long MIN_CHUNK_SIZE = 1024 * 1024L;

    /**
     * 最大分片大小：10MB
     */
    public static final long MAX_CHUNK_SIZE = 10 * 1024 * 1024L;

    // ==================== 文件大小限制 ====================
    /**
     * 普通上传最大文件大小：100MB
     */
    public static final long MAX_NORMAL_UPLOAD_SIZE = 100 * 1024 * 1024L;

    /**
     * 分片上传最大文件大小：10GB
     */
    public static final long MAX_CHUNK_UPLOAD_SIZE = 10L * 1024 * 1024 * 1024;

    /**
     * 头像最大大小：2MB
     */
    public static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024L;

    // ==================== 文件类型分类 ====================
    /**
     * 文件类型 - 图片
     */
    public static final String FILE_TYPE_IMAGE = "image";

    /**
     * 文件类型 - 视频
     */
    public static final String FILE_TYPE_VIDEO = "video";

    /**
     * 文件类型 - 音频
     */
    public static final String FILE_TYPE_AUDIO = "audio";

    /**
     * 文件类型 - 文档
     */
    public static final String FILE_TYPE_DOCUMENT = "document";

    /**
     * 文件类型 - 其他
     */
    public static final String FILE_TYPE_OTHER = "other";

    // ==================== 文件扩展名白名单 ====================
    /**
     * 图片扩展名
     */
    public static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico");

    /**
     * 视频扩展名
     */
    public static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v");

    /**
     * 音频扩展名
     */
    public static final Set<String> AUDIO_EXTENSIONS = Set.of(
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a");

    /**
     * 文档扩展名（可向量化）
     */
    public static final Set<String> DOCUMENT_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "md", "rtf", "csv", "json", "xml");

    /**
     * 压缩文件扩展名
     */
    public static final Set<String> ARCHIVE_EXTENSIONS = Set.of(
            "zip", "rar", "7z", "tar", "gz", "bz2");

    /**
     * 可预览的文档扩展名
     */
    public static final Set<String> PREVIEWABLE_DOC_EXTENSIONS = Set.of(
            "pdf", "txt", "md");

    /**
     * 可向量化的文档扩展名
     */
    public static final Set<String> VECTORIZABLE_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "txt", "md", "rtf");

    // ==================== 存储路径 ====================
    /**
     * MinIO 存储桶 - 文件
     */
    public static final String BUCKET_FILES = "files";

    /**
     * MinIO 存储桶 - 分片临时目录
     */
    public static final String BUCKET_CHUNKS = "chunks";

    /**
     * MinIO 存储桶 - 头像
     */
    public static final String BUCKET_AVATARS = "avatars";

    /**
     * MinIO 存储桶 - 缩略图
     */
    public static final String BUCKET_THUMBNAILS = "thumbnails";

    // ==================== MD5 相关 ====================
    /**
     * MD5 长度
     */
    public static final int MD5_LENGTH = 32;

    // ==================== 文件名长度限制 ====================
    /**
     * 文件名最大长度
     */
    public static final int MAX_FILE_NAME_LENGTH = 200;

    /**
     * 文件夹名最大长度
     */
    public static final int MAX_FOLDER_NAME_LENGTH = 100;
}

package com.wmt.smartnetdisk.controller;

import com.wmt.smartnetdisk.common.result.PageResult;
import com.wmt.smartnetdisk.common.result.Result;
import com.wmt.smartnetdisk.dto.request.ChunkMergeDTO;
import com.wmt.smartnetdisk.dto.request.ChunkUploadDTO;
import com.wmt.smartnetdisk.dto.request.CopyDTO;
import com.wmt.smartnetdisk.dto.request.FastUploadDTO;
import com.wmt.smartnetdisk.dto.request.FileListDTO;
import com.wmt.smartnetdisk.dto.request.MoveDTO;
import com.wmt.smartnetdisk.dto.request.RenameDTO;
import com.wmt.smartnetdisk.dto.request.SaveContentDTO;
import com.wmt.smartnetdisk.service.IAuthService;
import com.wmt.smartnetdisk.service.IFileChunkService;
import com.wmt.smartnetdisk.service.IFileService;
import com.wmt.smartnetdisk.vo.ChunkCheckResultVO;
import com.wmt.smartnetdisk.vo.FileVO;
import com.wmt.smartnetdisk.vo.UploadResultVO;
import com.wmt.smartnetdisk.entity.FileInfo;
import com.wmt.smartnetdisk.utils.MinioUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件控制器
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final IFileService fileService;
    private final IFileChunkService fileChunkService;
    private final IAuthService authService;
    private final MinioUtils minioUtils;

    // ==================== 文件上传相关 ====================

    /**
     * 秒传检测
     * 前端先计算文件 MD5，调用此接口检测是否可以秒传
     */
    @PostMapping("/check")
    public Result<UploadResultVO> checkFastUpload(@RequestBody FastUploadDTO fastUploadDTO) {
        Long userId = authService.getCurrentUserId();
        UploadResultVO result = fileService.checkFastUpload(userId, fastUploadDTO);
        if (result != null) {
            return Result.success("秒传成功", result);
        }
        // 返回 null 表示需要上传
        return Result.success(null);
    }

    /**
     * 普通文件上传
     */
    @PostMapping("/upload")
    public Result<UploadResultVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", defaultValue = "0") Long folderId) {
        Long userId = authService.getCurrentUserId();
        UploadResultVO result = fileService.uploadFile(userId, file, folderId);
        return Result.success("上传成功", result);
    }

    // ==================== 分片上传相关 ====================

    /**
     * 分片上传检测（秒传 + 断点续传）
     * 前端先计算文件 MD5，调用此接口检测是否可以秒传或获取已上传分片列表
     */
    @PostMapping("/chunk/check")
    public Result<ChunkCheckResultVO> checkChunks(@Valid @RequestBody FastUploadDTO fastUploadDTO) {
        Long userId = authService.getCurrentUserId();
        ChunkCheckResultVO result = fileChunkService.checkChunks(userId, fastUploadDTO);
        return Result.success(result);
    }

    /**
     * 上传单个分片
     */
    @PostMapping("/chunk")
    public Result<Void> uploadChunk(
            ChunkUploadDTO chunkUploadDTO,
            @RequestParam("file") MultipartFile chunkFile) {
        Long userId = authService.getCurrentUserId();
        fileChunkService.uploadChunk(userId, chunkUploadDTO, chunkFile);
        return Result.success("分片上传成功", null);
    }

    /**
     * 合并分片
     */
    @PostMapping("/merge")
    public Result<UploadResultVO> mergeChunks(@Valid @RequestBody ChunkMergeDTO mergeDTO) {
        Long userId = authService.getCurrentUserId();
        UploadResultVO result = fileChunkService.mergeChunks(userId, mergeDTO);
        return Result.success("合并成功", result);
    }

    /**
     * 获取文件下载链接
     */
    @GetMapping("/{id}/download")
    public Result<Map<String, String>> getDownloadUrl(@PathVariable("id") Long fileId) {
        Long userId = authService.getCurrentUserId();
        // 下载链接有效期：1小时
        String url = fileService.getDownloadUrl(userId, fileId, 3600);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success(data);
    }

    /**
     * 直接下载文件（流式传输）
     * 通过后端流式传输文件，确保文件名正确
     */
    @GetMapping("/{id}/download/stream")
    public void downloadFileStream(@PathVariable("id") Long fileId, HttpServletResponse response) {
        Long userId = authService.getCurrentUserId();
        FileInfo fileInfo = fileService.getFileWithPermission(userId, fileId);

        try (InputStream inputStream = minioUtils.downloadFile(fileInfo.getStoragePath());
                OutputStream outputStream = response.getOutputStream()) {

            // 设置响应头
            String fileName = fileInfo.getFileName();
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileInfo.getFileSize()));

            // 流式传输
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();

            log.info("文件下载成功: userId={}, fileId={}, fileName={}", userId, fileId, fileName);
        } catch (Exception e) {
            log.error("文件下载失败: fileId={}", fileId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 流式传输文件（支持 Range 请求，用于视频/音频播放）
     * 支持分段加载，浏览器可以拖动进度条
     */
    @GetMapping("/{id}/stream")
    public void streamFile(
            @PathVariable("id") Long fileId,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpServletResponse response) {

        Long userId = authService.getCurrentUserId();
        FileInfo fileInfo = fileService.getFileWithPermission(userId, fileId);

        long fileSize = fileInfo.getFileSize();
        long start = 0;
        long end = fileSize - 1;

        try {
            // 解析 Range 请求头 (格式: bytes=start-end)
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
                // 确保end不超过文件大小
                end = Math.min(end, fileSize - 1);
            }

            long contentLength = end - start + 1;

            // 设置响应头
            response.setStatus(
                    rangeHeader != null ? HttpServletResponse.SC_PARTIAL_CONTENT : HttpServletResponse.SC_OK);

            // 根据文件扩展名或已存储的 MIME 类型设置 Content-Type
            String mimeType = fileInfo.getMimeType();
            if (mimeType == null || mimeType.isBlank() || mimeType.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
                // 根据扩展名推断 MIME 类型
                mimeType = inferMimeType(fileInfo.getFileExt());
            }
            response.setContentType(mimeType);

            // CORS 支持（跨域 Range 请求需要）
            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                    "Content-Range, Accept-Ranges, Content-Length, Content-Type");
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            response.setHeader(HttpHeaders.CONTENT_RANGE, String.format("bytes %d-%d/%d", start, end, fileSize));
            response.setContentLengthLong(contentLength);

            // 使用 MinIO 的 Range 请求直接获取指定范围的数据（高效！）
            try (InputStream inputStream = minioUtils.downloadFileRange(fileInfo.getStoragePath(), start,
                    contentLength);
                    OutputStream outputStream = response.getOutputStream()) {

                // 直接读取并输出数据，不需要 skip()
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

                log.debug("流式传输成功: userId={}, fileId={}, range={}-{}/{}",
                        userId, fileId, start, end, fileSize);
            }
        } catch (org.apache.catalina.connector.ClientAbortException e) {
            // 客户端主动中止连接（浏览器取消请求），这是正常行为，不记录错误日志
            log.debug("客户端中止连接: fileId={}, range={} (正常行为)", fileId, rangeHeader);
        } catch (java.io.IOException e) {
            // 检查是否是客户端中止导致的IOException
            String message = e.getMessage();
            if (message != null &&
                    (message.contains("你的主机中的软件中止了一个已建立的连接") ||
                            message.contains("Connection reset by peer") ||
                            message.contains("Broken pipe") ||
                            message.contains("远程主机强迫关闭了一个现有的连接"))) {
                log.debug("客户端中止连接: fileId={} (正常行为)", fileId);
            } else {
                log.error("流式传输IO失败: fileId={}, range={}", fileId, rangeHeader, e);
            }
        } catch (Exception e) {
            log.error("流式传输失败: fileId={}, range={}", fileId, rangeHeader, e);
        }
    }

    /**
     * 获取文件预览链接
     * 所有文件类型都使用 kkFileView 预览
     */
    @GetMapping("/{id}/preview")
    public Result<Map<String, String>> getPreviewUrl(@PathVariable("id") Long fileId) {
        Long userId = authService.getCurrentUserId();
        // 所有文件都使用 kkFileView 预览（2小时有效期）
        String url = fileService.getKkFileViewPreviewUrl(userId, fileId, 7200);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success(data);
    }

    /**
     * 获取文件文本内容（用于在线编辑）
     * 支持的文件类型：txt, md, json, xml, html, css, js, ts, java, py, go, c, cpp, h, yml,
     * yaml, sh, bat, sql 等
     * 支持分块加载，默认每次加载 1MB
     * 
     * @param fileId 文件ID
     * @param offset 偏移量（字节），默认为 0
     * @param limit  限制大小（字节），默认为 1MB，最大 5MB
     */
    @GetMapping("/{id}/content")
    public Result<Map<String, Object>> getFileContent(
            @PathVariable("id") Long fileId,
            @RequestParam(value = "offset", required = false) Long offset,
            @RequestParam(value = "limit", required = false) Long limit) {
        Long userId = authService.getCurrentUserId();
        Map<String, Object> data = fileService.getFileContent(userId, fileId, offset, limit);
        return Result.success(data);
    }

    /**
     * 保存文件文本内容（在线编辑后保存）
     */
    @PutMapping("/{id}/content")
    public Result<Void> saveFileContent(@PathVariable("id") Long fileId,
            @Valid @RequestBody SaveContentDTO saveContentDTO) {
        Long userId = authService.getCurrentUserId();
        fileService.saveFileContent(userId, fileId, saveContentDTO.getContent());
        return Result.success("保存成功", null);
    }

    // ==================== 文件管理相关 ====================

    /**
     * 获取文件列表
     */
    @GetMapping("/list")
    public Result<PageResult<FileVO>> listFiles(FileListDTO listDTO) {
        Long userId = authService.getCurrentUserId();
        PageResult<FileVO> result = fileService.listFiles(userId, listDTO);
        return Result.success(result);
    }

    /**
     * 获取文件详情
     */
    @GetMapping("/{id}")
    public Result<FileVO> getFileDetail(@PathVariable("id") Long fileId) {
        Long userId = authService.getCurrentUserId();
        FileVO fileVO = fileService.getFileDetail(userId, fileId);
        return Result.success(fileVO);
    }

    /**
     * 重命名文件
     */
    @PutMapping("/{id}")
    public Result<Void> renameFile(@PathVariable("id") Long fileId, @Valid @RequestBody RenameDTO renameDTO) {
        Long userId = authService.getCurrentUserId();
        fileService.renameFile(userId, fileId, renameDTO.getName());
        return Result.success("重命名成功", null);
    }

    /**
     * 移动文件
     */
    @PutMapping("/{id}/move")
    public Result<Void> moveFile(@PathVariable("id") Long fileId, @Valid @RequestBody MoveDTO moveDTO) {
        Long userId = authService.getCurrentUserId();
        fileService.moveFile(userId, fileId, moveDTO.getTargetFolderId());
        return Result.success("移动成功", null);
    }

    /**
     * 复制文件
     */
    @PostMapping("/{id}/copy")
    public Result<FileVO> copyFile(@PathVariable("id") Long fileId, @Valid @RequestBody CopyDTO copyDTO) {
        Long userId = authService.getCurrentUserId();
        FileVO newFile = fileService.copyFile(userId, fileId, copyDTO.getTargetFolderId());
        return Result.success("复制成功", newFile);
    }

    /**
     * 删除文件（移入回收站）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable("id") Long fileId) {
        Long userId = authService.getCurrentUserId();
        fileService.deleteFile(userId, fileId);
        return Result.success("删除成功", null);
    }

    /**
     * 彻底删除文件
     */
    @DeleteMapping("/{id}/permanent")
    public Result<Void> permanentDeleteFile(@PathVariable("id") Long fileId) {
        Long userId = authService.getCurrentUserId();
        fileService.permanentDeleteFile(userId, fileId);
        return Result.success("彻底删除成功", null);
    }

    /**
     * 恢复文件
     */
    @PostMapping("/{id}/restore")
    public Result<Void> restoreFile(@PathVariable("id") Long fileId) {
        Long userId = authService.getCurrentUserId();
        fileService.restoreFile(userId, fileId);
        return Result.success("恢复成功", null);
    }

    /**
     * 回收站列表
     */
    @GetMapping("/recycle")
    public Result<PageResult<FileVO>> listRecycledFiles(FileListDTO listDTO) {
        Long userId = authService.getCurrentUserId();
        PageResult<FileVO> result = fileService.listRecycledFiles(userId, listDTO);
        return Result.success(result);
    }

    /**
     * 批量删除
     */
    @PostMapping("/batch/delete")
    public Result<Void> batchDeleteFiles(@RequestBody List<Long> fileIds) {
        Long userId = authService.getCurrentUserId();
        fileService.batchDeleteFiles(userId, fileIds);
        return Result.success("批量删除成功", null);
    }

    /**
     * 批量移动
     */
    @PostMapping("/batch/move")
    public Result<Void> batchMoveFiles(@Valid @RequestBody MoveDTO moveDTO) {
        Long userId = authService.getCurrentUserId();
        fileService.batchMoveFiles(userId, moveDTO.getFileIds(), moveDTO.getTargetFolderId());
        return Result.success("批量移动成功", null);
    }

    /**
     * 批量复制
     */
    @PostMapping("/batch/copy")
    public Result<List<FileVO>> batchCopyFiles(@Valid @RequestBody CopyDTO copyDTO) {
        Long userId = authService.getCurrentUserId();
        List<FileVO> newFiles = fileService.batchCopyFiles(userId, copyDTO.getFileIds(), copyDTO.getTargetFolderId());
        return Result.success("批量复制成功", newFiles);
    }

    /**
     * 根据文件扩展名推断 MIME 类型
     * 
     * @param fileExt 文件扩展名（不含点号）
     * @return MIME 类型
     */
    private String inferMimeType(String fileExt) {
        if (fileExt == null || fileExt.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        String ext = fileExt.toLowerCase();
        return switch (ext) {
            // 视频格式
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "ogg" -> "video/ogg";
            case "mov" -> "video/quicktime";
            case "avi" -> "video/x-msvideo";
            case "mkv" -> "video/x-matroska";
            case "m4v" -> "video/x-m4v";
            case "flv" -> "video/x-flv";
            case "wmv" -> "video/x-ms-wmv";
            case "3gp" -> "video/3gpp";

            // 音频格式
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "aac" -> "audio/aac";
            case "flac" -> "audio/flac";
            case "m4a" -> "audio/mp4";
            case "wma" -> "audio/x-ms-wma";
            case "ape" -> "audio/ape";

            // 图片格式
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "bmp" -> "image/bmp";
            case "ico" -> "image/x-icon";

            // 文档格式
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";

            // 文本格式
            case "txt" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "md" -> "text/markdown";

            // 默认
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }
}

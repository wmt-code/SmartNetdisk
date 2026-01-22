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
import com.wmt.smartnetdisk.service.IAuthService;
import com.wmt.smartnetdisk.service.IFileChunkService;
import com.wmt.smartnetdisk.service.IFileService;
import com.wmt.smartnetdisk.vo.ChunkCheckResultVO;
import com.wmt.smartnetdisk.vo.FileVO;
import com.wmt.smartnetdisk.vo.UploadResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        String url = fileService.getFileUrl(userId, fileId, 3600);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success(data);
    }

    /**
     * 获取文件预览链接
     */
    @GetMapping("/{id}/preview")
    public Result<Map<String, String>> getPreviewUrl(@PathVariable("id") Long fileId) {
        Long userId = authService.getCurrentUserId();
        // 预览链接有效期：10分钟
        String url = fileService.getFileUrl(userId, fileId, 600);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success(data);
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
}

package com.wmt.smartnetdisk.controller;

import com.wmt.smartnetdisk.common.result.Result;
import com.wmt.smartnetdisk.dto.request.CreateFolderDTO;
import com.wmt.smartnetdisk.dto.request.RenameDTO;
import com.wmt.smartnetdisk.service.IAuthService;
import com.wmt.smartnetdisk.service.IFolderService;
import com.wmt.smartnetdisk.vo.FolderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文件夹控制器
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/folder")
@RequiredArgsConstructor
public class FolderController {

    private final IFolderService folderService;
    private final IAuthService authService;

    /**
     * 创建文件夹
     */
    @PostMapping
    public Result<FolderVO> createFolder(@Valid @RequestBody CreateFolderDTO createDTO) {
        Long userId = authService.getCurrentUserId();
        FolderVO folderVO = folderService.createFolder(userId, createDTO);
        return Result.success("创建成功", folderVO);
    }

    /**
     * 获取文件夹详情
     */
    @GetMapping("/{id}")
    public Result<FolderVO> getFolderDetail(@PathVariable("id") Long folderId) {
        Long userId = authService.getCurrentUserId();
        FolderVO folderVO = folderService.getFolderDetail(userId, folderId);
        return Result.success(folderVO);
    }

    /**
     * 重命名文件夹
     */
    @PutMapping("/{id}")
    public Result<Void> renameFolder(@PathVariable("id") Long folderId, @Valid @RequestBody RenameDTO renameDTO) {
        Long userId = authService.getCurrentUserId();
        folderService.renameFolder(userId, folderId, renameDTO.getName());
        return Result.success("重命名成功", null);
    }

    /**
     * 删除文件夹
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteFolder(@PathVariable("id") Long folderId) {
        Long userId = authService.getCurrentUserId();
        folderService.deleteFolder(userId, folderId);
        return Result.success("删除成功", null);
    }

    /**
     * 恢复文件夹
     */
    @PostMapping("/{id}/restore")
    public Result<Void> restoreFolder(@PathVariable("id") Long folderId) {
        Long userId = authService.getCurrentUserId();
        folderService.restoreFolder(userId, folderId);
        return Result.success("恢复成功", null);
    }

    /**
     * 彻底删除文件夹
     */
    @DeleteMapping("/{id}/permanent")
    public Result<Void> permanentDeleteFolder(@PathVariable("id") Long folderId) {
        Long userId = authService.getCurrentUserId();
        folderService.permanentDeleteFolder(userId, folderId);
        return Result.success("彻底删除成功", null);
    }

    /**
     * 获取文件夹树
     */
    @GetMapping("/tree")
    public Result<List<FolderVO>> getFolderTree() {
        Long userId = authService.getCurrentUserId();
        List<FolderVO> tree = folderService.getFolderTree(userId);
        return Result.success(tree);
    }

    /**
     * 获取子文件夹列表
     */
    @GetMapping("/children")
    public Result<List<FolderVO>> getChildren(@RequestParam(defaultValue = "0") Long parentId) {
        Long userId = authService.getCurrentUserId();
        List<FolderVO> children = folderService.getChildren(userId, parentId);
        return Result.success(children);
    }
}

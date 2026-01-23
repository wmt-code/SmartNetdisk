package com.wmt.smartnetdisk.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wmt.smartnetdisk.common.result.Result;
import com.wmt.smartnetdisk.service.IShareService;
import com.wmt.smartnetdisk.vo.ShareVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 公开分享控制器（无需登录）
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/s")
@RequiredArgsConstructor
public class PublicShareController {

    private final IShareService shareService;

    /**
     * 访问分享（获取分享信息）
     */
    @GetMapping("/{code}")
    public Result<ShareVO> getShare(@PathVariable("code") String shareCode) {
        ShareVO shareVO = shareService.getShareByCode(shareCode);
        return Result.success(shareVO);
    }

    /**
     * 验证提取码
     */
    @PostMapping("/{code}/verify")
    public Result<ShareVO> verifyPassword(
            @PathVariable("code") String shareCode,
            @RequestBody Map<String, String> body) {
        String password = body.get("password");
        ShareVO shareVO = shareService.verifyPasswordAndGetInfo(shareCode, password);
        return Result.success("验证成功", shareVO);
    }

    /**
     * 下载分享文件
     */
    @GetMapping("/{code}/download")
    public Result<Map<String, String>> downloadShare(
            @PathVariable("code") String shareCode,
            @RequestParam(required = false) String password) {
        String url = shareService.getDownloadUrl(shareCode, password);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success(data);
    }

    /**
     * 流式下载分享文件（直接传输，确保文件名正确）
     */
    @GetMapping("/{code}/download/stream")
    public void downloadShareStream(
            @PathVariable("code") String shareCode,
            @RequestParam(required = false) String password,
            jakarta.servlet.http.HttpServletResponse response) {
        shareService.downloadShareStream(shareCode, password, response);
    }

    /**
     * 获取分享项列表（批量/目录分享时使用）
     */
    @GetMapping("/{code}/items")
    public Result<List<ShareVO.ShareItemVO>> getShareItems(
            @PathVariable("code") String shareCode,
            @RequestParam(required = false) String password) {
        List<ShareVO.ShareItemVO> items = shareService.getShareItems(shareCode, password);
        return Result.success(items);
    }

    /**
     * 浏览分享文件夹内容（进入子文件夹）
     */
    @GetMapping("/{code}/browse")
    public Result<List<ShareVO.ShareItemVO>> browseFolderContents(
            @PathVariable("code") String shareCode,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "0") Long folderId) {
        List<ShareVO.ShareItemVO> items = shareService.browseFolderContents(shareCode, password, folderId);
        return Result.success(items);
    }

    /**
     * 下载分享中的指定文件
     */
    @GetMapping("/{code}/download/{fileId}")
    public Result<Map<String, String>> downloadFileById(
            @PathVariable("code") String shareCode,
            @PathVariable("fileId") Long fileId,
            @RequestParam(required = false) String password) {
        String url = shareService.getFileDownloadUrl(shareCode, password, fileId);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success(data);
    }

    /**
     * 流式下载分享中的指定文件
     */
    @GetMapping("/{code}/download/{fileId}/stream")
    public void downloadFileByIdStream(
            @PathVariable("code") String shareCode,
            @PathVariable("fileId") Long fileId,
            @RequestParam(required = false) String password,
            jakarta.servlet.http.HttpServletResponse response) {
        shareService.downloadFileStream(shareCode, password, fileId, response);
    }
}

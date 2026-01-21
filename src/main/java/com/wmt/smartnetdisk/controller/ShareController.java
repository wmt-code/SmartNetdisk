package com.wmt.smartnetdisk.controller;

import com.wmt.smartnetdisk.common.result.PageRequest;
import com.wmt.smartnetdisk.common.result.PageResult;
import com.wmt.smartnetdisk.common.result.Result;
import com.wmt.smartnetdisk.dto.request.CreateShareDTO;
import com.wmt.smartnetdisk.service.IAuthService;
import com.wmt.smartnetdisk.service.IShareService;
import com.wmt.smartnetdisk.vo.ShareVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 分享控制器（需登录）
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {

    private final IShareService shareService;
    private final IAuthService authService;

    /**
     * 创建分享
     */
    @PostMapping
    public Result<ShareVO> createShare(@Valid @RequestBody CreateShareDTO createDTO) {
        Long userId = authService.getCurrentUserId();
        ShareVO shareVO = shareService.createShare(userId, createDTO);
        return Result.success("分享创建成功", shareVO);
    }

    /**
     * 我的分享列表
     */
    @GetMapping("/list")
    public Result<PageResult<ShareVO>> listMyShares(PageRequest pageRequest) {
        Long userId = authService.getCurrentUserId();
        PageResult<ShareVO> result = shareService.listMyShares(userId, pageRequest);
        return Result.success(result);
    }

    /**
     * 取消分享
     */
    @DeleteMapping("/{id}")
    public Result<Void> cancelShare(@PathVariable("id") Long shareId) {
        Long userId = authService.getCurrentUserId();
        shareService.cancelShare(userId, shareId);
        return Result.success("取消分享成功", null);
    }
}

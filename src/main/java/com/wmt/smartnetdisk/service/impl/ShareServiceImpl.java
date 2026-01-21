package com.wmt.smartnetdisk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmt.smartnetdisk.common.enums.ShareStatusEnum;
import com.wmt.smartnetdisk.common.exception.BusinessException;
import com.wmt.smartnetdisk.common.result.PageRequest;
import com.wmt.smartnetdisk.common.result.PageResult;
import com.wmt.smartnetdisk.common.result.ResultCode;
import com.wmt.smartnetdisk.dto.request.CreateShareDTO;
import com.wmt.smartnetdisk.entity.FileInfo;
import com.wmt.smartnetdisk.entity.Share;
import com.wmt.smartnetdisk.mapper.ShareMapper;
import com.wmt.smartnetdisk.service.IFileService;
import com.wmt.smartnetdisk.service.IShareService;
import com.wmt.smartnetdisk.utils.MinioUtils;
import com.wmt.smartnetdisk.vo.ShareVO;
import com.wmt.smartnetdisk.vo.SpaceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 分享服务实现类
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareServiceImpl extends ServiceImpl<ShareMapper, Share> implements IShareService {

    private final IFileService fileService;
    private final MinioUtils minioUtils;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareVO createShare(Long userId, CreateShareDTO createDTO) {
        // 验证文件存在且属于当前用户
        FileInfo fileInfo = fileService.getFileWithPermission(userId, createDTO.getFileId());

        // 生成分享码（6位）
        String shareCode = generateShareCode();

        // 生成或使用提供的提取码（4位）
        String password = createDTO.getPassword();
        if (password == null || password.isBlank()) {
            password = generatePassword();
        }

        // 计算过期时间
        LocalDateTime expireTime = null;
        if (createDTO.getExpireDays() != null && createDTO.getExpireDays() > 0) {
            expireTime = LocalDateTime.now().plusDays(createDTO.getExpireDays());
        }

        // 创建分享记录
        Share share = new Share();
        share.setUserId(userId);
        share.setFileId(createDTO.getFileId());
        share.setShareCode(shareCode);
        share.setPassword(password);
        share.setExpireTime(expireTime);
        share.setViewCount(0);
        share.setDownloadCount(0);
        share.setMaxViewCount(createDTO.getMaxViewCount() != null ? createDTO.getMaxViewCount() : 0);
        share.setStatus(ShareStatusEnum.ACTIVE.getCode());

        save(share);
        log.info("创建分享成功: userId={}, fileId={}, shareCode={}", userId, createDTO.getFileId(), shareCode);

        // 构建返回对象（包含提取码）
        ShareVO vo = toVO(share, true);
        vo.setFileName(fileInfo.getFileName());
        vo.setFileSize(fileInfo.getFileSize());
        vo.setFileSizeStr(SpaceVO.formatSize(fileInfo.getFileSize()));
        return vo;
    }

    @Override
    public PageResult<ShareVO> listMyShares(Long userId, PageRequest pageRequest) {
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Share::getUserId, userId)
                .orderByDesc(Share::getCreateTime);

        Page<Share> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        Page<Share> result = baseMapper.selectPage(page, wrapper);

        List<ShareVO> voList = result.getRecords().stream()
                .map(share -> {
                    ShareVO vo = toVO(share, false);
                    // 查询文件信息
                    FileInfo fileInfo = fileService.getById(share.getFileId());
                    if (fileInfo != null) {
                        vo.setFileName(fileInfo.getFileName());
                        vo.setFileSize(fileInfo.getFileSize());
                        vo.setFileSizeStr(SpaceVO.formatSize(fileInfo.getFileSize()));
                    }
                    return vo;
                })
                .toList();

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelShare(Long userId, Long shareId) {
        Share share = getById(shareId);
        if (share == null || !share.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }
        share.setStatus(ShareStatusEnum.CANCELLED.getCode());
        updateById(share);
        log.info("取消分享成功: shareId={}", shareId);
    }

    @Override
    public ShareVO getShareByCode(String shareCode) {
        Share share = baseMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }

        // 检查分享状态
        checkShareValid(share);

        // 增加访问次数
        baseMapper.incrementViewCount(share.getId());

        // 查询文件信息
        FileInfo fileInfo = fileService.getById(share.getFileId());
        ShareVO vo = toVO(share, false);
        if (fileInfo != null) {
            vo.setFileName(fileInfo.getFileName());
            vo.setFileSize(fileInfo.getFileSize());
            vo.setFileSizeStr(SpaceVO.formatSize(fileInfo.getFileSize()));
        }
        return vo;
    }

    @Override
    public String verifyPassword(String shareCode, String password) {
        Share share = baseMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException(ResultCode.SHARE_NOT_FOUND);
        }

        checkShareValid(share);

        // 验证提取码
        if (!share.getPassword().equals(password)) {
            throw new BusinessException(ResultCode.SHARE_PASSWORD_ERROR);
        }

        // 返回文件下载 URL
        FileInfo fileInfo = fileService.getById(share.getFileId());
        if (fileInfo == null) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }

        return minioUtils.getPresignedUrl(fileInfo.getStoragePath(), 3600);
    }

    @Override
    public String getDownloadUrl(String shareCode, String password) {
        // 验证密码并获取下载链接
        String url = verifyPassword(shareCode, password);

        // 增加下载次数
        Share share = baseMapper.selectByShareCode(shareCode);
        if (share != null) {
            baseMapper.incrementDownloadCount(share.getId());
        }

        return url;
    }

    @Override
    public ShareVO toVO(Share share, boolean includePassword) {
        if (share == null) {
            return null;
        }
        ShareVO vo = new ShareVO();
        vo.setId(share.getId());
        vo.setFileId(share.getFileId());
        vo.setShareCode(share.getShareCode());
        vo.setShareUrl(contextPath + "/s/" + share.getShareCode());
        vo.setHasPassword(share.getPassword() != null && !share.getPassword().isEmpty());
        if (includePassword) {
            vo.setPassword(share.getPassword());
        }
        vo.setExpireTime(share.getExpireTime());
        vo.setExpired(share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now()));
        vo.setViewCount(share.getViewCount());
        vo.setDownloadCount(share.getDownloadCount());
        vo.setMaxViewCount(share.getMaxViewCount());
        vo.setStatus(share.getStatus());
        vo.setCreateTime(share.getCreateTime());
        return vo;
    }

    /**
     * 检查分享是否有效
     */
    private void checkShareValid(Share share) {
        // 检查状态
        if (share.getStatus() == ShareStatusEnum.CANCELLED.getCode()) {
            throw new BusinessException(ResultCode.SHARE_CANCELLED);
        }

        // 检查是否过期
        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.SHARE_EXPIRED);
        }

        // 检查访问次数
        if (share.getMaxViewCount() > 0 && share.getViewCount() >= share.getMaxViewCount()) {
            throw new BusinessException(ResultCode.SHARE_EXPIRED, "分享已达到最大访问次数");
        }
    }

    /**
     * 生成分享码（6位字母数字）
     */
    private String generateShareCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    /**
     * 生成提取码（4位字母数字）
     */
    private String generatePassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 4);
    }
}

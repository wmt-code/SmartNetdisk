package com.wmt.smartnetdisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wmt.smartnetdisk.common.result.PageResult;
import com.wmt.smartnetdisk.common.result.PageRequest;
import com.wmt.smartnetdisk.dto.request.CreateShareDTO;
import com.wmt.smartnetdisk.entity.Share;
import com.wmt.smartnetdisk.vo.ShareVO;

import java.util.List;

/**
 * 分享服务接口
 *
 * @author wmt
 * @since 1.0.0
 */
public interface IShareService extends IService<Share> {

    /**
     * 创建分享（支持单文件、目录、批量分享）
     *
     * @param userId    用户ID
     * @param createDTO 创建请求
     * @return 分享信息（包含提取码）
     */
    ShareVO createShare(Long userId, CreateShareDTO createDTO);

    /**
     * 获取我的分享列表
     *
     * @param userId      用户ID
     * @param pageRequest 分页请求
     * @return 分页结果
     */
    PageResult<ShareVO> listMyShares(Long userId, PageRequest pageRequest);

    /**
     * 取消分享
     *
     * @param userId  用户ID
     * @param shareId 分享ID
     */
    void cancelShare(Long userId, Long shareId);

    /**
     * 根据分享码获取分享信息（公开）
     *
     * @param shareCode 分享码
     * @return 分享信息（不含提取码）
     */
    ShareVO getShareByCode(String shareCode);

    /**
     * 验证提取码
     *
     * @param shareCode 分享码
     * @param password  提取码
     * @return 验证通过返回文件下载 URL，否则返回 null
     */
    String verifyPassword(String shareCode, String password);

    /**
     * 验证提取码并返回完整的分享信息
     *
     * @param shareCode 分享码
     * @param password  提取码
     * @return 分享信息（包含文件名、大小等）
     */
    ShareVO verifyPasswordAndGetInfo(String shareCode, String password);

    /**
     * 获取分享文件下载链接
     *
     * @param shareCode 分享码
     * @param password  提取码（如果有的话）
     * @return 下载 URL
     */
    String getDownloadUrl(String shareCode, String password);

    /**
     * 流式下载分享文件
     *
     * @param shareCode 分享码
     * @param password  提取码（如果有的话）
     * @param response  HTTP响应对象
     */
    void downloadShareStream(String shareCode, String password, jakarta.servlet.http.HttpServletResponse response);

    /**
     * 获取分享项列表（批量/目录分享时使用）
     *
     * @param shareCode 分享码
     * @param password  提取码（如果有的话）
     * @return 分享项列表
     */
    List<ShareVO.ShareItemVO> getShareItems(String shareCode, String password);

    /**
     * 浏览分享文件夹内容（支持进入子文件夹）
     *
     * @param shareCode 分享码
     * @param password  提取码（如果有的话）
     * @param folderId  要浏览的文件夹ID（0表示根目录）
     * @return 文件夹内容列表
     */
    List<ShareVO.ShareItemVO> browseFolderContents(String shareCode, String password, Long folderId);

    /**
     * 获取分享中指定文件的下载链接
     *
     * @param shareCode 分享码
     * @param password  提取码（如果有的话）
     * @param fileId    文件ID
     * @return 下载 URL
     */
    String getFileDownloadUrl(String shareCode, String password, Long fileId);

    /**
     * 流式下载分享中的指定文件
     *
     * @param shareCode 分享码
     * @param password  提取码（如果有的话）
     * @param fileId    文件ID
     * @param response  HTTP响应对象
     */
    void downloadFileStream(String shareCode, String password, Long fileId,
            jakarta.servlet.http.HttpServletResponse response);

    /**
     * 获取分享文件预览链接（kkFileView）
     *
     * @param shareCode 分享码
     * @param password  提取码（如果有的话）
     * @param fileId    文件ID
     * @return kkFileView 预览 URL
     */
    String getFilePreviewUrl(String shareCode, String password, Long fileId);

    /**
     * 流式传输分享文件（支持 Range 请求，用于视频/音频/图片预览）
     *
     * @param shareCode   分享码
     * @param password    提取码
     * @param fileId      文件ID
     * @param rangeHeader Range 请求头
     * @param response    HTTP响应对象
     */
    void streamFile(String shareCode, String password, Long fileId,
            String rangeHeader, jakarta.servlet.http.HttpServletResponse response);

    /**
     * 将分享实体转换为视图对象
     *
     * @param share           分享实体
     * @param includePassword 是否包含提取码
     * @return 分享视图对象
     */
    ShareVO toVO(Share share, boolean includePassword);
}

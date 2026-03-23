package com.wmt.smartnetdisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wmt.smartnetdisk.common.result.PageResult;
import com.wmt.smartnetdisk.dto.request.FastUploadDTO;
import com.wmt.smartnetdisk.dto.request.FileListDTO;
import com.wmt.smartnetdisk.entity.FileInfo;
import com.wmt.smartnetdisk.vo.FileVO;
import com.wmt.smartnetdisk.vo.UploadResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务接口
 *
 * @author wmt
 * @since 1.0.0
 */
public interface IFileService extends IService<FileInfo> {

    /**
     * 分页查询文件列表
     *
     * @param userId  用户ID
     * @param listDTO 查询条件
     * @return 分页结果
     */
    PageResult<FileVO> listFiles(Long userId, FileListDTO listDTO);

    /**
     * 获取最近访问的文件列表
     */
    PageResult<FileVO> listRecentFiles(Long userId, FileListDTO listDTO);

    /**
     * 根据 MD5 检查文件是否存在（秒传检测）
     *
     * @param fileMd5 文件MD5
     * @return 已存在的文件，不存在返回 null
     */
    FileInfo getByMd5(String fileMd5);

    /**
     * 获取文件详情
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     * @return 文件视图对象
     */
    FileVO getFileDetail(Long userId, Long fileId);

    /**
     * 重命名文件
     *
     * @param userId  用户ID
     * @param fileId  文件ID
     * @param newName 新文件名
     */
    void renameFile(Long userId, Long fileId, String newName);

    /**
     * 移动文件
     *
     * @param userId         用户ID
     * @param fileId         文件ID
     * @param targetFolderId 目标文件夹ID
     */
    void moveFile(Long userId, Long fileId, Long targetFolderId);

    /**
     * 批量移动文件
     *
     * @param userId         用户ID
     * @param fileIds        文件ID列表
     * @param targetFolderId 目标文件夹ID
     */
    void batchMoveFiles(Long userId, List<Long> fileIds, Long targetFolderId);

    /**
     * 复制文件
     * <p>
     * 在目标文件夹创建文件副本，不复制实际存储文件（共享同一存储路径）
     * </p>
     *
     * @param userId         用户ID
     * @param fileId         文件ID
     * @param targetFolderId 目标文件夹ID
     * @return 新文件的视图对象
     */
    FileVO copyFile(Long userId, Long fileId, Long targetFolderId);

    /**
     * 批量复制文件
     *
     * @param userId         用户ID
     * @param fileIds        文件ID列表
     * @param targetFolderId 目标文件夹ID
     * @return 新文件的视图对象列表
     */
    List<FileVO> batchCopyFiles(Long userId, List<Long> fileIds, Long targetFolderId);

    /**
     * 删除文件（移入回收站）
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     */
    void deleteFile(Long userId, Long fileId);

    /**
     * 批量删除文件（移入回收站）
     *
     * @param userId  用户ID
     * @param fileIds 文件ID列表
     */
    void batchDeleteFiles(Long userId, List<Long> fileIds);

    /**
     * 彻底删除文件
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     */
    void permanentDeleteFile(Long userId, Long fileId);

    /**
     * 恢复文件（从回收站）
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     */
    void restoreFile(Long userId, Long fileId);

    /**
     * 获取回收站文件列表
     *
     * @param userId  用户ID
     * @param listDTO 查询条件
     * @return 分页结果
     */
    PageResult<FileVO> listRecycledFiles(Long userId, FileListDTO listDTO);

    /**
     * 清空回收站
     *
     * @param userId 用户ID
     */
    void clearRecycleBin(Long userId);

    /**
     * 将文件实体转换为视图对象
     *
     * @param fileInfo 文件实体
     * @return 文件视图对象
     */
    FileVO toVO(FileInfo fileInfo);

    /**
     * 计算文件夹大小（递归计算所有子文件和子文件夹中的文件总大小）
     *
     * @param userId   用户ID
     * @param folderId 文件夹ID
     * @return 文件夹总大小（字节）
     */
    Long calculateFolderSize(Long userId, Long folderId);

    // ==================== 文件上传相关 ====================

    /**
     * 秒传检测
     *
     * @param userId        用户ID
     * @param fastUploadDTO 秒传请求
     * @return 秒传成功返回上传结果，否则返回 null
     */
    UploadResultVO checkFastUpload(Long userId, FastUploadDTO fastUploadDTO);

    /**
     * 普通文件上传
     *
     * @param userId   用户ID
     * @param file     文件
     * @param folderId 目标文件夹ID
     * @return 上传结果
     */
    UploadResultVO uploadFile(Long userId, MultipartFile file, Long folderId);

    /**
     * 获取文件下载 URL（触发浏览器下载）
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     * @param expiry 有效期（秒）
     * @return 预签名下载 URL
     */
    String getDownloadUrl(Long userId, Long fileId, int expiry);

    /**
     * 获取文件预览 URL（浏览器内联显示）
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     * @param expiry 有效期（秒）
     * @return 预签名预览 URL
     */
    String getPreviewUrl(Long userId, Long fileId, int expiry);

    /**
     * 获取 kkFileView 预览 URL（用于 Office、PDF 等文档）
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     * @param expiry 有效期（秒）
     * @return kkFileView 预览页面 URL
     */
    String getKkFileViewPreviewUrl(Long userId, Long fileId, int expiry);

    /**
     * 获取文件实体（验证用户权限）
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     * @return 文件实体
     */
    FileInfo getFileWithPermission(Long userId, Long fileId);

    /**
     * 获取文件文本内容（用于在线编辑）
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     * @param offset 偏移量（从第几个字节开始读取，null 表示从头开始）
     * @param limit  限制读取的字节数（null 表示使用默认分块大小）
     * @return 包含文件内容和元信息的 Map
     */
    java.util.Map<String, Object> getFileContent(Long userId, Long fileId, Long offset, Long limit);

    /**
     * 保存文件文本内容（在线编辑后保存）
     *
     * @param userId  用户ID
     * @param fileId  文件ID
     * @param content 文件内容
     */
    void saveFileContent(Long userId, Long fileId, String content);
}

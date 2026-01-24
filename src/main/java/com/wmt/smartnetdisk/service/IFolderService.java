package com.wmt.smartnetdisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wmt.smartnetdisk.dto.request.CreateFolderDTO;
import com.wmt.smartnetdisk.entity.Folder;
import com.wmt.smartnetdisk.vo.FolderVO;

import java.util.List;

/**
 * 文件夹服务接口
 *
 * @author wmt
 * @since 1.0.0
 */
public interface IFolderService extends IService<Folder> {

    /**
     * 创建文件夹
     *
     * @param userId    用户ID
     * @param createDTO 创建请求
     * @return 创建的文件夹
     */
    FolderVO createFolder(Long userId, CreateFolderDTO createDTO);

    /**
     * 按路径创建文件夹 (如果由于并发导致已存在则直接返回)
     *
     * @param userId        用户ID
     * @param createPathDTO 创建请求
     * @return 最终的文件夹ID
     */
    Long createFolderPath(Long userId, com.wmt.smartnetdisk.dto.request.CreateFolderPathDTO createPathDTO);

    /**
     * 获取文件夹详情
     *
     * @param userId   用户ID
     * @param folderId 文件夹ID
     * @return 文件夹视图对象
     */
    FolderVO getFolderDetail(Long userId, Long folderId);

    /**
     * 重命名文件夹
     *
     * @param userId   用户ID
     * @param folderId 文件夹ID
     * @param newName  新名称
     */
    void renameFolder(Long userId, Long folderId, String newName);

    /**
     * 删除文件夹
     *
     * @param userId   用户ID
     * @param folderId 文件夹ID
     */
    void deleteFolder(Long userId, Long folderId);

    /**
     * 恢复文件夹
     *
     * @param userId   用户ID
     * @param folderId 文件夹ID
     */
    void restoreFolder(Long userId, Long folderId);

    /**
     * 彻底删除文件夹
     *
     * @param userId   用户ID
     * @param folderId 文件夹ID
     */
    void permanentDeleteFolder(Long userId, Long folderId);

    /**
     * 获取文件夹树
     *
     * @param userId 用户ID
     * @return 文件夹树
     */
    List<FolderVO> getFolderTree(Long userId);

    /**
     * 获取子文件夹列表
     *
     * @param userId   用户ID
     * @param parentId 父文件夹ID
     * @return 子文件夹列表
     */
    List<FolderVO> getChildren(Long userId, Long parentId);

    /**
     * 检查文件夹是否存在
     *
     * @param userId   用户ID
     * @param folderId 文件夹ID
     * @return 是否存在
     */
    boolean existsById(Long userId, Long folderId);

    /**
     * 将文件夹实体转换为视图对象
     *
     * @param folder 文件夹实体
     * @return 文件夹视图对象
     */
    FolderVO toVO(Folder folder);
}

package com.wmt.smartnetdisk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmt.smartnetdisk.common.exception.BusinessException;
import com.wmt.smartnetdisk.common.result.ResultCode;
import com.wmt.smartnetdisk.dto.request.CreateFolderDTO;
import com.wmt.smartnetdisk.entity.Folder;
import com.wmt.smartnetdisk.mapper.FolderMapper;
import com.wmt.smartnetdisk.service.IFolderService;
import com.wmt.smartnetdisk.vo.FolderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件夹服务实现类
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FolderServiceImpl extends ServiceImpl<FolderMapper, Folder> implements IFolderService {

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private com.wmt.smartnetdisk.service.IFileService fileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFolderPath(Long userId, com.wmt.smartnetdisk.dto.request.CreateFolderPathDTO createPathDTO) {
        String path = createPathDTO.getPath();
        Long parentId = createPathDTO.getParentId();

        // 统一分隔符
        path = path.replace("\\", "/");
        if (path.startsWith("/"))
            path = path.substring(1);
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);

        String[] folderNames = path.split("/");
        Long currentParentId = parentId;

        for (String folderName : folderNames) {
            if (folderName.isEmpty())
                continue;

            // 检查文件夹是否存在
            LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Folder::getUserId, userId)
                    .eq(Folder::getParentId, currentParentId)
                    .eq(Folder::getFolderName, folderName)
                    .eq(Folder::getDeleted, 0);

            Folder folder = getOne(wrapper);

            if (folder == null) {
                // 不存在则创建
                folder = new Folder();
                folder.setUserId(userId);
                folder.setParentId(currentParentId);
                folder.setFolderName(folderName);

                try {
                    save(folder);
                } catch (Exception e) {
                    // 并发情况下可能已存在，尝试再次查询
                    folder = getOne(wrapper);
                    if (folder == null) {
                        throw new BusinessException(ResultCode.BAD_REQUEST, "创建文件夹失败");
                    }
                }
            }

            currentParentId = folder.getId();
        }

        return currentParentId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FolderVO createFolder(Long userId, CreateFolderDTO createDTO) {
        // 检查父文件夹是否存在
        if (createDTO.getParentId() != 0) {
            if (!existsById(userId, createDTO.getParentId())) {
                throw new BusinessException(ResultCode.FOLDER_NOT_FOUND, "父文件夹不存在");
            }
        }

        // 检查同级目录下是否存在同名文件夹
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Folder::getUserId, userId)
                .eq(Folder::getParentId, createDTO.getParentId())
                .eq(Folder::getFolderName, createDTO.getFolderName())
                .eq(Folder::getDeleted, 0);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.FOLDER_NAME_DUPLICATE);
        }

        Folder folder = new Folder();
        folder.setUserId(userId);
        folder.setParentId(createDTO.getParentId());
        folder.setFolderName(createDTO.getFolderName());

        save(folder);
        log.info("文件夹创建成功: userId={}, folderName={}", userId, createDTO.getFolderName());
        return toVO(folder);
    }

    @Override
    public FolderVO getFolderDetail(Long userId, Long folderId) {
        Folder folder = getById(folderId);
        if (folder == null || !folder.getUserId().equals(userId) || folder.getDeleted() == 1) {
            throw new BusinessException(ResultCode.FOLDER_NOT_FOUND);
        }
        return toVO(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameFolder(Long userId, Long folderId, String newName) {
        Folder folder = getById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FOLDER_NOT_FOUND);
        }

        // 检查同级目录下是否存在同名文件夹
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Folder::getUserId, userId)
                .eq(Folder::getParentId, folder.getParentId())
                .eq(Folder::getFolderName, newName)
                .ne(Folder::getId, folderId)
                .eq(Folder::getDeleted, 0);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.FOLDER_NAME_DUPLICATE);
        }

        folder.setFolderName(newName);
        updateById(folder);
        log.info("文件夹重命名成功: folderId={}, newName={}", folderId, newName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long userId, Long folderId) {
        Folder folder = getById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FOLDER_NOT_FOUND);
        }
        // 检查文件夹是否已删除
        if (folder.getDeleted() == 1) {
            log.warn("文件夹已在回收站中: folderId={}", folderId);
            return;
        }
        // 软删除（移入回收站）
        folder.setDeleted(1);
        folder.setDeleteTime(java.time.LocalDateTime.now());
        boolean updated = updateById(folder);
        if (!updated) {
            log.error("文件夹删除失败，数据库更新失败: folderId={}", folderId);
            throw new BusinessException(ResultCode.DATA_UPDATE_FAIL, "文件夹删除失败");
        }
        log.info("文件夹移入回收站成功: folderId={}, folderName={}", folderId, folder.getFolderName());
        // TODO: 递归删除子文件夹和文件
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreFolder(Long userId, Long folderId) {
        Folder folder = getById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FOLDER_NOT_FOUND);
        }
        folder.setDeleted(0);
        folder.setDeleteTime(null);
        updateById(folder);
        log.info("文件夹恢复成功: folderId={}, folderName={}", folderId, folder.getFolderName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permanentDeleteFolder(Long userId, Long folderId) {
        Folder folder = getById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FOLDER_NOT_FOUND);
        }
        // 递归删除子文件夹和文件
        recursivePermanentDelete(userId, folderId);
        log.info("文件夹彻底删除成功: folderId={}, folderName={}", folderId, folder.getFolderName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearRecycleBin(Long userId) {
        // 查询所有已删除的文件夹
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Folder::getUserId, userId)
                .eq(Folder::getDeleted, 1);
        List<Folder> recycledFolders = list(wrapper);

        for (Folder folder : recycledFolders) {
            // 递归彻底删除（包括子文件夹和文件）
            recursivePermanentDelete(userId, folder.getId());
        }
        log.info("回收站文件夹清空成功: userId={}, count={}", userId, recycledFolders.size());
    }

    /**
     * 递归彻底删除文件夹及其内容
     */
    private void recursivePermanentDelete(Long userId, Long folderId) {
        // 1. 删除当前文件夹下的所有文件
        List<com.wmt.smartnetdisk.entity.FileInfo> files = fileService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.wmt.smartnetdisk.entity.FileInfo>()
                        .eq(com.wmt.smartnetdisk.entity.FileInfo::getUserId, userId)
                        .eq(com.wmt.smartnetdisk.entity.FileInfo::getFolderId, folderId));

        for (com.wmt.smartnetdisk.entity.FileInfo file : files) {
            fileService.permanentDeleteFile(userId, file.getId());
        }

        // 2. 递归删除子文件夹
        List<Folder> children = list(new LambdaQueryWrapper<Folder>()
                .eq(Folder::getUserId, userId)
                .eq(Folder::getParentId, folderId));

        for (Folder child : children) {
            recursivePermanentDelete(userId, child.getId());
        }

        // 3. 删除当前文件夹
        removeById(folderId);
    }

    @Override
    public List<FolderVO> getFolderTree(Long userId) {
        List<Folder> allFolders = baseMapper.selectByUserId(userId);
        return buildTree(allFolders, 0L);
    }

    @Override
    public List<FolderVO> getChildren(Long userId, Long parentId) {
        List<Folder> children = baseMapper.selectByParentId(userId, parentId);
        return children.stream().map(this::toVO).toList();
    }

    @Override
    public boolean existsById(Long userId, Long folderId) {
        if (folderId == null || folderId == 0) {
            return true; // 根目录始终存在
        }
        Folder folder = getById(folderId);
        return folder != null && folder.getUserId().equals(userId) && folder.getDeleted() == 0;
    }

    @Override
    public FolderVO toVO(Folder folder) {
        if (folder == null) {
            return null;
        }
        FolderVO vo = new FolderVO();
        vo.setId(folder.getId());
        vo.setFolderName(folder.getFolderName());
        vo.setParentId(folder.getParentId());
        vo.setCreateTime(folder.getCreateTime());
        return vo;
    }

    /**
     * 构建文件夹树
     */
    private List<FolderVO> buildTree(List<Folder> allFolders, Long parentId) {
        List<FolderVO> tree = new ArrayList<>();
        Map<Long, List<Folder>> childrenMap = allFolders.stream()
                .collect(Collectors.groupingBy(Folder::getParentId));

        List<Folder> children = childrenMap.getOrDefault(parentId, new ArrayList<>());
        for (Folder folder : children) {
            FolderVO vo = toVO(folder);
            vo.setChildren(buildTree(allFolders, folder.getId()));
            tree.add(vo);
        }
        return tree;
    }
}

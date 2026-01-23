package com.wmt.smartnetdisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wmt.smartnetdisk.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 文件 Mapper 接口
 *
 * @author wmt
 * @since 1.0.0
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    /**
     * 联合查询文件夹和文件（优化性能：单次查询代替两次查询）
     * 文件夹在前，文件在后，统一分页
     *
     * @param userId   用户ID
     * @param folderId 文件夹ID
     * @param offset   偏移量
     * @param limit    限制数量
     * @return 文件列表（包含文件夹）
     */
    @Select("""
            (
                SELECT id, folder_name AS file_name, 0 AS file_size, 'folder' AS file_type,
                       '' AS file_ext, NULL AS thumbnail_path, 0 AS is_vectorized,
                       parent_id AS folder_id, create_time, create_time AS update_time, 0 AS sort_order
                FROM folder
                WHERE user_id = #{userId} AND parent_id = #{folderId} AND deleted = 0
            )
            UNION ALL
            (
                SELECT id, file_name, file_size, file_type, file_ext, thumbnail_path,
                       is_vectorized, folder_id, create_time, update_time, 1 AS sort_order
                FROM file_info
                WHERE user_id = #{userId} AND folder_id = #{folderId} AND deleted = 0
            )
            ORDER BY sort_order ASC, create_time DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Map<String, Object>> listFilesAndFolders(@Param("userId") Long userId,
                                                   @Param("folderId") Long folderId,
                                                   @Param("offset") long offset,
                                                   @Param("limit") int limit);

    /**
     * 统计文件夹和文件总数
     */
    @Select("""
            SELECT (
                SELECT COUNT(*) FROM folder
                WHERE user_id = #{userId} AND parent_id = #{folderId} AND deleted = 0
            ) + (
                SELECT COUNT(*) FROM file_info
                WHERE user_id = #{userId} AND folder_id = #{folderId} AND deleted = 0
            ) AS total
            """)
    long countFilesAndFolders(@Param("userId") Long userId, @Param("folderId") Long folderId);

    /**
     * 根据 MD5 查询文件
     *
     * @param fileMd5 文件MD5
     * @return 文件信息
     */
    @Select("SELECT * FROM file_info WHERE file_md5 = #{fileMd5} AND deleted = 0 LIMIT 1")
    FileInfo selectByMd5(@Param("fileMd5") String fileMd5);

    /**
     * 统计用户文件数量
     *
     * @param userId 用户ID
     * @return 文件数量
     */
    @Select("SELECT COUNT(*) FROM file_info WHERE user_id = #{userId} AND deleted = 0")
    Long countByUserId(@Param("userId") Long userId);

    /**
     * 计算指定文件夹下所有文件的总大小
     *
     * @param userId   用户ID
     * @param folderId 文件夹ID
     * @return 文件总大小（字节）
     */
    @Select("SELECT COALESCE(SUM(file_size), 0) FROM file_info WHERE user_id = #{userId} AND folder_id = #{folderId} AND deleted = 0")
    Long sumFileSizeByFolderId(@Param("userId") Long userId, @Param("folderId") Long folderId);

    /**
     * 递归计算文件夹及其所有子文件夹下的文件统计（使用 PostgreSQL CTE）
     * 返回 Map 包含：totalSize（总大小）、fileCount（文件数）
     *
     * @param userId   用户ID
     * @param folderId 根文件夹ID
     * @return 统计结果
     */
    @Select("""
            WITH RECURSIVE folder_tree AS (
                SELECT id FROM folder WHERE id = #{folderId} AND user_id = #{userId} AND deleted = 0
                UNION ALL
                SELECT f.id FROM folder f
                INNER JOIN folder_tree ft ON f.parent_id = ft.id
                WHERE f.user_id = #{userId} AND f.deleted = 0
            )
            SELECT
                COALESCE(SUM(fi.file_size), 0) AS totalSize,
                COUNT(fi.id) AS fileCount
            FROM file_info fi
            WHERE fi.user_id = #{userId}
              AND fi.folder_id IN (SELECT id FROM folder_tree)
              AND fi.deleted = 0
            """)
    Map<String, Object> sumFileSizeRecursive(@Param("userId") Long userId, @Param("folderId") Long folderId);

    /**
     * 联合查询回收站中的文件夹和文件
     *
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 已删除的文件列表（包含文件夹）
     */
    @Select("""
            (
                SELECT id, folder_name AS file_name, 0 AS file_size, 'folder' AS file_type,
                       '' AS file_ext, NULL AS thumbnail_path, 0 AS is_vectorized,
                       parent_id AS folder_id, create_time, update_time, delete_time, 0 AS sort_order
                FROM folder
                WHERE user_id = #{userId} AND deleted = 1
            )
            UNION ALL
            (
                SELECT id, file_name, file_size, file_type, file_ext, thumbnail_path,
                       is_vectorized, folder_id, create_time, update_time, delete_time, 1 AS sort_order
                FROM file_info
                WHERE user_id = #{userId} AND deleted = 1
            )
            ORDER BY delete_time DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Map<String, Object>> listRecycledFilesAndFolders(@Param("userId") Long userId,
                                                           @Param("offset") long offset,
                                                           @Param("limit") int limit);

    /**
     * 统计回收站中的文件夹和文件总数
     */
    @Select("""
            SELECT (
                SELECT COUNT(*) FROM folder
                WHERE user_id = #{userId} AND deleted = 1
            ) + (
                SELECT COUNT(*) FROM file_info
                WHERE user_id = #{userId} AND deleted = 1
            ) AS total
            """)
    long countRecycledFilesAndFolders(@Param("userId") Long userId);
}

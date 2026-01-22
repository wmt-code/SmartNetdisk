package com.wmt.smartnetdisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wmt.smartnetdisk.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文件 Mapper 接口
 *
 * @author wmt
 * @since 1.0.0
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

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
}

package com.wmt.smartnetdisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wmt.smartnetdisk.entity.Folder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文件夹 Mapper 接口
 *
 * @author wmt
 * @since 1.0.0
 */
@Mapper
public interface FolderMapper extends BaseMapper<Folder> {

    /**
     * 查询用户的所有文件夹
     *
     * @param userId 用户ID
     * @return 文件夹列表
     */
    @Select("SELECT * FROM folder WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time")
    List<Folder> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询子文件夹
     *
     * @param userId   用户ID
     * @param parentId 父文件夹ID
     * @return 子文件夹列表
     */
    @Select("SELECT * FROM folder WHERE user_id = #{userId} AND parent_id = #{parentId} AND deleted = 0 ORDER BY folder_name")
    List<Folder> selectByParentId(@Param("userId") Long userId, @Param("parentId") Long parentId);
}

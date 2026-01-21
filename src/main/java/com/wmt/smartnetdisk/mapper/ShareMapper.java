package com.wmt.smartnetdisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wmt.smartnetdisk.entity.Share;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 分享 Mapper 接口
 *
 * @author wmt
 * @since 1.0.0
 */
@Mapper
public interface ShareMapper extends BaseMapper<Share> {

    /**
     * 根据分享码查询分享
     *
     * @param shareCode 分享码
     * @return 分享实体
     */
    @Select("SELECT * FROM share WHERE share_code = #{shareCode}")
    Share selectByShareCode(@Param("shareCode") String shareCode);

    /**
     * 增加访问次数
     *
     * @param id 分享ID
     * @return 影响行数
     */
    @Update("UPDATE share SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 增加下载次数
     *
     * @param id 分享ID
     * @return 影响行数
     */
    @Update("UPDATE share SET download_count = download_count + 1 WHERE id = #{id}")
    int incrementDownloadCount(@Param("id") Long id);
}

package com.wmt.smartnetdisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wmt.smartnetdisk.entity.ShareItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分享项 Mapper 接口
 *
 * @author wmt
 * @since 2.0.0
 */
@Mapper
public interface ShareItemMapper extends BaseMapper<ShareItem> {

    /**
     * 根据分享ID查询分享项列表
     *
     * @param shareId 分享ID
     * @return 分享项列表
     */
    @Select("SELECT * FROM share_item WHERE share_id = #{shareId} ORDER BY id")
    List<ShareItem> selectByShareId(@Param("shareId") Long shareId);
}

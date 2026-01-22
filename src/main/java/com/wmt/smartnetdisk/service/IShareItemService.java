package com.wmt.smartnetdisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wmt.smartnetdisk.entity.ShareItem;

import java.util.List;

/**
 * 分享项服务接口
 *
 * @author wmt
 * @since 2.0.0
 */
public interface IShareItemService extends IService<ShareItem> {

    /**
     * 根据分享ID查询分享项列表
     *
     * @param shareId 分享ID
     * @return 分享项列表
     */
    List<ShareItem> listByShareId(Long shareId);

    /**
     * 批量保存分享项
     *
     * @param shareItems 分享项列表
     * @return 是否成功
     */
    boolean saveBatch(List<ShareItem> shareItems);
}

package com.wmt.smartnetdisk.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmt.smartnetdisk.entity.ShareItem;
import com.wmt.smartnetdisk.mapper.ShareItemMapper;
import com.wmt.smartnetdisk.service.IShareItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分享项服务实现类
 *
 * @author wmt
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareItemServiceImpl extends ServiceImpl<ShareItemMapper, ShareItem> implements IShareItemService {

    @Override
    public List<ShareItem> listByShareId(Long shareId) {
        return baseMapper.selectByShareId(shareId);
    }

    @Override
    public boolean saveBatch(List<ShareItem> shareItems) {
        return saveBatch(shareItems, shareItems.size());
    }
}

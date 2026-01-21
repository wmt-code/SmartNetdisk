package com.wmt.smartnetdisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wmt.smartnetdisk.dto.request.ChunkMergeDTO;
import com.wmt.smartnetdisk.dto.request.ChunkUploadDTO;
import com.wmt.smartnetdisk.dto.request.FastUploadDTO;
import com.wmt.smartnetdisk.entity.FileChunk;
import com.wmt.smartnetdisk.vo.ChunkCheckResultVO;
import com.wmt.smartnetdisk.vo.UploadResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件分片服务接口
 *
 * @author wmt
 * @since 1.0.0
 */
public interface IFileChunkService extends IService<FileChunk> {

    /**
     * 检查分片上传状态（秒传检测 + 断点续传）
     *
     * @param userId        用户ID
     * @param fastUploadDTO 秒传检测请求
     * @return 分片检查结果
     */
    ChunkCheckResultVO checkChunks(Long userId, FastUploadDTO fastUploadDTO);

    /**
     * 上传单个分片
     *
     * @param userId    用户ID
     * @param uploadDTO 分片信息
     * @param chunkFile 分片文件
     */
    void uploadChunk(Long userId, ChunkUploadDTO uploadDTO, MultipartFile chunkFile);

    /**
     * 合并所有分片
     *
     * @param userId   用户ID
     * @param mergeDTO 合并请求
     * @return 上传结果
     */
    UploadResultVO mergeChunks(Long userId, ChunkMergeDTO mergeDTO);

    /**
     * 获取已上传的分片索引列表
     *
     * @param fileMd5 文件MD5
     * @return 已上传的分片索引列表
     */
    List<Integer> getUploadedChunkIndexes(String fileMd5);

    /**
     * 清理分片记录和临时文件
     *
     * @param fileMd5 文件MD5
     */
    void cleanupChunks(String fileMd5);
}

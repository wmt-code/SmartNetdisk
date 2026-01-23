package com.wmt.smartnetdisk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分片检查结果 VO
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkCheckResultVO {

    /**
     * 是否秒传成功（true 表示秒传成功，无需上传）
     */
    private Boolean fastUploaded;

    /**
     * 秒传成功时的文件信息
     */
    private UploadResultVO uploadResult;

    /**
     * 已上传的分片索引列表（用于断点续传）
     */
    private List<Integer> uploadedChunks;

    /**
     * 创建需要上传的结果
     */
    public static ChunkCheckResultVO needUpload(List<Integer> uploadedChunks) {
        ChunkCheckResultVO vo = new ChunkCheckResultVO();
        vo.setFastUploaded(false);
        vo.setUploadedChunks(uploadedChunks);
        return vo;
    }

    /**
     * 创建秒传成功的结果
     */
    public static ChunkCheckResultVO fastUploaded(UploadResultVO uploadResult) {
        ChunkCheckResultVO vo = new ChunkCheckResultVO();
        vo.setFastUploaded(true);
        vo.setUploadResult(uploadResult);
        return vo;
    }
}

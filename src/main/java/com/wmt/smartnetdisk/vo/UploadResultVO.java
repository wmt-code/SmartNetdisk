package com.wmt.smartnetdisk.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件上传结果视图对象
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class UploadResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否秒传
     */
    private Boolean fastUpload;

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件MD5
     */
    private String fileMd5;

    /**
     * 创建秒传结果
     */
    public static UploadResultVO fastUpload(Long fileId, String fileName) {
        UploadResultVO vo = new UploadResultVO();
        vo.setFastUpload(true);
        vo.setFileId(fileId);
        vo.setFileName(fileName);
        return vo;
    }

    /**
     * 创建普通上传结果
     */
    public static UploadResultVO normalUpload(Long fileId, String fileName, Long fileSize, String fileMd5) {
        UploadResultVO vo = new UploadResultVO();
        vo.setFastUpload(false);
        vo.setFileId(fileId);
        vo.setFileName(fileName);
        vo.setFileSize(fileSize);
        vo.setFileMd5(fileMd5);
        return vo;
    }
}

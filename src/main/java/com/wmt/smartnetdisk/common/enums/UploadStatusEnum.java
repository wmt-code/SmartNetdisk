package com.wmt.smartnetdisk.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 上传状态枚举
 *
 * @author wmt
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum UploadStatusEnum {

    /**
     * 上传中
     */
    UPLOADING(0, "上传中"),

    /**
     * 已完成
     */
    COMPLETED(1, "已完成"),

    /**
     * 转码中
     */
    TRANSCODING(2, "转码中"),

    /**
     * 上传失败
     */
    FAILED(3, "上传失败");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 上传状态枚举
     */
    public static UploadStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UploadStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否上传完成
     *
     * @return 是否完成
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }

    /**
     * 判断是否正在处理中
     *
     * @return 是否处理中
     */
    public boolean isProcessing() {
        return this == UPLOADING || this == TRANSCODING;
    }
}

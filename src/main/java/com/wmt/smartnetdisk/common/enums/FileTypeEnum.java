package com.wmt.smartnetdisk.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

import static com.wmt.smartnetdisk.common.constant.FileConstants.*;

/**
 * 文件类型枚举
 *
 * @author wmt
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum FileTypeEnum {

    /**
     * 图片
     */
    IMAGE(FILE_TYPE_IMAGE, "图片", IMAGE_EXTENSIONS),

    /**
     * 视频
     */
    VIDEO(FILE_TYPE_VIDEO, "视频", VIDEO_EXTENSIONS),

    /**
     * 音频
     */
    AUDIO(FILE_TYPE_AUDIO, "音频", AUDIO_EXTENSIONS),

    /**
     * 文档
     */
    DOCUMENT(FILE_TYPE_DOCUMENT, "文档", DOCUMENT_EXTENSIONS),

    /**
     * 其他
     */
    OTHER(FILE_TYPE_OTHER, "其他", Set.of());

    /**
     * 类型编码
     */
    private final String code;

    /**
     * 类型名称
     */
    private final String name;

    /**
     * 支持的扩展名集合
     */
    private final Set<String> extensions;

    /**
     * 根据文件扩展名获取文件类型
     *
     * @param extension 文件扩展名（不含点号）
     * @return 文件类型枚举
     */
    public static FileTypeEnum getByExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return OTHER;
        }
        String ext = extension.toLowerCase();
        for (FileTypeEnum type : values()) {
            if (type.getExtensions().contains(ext)) {
                return type;
            }
        }
        return OTHER;
    }

    /**
     * 根据类型编码获取枚举
     *
     * @param code 类型编码
     * @return 文件类型枚举
     */
    public static FileTypeEnum getByCode(String code) {
        if (code == null || code.isBlank()) {
            return OTHER;
        }
        for (FileTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
}

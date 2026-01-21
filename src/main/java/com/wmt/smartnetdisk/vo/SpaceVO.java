package com.wmt.smartnetdisk.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 空间使用情况视图对象
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class SpaceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 已用空间（字节）
     */
    private Long usedSpace;

    /**
     * 总空间（字节）
     */
    private Long totalSpace;

    /**
     * 剩余空间（字节）
     */
    private Long freeSpace;

    /**
     * 使用百分比
     */
    private Double usedPercent;

    /**
     * 已用空间（格式化后，如 "1.5 GB"）
     */
    private String usedSpaceStr;

    /**
     * 总空间（格式化后）
     */
    private String totalSpaceStr;

    /**
     * 剩余空间（格式化后）
     */
    private String freeSpaceStr;

    /**
     * 格式化文件大小
     *
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    public static String formatSize(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "0 B";
        }
        String[] units = { "B", "KB", "MB", "GB", "TB" };
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    /**
     * 创建空间视图对象
     *
     * @param usedSpace  已用空间
     * @param totalSpace 总空间
     * @return 空间视图对象
     */
    public static SpaceVO of(Long usedSpace, Long totalSpace) {
        SpaceVO vo = new SpaceVO();
        vo.setUsedSpace(usedSpace);
        vo.setTotalSpace(totalSpace);
        vo.setFreeSpace(totalSpace - usedSpace);
        if (totalSpace > 0) {
            double percent = (usedSpace * 100.0) / totalSpace;
            vo.setUsedPercent(Math.round(percent * 100.0) / 100.0);
        } else {
            vo.setUsedPercent(0.0);
        }
        vo.setUsedSpaceStr(formatSize(usedSpace));
        vo.setTotalSpaceStr(formatSize(totalSpace));
        vo.setFreeSpaceStr(formatSize(vo.getFreeSpace()));
        return vo;
    }
}

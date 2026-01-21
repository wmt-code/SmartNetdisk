package com.wmt.smartnetdisk.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分享状态枚举
 *
 * @author wmt
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ShareStatusEnum {

    /**
     * 已取消
     */
    CANCELLED(0, "已取消"),

    /**
     * 有效
     */
    ACTIVE(1, "有效"),

    /**
     * 已过期
     */
    EXPIRED(2, "已过期");

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
     * @return 分享状态枚举
     */
    public static ShareStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ShareStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断分享是否有效
     *
     * @return 是否有效
     */
    public boolean isValid() {
        return this == ACTIVE;
    }
}

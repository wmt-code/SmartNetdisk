package com.wmt.smartnetdisk.common.constant;

/**
 * 系统常量定义
 *
 * @author wmt
 * @since 1.0.0
 */
public final class SystemConstants {

    private SystemConstants() {
        throw new IllegalStateException("常量类不允许实例化");
    }

    // ==================== HTTP 相关 ====================
    /**
     * 请求头 - Authorization
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * 请求头 - Token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 请求头 - 用户代理
     */
    public static final String HEADER_USER_AGENT = "User-Agent";

    // ==================== 分页默认值 ====================
    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页大小
     */
    public static final int MAX_PAGE_SIZE = 100;

    // ==================== 逻辑删除 ====================
    /**
     * 未删除标识
     */
    public static final int NOT_DELETED = 0;

    /**
     * 已删除标识
     */
    public static final int DELETED = 1;

    // ==================== 状态标识 ====================
    /**
     * 启用状态
     */
    public static final int STATUS_ENABLE = 1;

    /**
     * 禁用状态
     */
    public static final int STATUS_DISABLE = 0;

    // ==================== 是否标识 ====================
    /**
     * 是
     */
    public static final int YES = 1;

    /**
     * 否
     */
    public static final int NO = 0;

    // ==================== 时间相关 ====================
    /**
     * 一分钟（秒）
     */
    public static final long ONE_MINUTE_SECONDS = 60L;

    /**
     * 一小时（秒）
     */
    public static final long ONE_HOUR_SECONDS = 3600L;

    /**
     * 一天（秒）
     */
    public static final long ONE_DAY_SECONDS = 86400L;

    /**
     * 一周（秒）
     */
    public static final long ONE_WEEK_SECONDS = 604800L;

    // ==================== 编码格式 ====================
    /**
     * UTF-8 编码
     */
    public static final String CHARSET_UTF8 = "UTF-8";

    // ==================== 日期格式 ====================
    /**
     * 日期格式 - 年月日
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 日期时间格式 - 年月日时分秒
     */
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 时间格式 - 时分秒
     */
    public static final String TIME_FORMAT = "HH:mm:ss";
}

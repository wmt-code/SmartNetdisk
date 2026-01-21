package com.wmt.smartnetdisk.common.constant;

/**
 * Redis Key 常量定义
 * <p>
 * 命名规范：业务模块:具体功能:唯一标识
 * </p>
 *
 * @author wmt
 * @since 1.0.0
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
        throw new IllegalStateException("常量类不允许实例化");
    }

    /**
     * Key 分隔符
     */
    public static final String KEY_SEPARATOR = ":";

    /**
     * 项目前缀
     */
    public static final String PROJECT_PREFIX = "smartnetdisk";

    // ==================== 用户相关 ====================
    /**
     * 用户信息缓存 - user:info:{userId}
     */
    public static final String USER_INFO = PROJECT_PREFIX + ":user:info:";

    /**
     * 用户 Token - user:token:{token}
     */
    public static final String USER_TOKEN = PROJECT_PREFIX + ":user:token:";

    /**
     * 用户会话 - user:session:{sessionId}
     */
    public static final String USER_SESSION = PROJECT_PREFIX + ":user:session:";

    // ==================== 验证码相关 ====================
    /**
     * 图形验证码 - captcha:image:{uuid}
     */
    public static final String CAPTCHA_IMAGE = PROJECT_PREFIX + ":captcha:image:";

    /**
     * 短信验证码 - captcha:sms:{phone}
     */
    public static final String CAPTCHA_SMS = PROJECT_PREFIX + ":captcha:sms:";

    /**
     * 邮箱验证码 - captcha:email:{email}
     */
    public static final String CAPTCHA_EMAIL = PROJECT_PREFIX + ":captcha:email:";

    // ==================== 文件上传相关 ====================
    /**
     * 分片上传进度 - file:chunk:{fileMd5}
     */
    public static final String FILE_CHUNK_PROGRESS = PROJECT_PREFIX + ":file:chunk:";

    /**
     * 秒传检测 - file:md5:{fileMd5}
     */
    public static final String FILE_MD5 = PROJECT_PREFIX + ":file:md5:";

    // ==================== 限流相关 ====================
    /**
     * 接口限流 - ratelimit:api:{apiPath}:{ip}
     */
    public static final String RATE_LIMIT_API = PROJECT_PREFIX + ":ratelimit:api:";

    /**
     * 登录限流 - ratelimit:login:{ip}
     */
    public static final String RATE_LIMIT_LOGIN = PROJECT_PREFIX + ":ratelimit:login:";

    // ==================== 锁相关 ====================
    /**
     * 分布式锁前缀 - lock:{业务}:{id}
     */
    public static final String LOCK_PREFIX = PROJECT_PREFIX + ":lock:";

    // ==================== 分享相关 ====================
    /**
     * 分享信息缓存 - share:info:{shareCode}
     */
    public static final String SHARE_INFO = PROJECT_PREFIX + ":share:info:";

    /**
     * 分享验证 - share:verify:{shareCode}
     */
    public static final String SHARE_VERIFY = PROJECT_PREFIX + ":share:verify:";

    // ==================== 缓存过期时间（秒） ====================
    /**
     * 用户信息缓存过期时间：30分钟
     */
    public static final long USER_INFO_EXPIRE = 1800L;

    /**
     * Token 过期时间：7天
     */
    public static final long TOKEN_EXPIRE = 604800L;

    /**
     * 验证码过期时间：5分钟
     */
    public static final long CAPTCHA_EXPIRE = 300L;

    /**
     * 限流窗口时间：1分钟
     */
    public static final long RATE_LIMIT_WINDOW = 60L;

    /**
     * 分片上传进度过期时间：24小时
     */
    public static final long CHUNK_PROGRESS_EXPIRE = 86400L;

    /**
     * 分享信息缓存过期时间：1小时
     */
    public static final long SHARE_INFO_EXPIRE = 3600L;
}

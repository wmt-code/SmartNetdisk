package com.wmt.smartnetdisk.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author wmt
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ==================== 成功 ====================
    SUCCESS(200, "操作成功"),

    // ==================== 客户端错误 4xx ====================
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    REQUEST_TIMEOUT(408, "请求超时"),
    CONFLICT(409, "资源冲突"),
    PARAM_VALID_ERROR(422, "参数校验失败"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    // ==================== 服务端错误 5xx ====================
    FAIL(500, "操作失败"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // ==================== 业务错误 1xxx ====================
    USER_NOT_EXIST(1001, "用户不存在"),
    USER_ALREADY_EXIST(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    TOKEN_INVALID(1004, "Token 无效或已过期"),
    TOKEN_EXPIRED(1005, "Token 已过期"),
    ACCOUNT_DISABLED(1006, "账号已被禁用"),
    ACCOUNT_LOCKED(1007, "账号已被锁定"),
    CAPTCHA_ERROR(1008, "验证码错误"),
    CAPTCHA_EXPIRED(1009, "验证码已过期"),

    // ==================== 文件错误 2xxx ====================
    FILE_NOT_FOUND(2001, "文件不存在"),
    FILE_UPLOAD_FAIL(2002, "文件上传失败"),
    FILE_DELETE_FAIL(2003, "文件删除失败"),
    FILE_TYPE_NOT_ALLOWED(2004, "文件类型不允许"),
    FILE_SIZE_EXCEED(2005, "文件大小超出限制"),
    FILE_MD5_NOT_MATCH(2006, "文件MD5校验失败"),
    CHUNK_UPLOAD_FAIL(2007, "分片上传失败"),
    CHUNK_MERGE_FAIL(2008, "分片合并失败"),
    FOLDER_NOT_FOUND(2009, "文件夹不存在"),
    FOLDER_NAME_DUPLICATE(2010, "文件夹名称重复"),

    // ==================== 分享错误 3xxx ====================
    SHARE_NOT_FOUND(3001, "分享不存在"),
    SHARE_EXPIRED(3002, "分享已过期"),
    SHARE_PASSWORD_ERROR(3003, "提取码错误"),
    SHARE_VIEW_LIMIT_EXCEED(3004, "访问次数已达上限"),
    SHARE_CANCELLED(3005, "分享已取消"),

    // ==================== AI 错误 4xxx ====================
    AI_SERVICE_ERROR(4001, "AI服务异常"),
    AI_VECTORIZE_FAIL(4002, "文档向量化失败"),
    AI_SEARCH_FAIL(4003, "语义搜索失败"),
    AI_CHAT_FAIL(4004, "智能问答失败"),
    DOCUMENT_PARSE_FAIL(4005, "文档解析失败"),
    DOCUMENT_NOT_VECTORIZED(4006, "文档尚未向量化"),

    // ==================== 数据库错误 5xxx ====================
    DATA_NOT_FOUND(5001, "数据不存在"),
    DATA_ALREADY_EXIST(5002, "数据已存在"),
    DATA_SAVE_FAIL(5003, "数据保存失败"),
    DATA_UPDATE_FAIL(5004, "数据更新失败"),
    DATA_DELETE_FAIL(5005, "数据删除失败");

    /**
     * 响应码
     */
    private final Integer code;

    /**
     * 响应消息
     */
    private final String message;
}

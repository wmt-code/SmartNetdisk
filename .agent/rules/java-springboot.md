---
trigger: always_on
glob: "**/*.java"
description: Java Spring Boot 项目开发规范 - 基于阿里巴巴Java开发规范(嵩山版)
---

# Java Spring Boot 项目开发规范

> 本规范基于**阿里巴巴Java开发规范(嵩山版)**，适用于使用 JDK 21、Spring Boot、MySQL、Redis、MyBatis-Plus 技术栈的项目。

## 一、项目技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | 使用 LTS 版本 |
| Spring Boot | 4.x | 核心框架 |
| MyBatis-Plus | 3.5.x | ORM 框架 |
| MySQL | 8.x | 关系型数据库 |
| Redis | 7.x | 缓存数据库 |
| Lombok | 最新版 | 简化代码 |

---

## 二、项目包结构规范

```
com.{公司名}.{项目名}
├── config/              # 配置类
├── controller/          # 控制器层
├── service/             # 业务逻辑层
│   └── impl/           # 业务实现类
├── mapper/              # MyBatis Mapper 接口
├── entity/              # 数据库实体类
├── dto/                 # 数据传输对象
├── vo/                  # 视图对象(返回给前端)
├── common/              # 通用类
│   ├── constant/       # 常量定义
│   ├── enums/          # 枚举类
│   ├── exception/      # 自定义异常
│   └── result/         # 统一响应结果
├── utils/               # 工具类
├── interceptor/         # 拦截器
├── filter/              # 过滤器
└── aspect/              # 切面类
```

---

## 三、命名规范

### 3.1 包名规范
- **全部小写**，不使用下划线
- 使用有意义的单词，如 `com.wmt.smartnetdisk`

### 3.2 类名规范
- 使用 **UpperCamelCase** 风格
- 抽象类以 `Abstract` 或 `Base` 开头
- 异常类以 `Exception` 结尾
- 测试类以被测试类名开头，以 `Test` 结尾
- 实现类以 `Impl` 结尾

| 类型 | 命名规范 | 示例 |
|------|----------|------|
| 实体类 | 与表名对应，UpperCamelCase | `User`、`FileInfo` |
| DTO | 实体名 + DTO | `UserDTO`、`FileInfoDTO` |
| VO | 实体名 + VO | `UserVO`、`FileInfoVO` |
| Controller | 实体名 + Controller | `UserController` |
| Service 接口 | I + 实体名 + Service | `IUserService` |
| Service 实现 | 实体名 + ServiceImpl | `UserServiceImpl` |
| Mapper | 实体名 + Mapper | `UserMapper` |

### 3.3 方法名规范
- 使用 **lowerCamelCase** 风格
- 获取单个对象用 `get` 前缀
- 获取多个对象用 `list` 前缀
- 获取统计值用 `count` 前缀
- 插入用 `save` 或 `insert` 前缀
- 删除用 `remove` 或 `delete` 前缀
- 修改用 `update` 前缀
- 查询用 `get`、`list`、`find` 前缀

### 3.4 变量名规范
- 使用 **lowerCamelCase** 风格
- 禁止使用拼音命名
- 布尔类型变量不要加 `is` 前缀（避免序列化问题）
- 常量使用 **UPPER_SNAKE_CASE**

---

## 四、通用类定义

### 4.1 统一响应结果类 (Result.java)

```java
package com.wmt.smartnetdisk.common.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果封装类
 *
 * @param <T> 响应数据类型
 * @author your_name
 * @since 1.0.0
 */
@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        return result;
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = success();
        result.setData(data);
        return result;
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = success(data);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> fail() {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.FAIL.getCode());
        result.setMessage(ResultCode.FAIL.getMessage());
        return result;
    }

    /**
     * 失败响应（带消息）
     */
    public static <T> Result<T> fail(String message) {
        Result<T> result = fail();
        result.setMessage(message);
        return result;
    }

    /**
     * 失败响应（自定义错误码）
     */
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败响应（使用 ResultCode）
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }
}
```

### 4.2 响应码枚举 (ResultCode.java)

```java
package com.wmt.smartnetdisk.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author your_name
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

    // ==================== 文件错误 2xxx ====================
    FILE_NOT_FOUND(2001, "文件不存在"),
    FILE_UPLOAD_FAIL(2002, "文件上传失败"),
    FILE_DELETE_FAIL(2003, "文件删除失败"),
    FILE_TYPE_NOT_ALLOWED(2004, "文件类型不允许"),
    FILE_SIZE_EXCEED(2005, "文件大小超出限制"),

    // ==================== 数据库错误 3xxx ====================
    DATA_NOT_FOUND(3001, "数据不存在"),
    DATA_ALREADY_EXIST(3002, "数据已存在"),
    DATA_SAVE_FAIL(3003, "数据保存失败"),
    DATA_UPDATE_FAIL(3004, "数据更新失败"),
    DATA_DELETE_FAIL(3005, "数据删除失败");

    /**
     * 响应码
     */
    private final Integer code;

    /**
     * 响应消息
     */
    private final String message;
}
```

### 4.3 分页查询请求 (PageRequest.java)

```java
package com.wmt.smartnetdisk.common.result;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页查询请求基类
 *
 * @author your_name
 * @since 1.0.0
 */
@Data
public class PageRequest {

    /**
     * 当前页码，默认第1页
     */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /**
     * 每页大小，默认10条
     */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer pageSize = 10;

    /**
     * 排序字段
     */
    private String orderBy;

    /**
     * 是否升序，默认降序
     */
    private Boolean isAsc = false;
}
```

### 4.4 分页响应结果 (PageResult.java)

```java
package com.wmt.smartnetdisk.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果封装类
 *
 * @param <T> 数据类型
 * @author your_name
 * @since 1.0.0
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private Long pageNum;

    /**
     * 每页大小
     */
    private Long pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 从 MyBatis-Plus IPage 构建分页结果
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setRecords(page.getRecords());
        return result;
    }

    /**
     * 手动构建分页结果
     */
    public static <T> PageResult<T> of(Long pageNum, Long pageSize, Long total, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setPages((total + pageSize - 1) / pageSize);
        result.setRecords(records);
        return result;
    }
}
```

---

## 五、通用常量定义

### 5.1 系统常量 (SystemConstants.java)

```java
package com.wmt.smartnetdisk.common.constant;

/**
 * 系统常量定义
 *
 * @author your_name
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
```

### 5.2 Redis Key 常量 (RedisKeyConstants.java)

```java
package com.wmt.smartnetdisk.common.constant;

/**
 * Redis Key 常量定义
 * <p>
 * 命名规范：业务模块:具体功能:唯一标识
 * </p>
 *
 * @author your_name
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
    public static final String PROJECT_PREFIX = "filestorage";

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
}
```

---

## 六、通用异常定义

### 6.1 业务异常 (BusinessException.java)

```java
package com.wmt.smartnetdisk.common.exception;

import com.wmt.smartnetdisk.common.result.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常
 *
 * @author your_name
 * @since 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
        this.message = message;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.FAIL.getCode();
        this.message = message;
    }
}
```

### 6.2 全局异常处理器 (GlobalExceptionHandler.java)

```java
package com.wmt.smartnetdisk.common.exception;

import com.wmt.smartnetdisk.common.result.Result;
import com.wmt.smartnetdisk.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author your_name
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 - URI: {}, Code: {}, Message: {}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.fail(ResultCode.PARAM_VALID_ERROR.getCode(), message);
    }

    /**
     * 处理参数校验异常（@RequestParam）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.fail(ResultCode.PARAM_VALID_ERROR.getCode(), message);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return Result.fail(ResultCode.PARAM_VALID_ERROR.getCode(), message);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "缺少请求参数: " + e.getParameterName());
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        return Result.fail(ResultCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 处理资源不存在异常
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 - URI: {}", request.getRequestURI(), e);
        return Result.fail(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
```

---

## 七、MyBatis-Plus 配置

### 7.1 MyBatis-Plus 配置类

```java
package com.wmt.smartnetdisk.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置类
 *
 * @author your_name
 * @since 1.0.0
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 插件配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 防止全表更新与删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    /**
     * 自动填充处理器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                // 创建时间
                this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
                // 更新时间
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
                // 逻辑删除标识
                this.strictInsertFill(metaObject, "deleted", () -> 0, Integer.class);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                // 更新时间
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
            }
        };
    }
}
```

### 7.2 实体基类 (BaseEntity.java)

```java
package com.wmt.smartnetdisk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类
 * <p>
 * 包含通用字段：主键ID、创建时间、更新时间、逻辑删除标识
 * </p>
 *
 * @author your_name
 * @since 1.0.0
 */
@Data
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标识（0-未删除，1-已删除）
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
```

---

## 八、Redis 配置

### 8.1 Redis 配置类

```java
package com.wmt.smartnetdisk.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * @author your_name
 * @since 1.0.0
 */
@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 配置 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        // 支持 Java 8 日期时间类型
        objectMapper.registerModule(new JavaTimeModule());

        // JSON 序列化器
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // String 序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Key 使用 String 序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 使用 JSON 序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
```

---

## 九、工具类

### 9.1 断言工具类 (AssertUtils.java)

```java
package com.wmt.smartnetdisk.utils;

import com.wmt.smartnetdisk.common.exception.BusinessException;
import com.wmt.smartnetdisk.common.result.ResultCode;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * 断言工具类
 * <p>
 * 用于参数校验，校验失败时抛出业务异常
 * </p>
 *
 * @author your_name
 * @since 1.0.0
 */
public final class AssertUtils {

    private AssertUtils() {
        throw new IllegalStateException("工具类不允许实例化");
    }

    /**
     * 断言对象不为空
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言对象不为空（使用 ResultCode）
     */
    public static void notNull(Object object, ResultCode resultCode) {
        if (object == null) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * 断言字符串不为空
     */
    public static void notEmpty(String str, String message) {
        if (!StringUtils.hasText(str)) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言集合不为空
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言条件为真
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言条件为真（使用 ResultCode）
     */
    public static void isTrue(boolean expression, ResultCode resultCode) {
        if (!expression) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * 断言条件为假
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言两个对象相等
     */
    public static void equals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw new BusinessException(message);
        }
    }
}
```

---

## 十、代码规范要求

### 10.1 注释规范

1. **类注释**：所有类必须添加 Javadoc 注释，包含功能说明、作者、版本
2. **方法注释**：公共方法必须添加 Javadoc 注释，说明功能、参数、返回值
3. **行内注释**：使用中文注释，简洁明了
4. **注释模板**：

```java
/**
 * 类功能描述
 *
 * @author 作者名
 * @since 版本号
 */
public class ClassName {

    /**
     * 方法功能描述
     *
     * @param paramName 参数说明
     * @return 返回值说明
     */
    public ReturnType methodName(ParamType paramName) {
        // 行内注释
    }
}
```

### 10.2 日志规范

1. 使用 **Lombok @Slf4j** 注解
2. 日志级别使用规范：
   - `ERROR`：系统错误，需要立即处理
   - `WARN`：警告信息，需要关注
   - `INFO`：重要业务信息
   - `DEBUG`：调试信息
3. 日志格式：使用占位符 `{}`，不使用字符串拼接
4. 敏感信息脱敏处理

```java
// 正确示例
log.info("用户登录成功, userId: {}", userId);
log.error("文件上传失败, fileName: {}", fileName, e);

// 错误示例
log.info("用户登录成功, userId: " + userId);
```

### 10.3 Controller 层规范

1. 使用 **RESTful** 风格 API
2. 统一使用 `@RestController`
3. 请求映射使用 `@GetMapping`、`@PostMapping` 等
4. 参数校验使用 `@Valid` 或 `@Validated`
5. 返回统一使用 `Result<T>` 包装

```java
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理")
public class UserController {

    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建用户")
    public Result<Long> create(@Valid @RequestBody UserDTO dto) {
        return Result.success(userService.create(dto));
    }
}
```

### 10.4 Service 层规范

1. 接口以 `I` 开头，实现类以 `Impl` 结尾
2. 继承 MyBatis-Plus 的 `IService` 接口
3. 业务逻辑必须在 Service 层处理
4. 事务使用 `@Transactional` 注解

```java
public interface IUserService extends IService<User> {
    UserVO getById(Long id);
    Long create(UserDTO dto);
}

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    // 实现方法
}
```

### 10.5 Mapper 层规范

1. 继承 MyBatis-Plus 的 `BaseMapper`
2. 添加 `@Mapper` 注解
3. 自定义 SQL 使用 XML 或 `@Select` 等注解

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 自定义方法
}
```

---

## 十一、数据库规范

### 11.1 表设计规范

1. 表名使用小写字母，单词间用下划线分隔
2. 必须包含以下字段：
   - `id` - 主键（BIGINT，雪花算法）
   - `create_time` - 创建时间（DATETIME）
   - `update_time` - 更新时间（DATETIME）
   - `deleted` - 逻辑删除标识（TINYINT，默认0）
3. 字段名使用小写字母，单词间用下划线分隔
4. 索引命名：`idx_字段名`，唯一索引：`uk_字段名`

### 11.2 SQL 规范

1. 禁止使用 `SELECT *`
2. 必须使用参数化查询，防止 SQL 注入
3. 大数据量操作使用分批处理
4. 避免在 WHERE 子句中使用函数

---

## 十二、安全规范

1. **敏感信息加密**：密码使用 BCrypt 加密存储
2. **SQL 注入防护**：使用 MyBatis-Plus 预编译 SQL
3. **XSS 防护**：对用户输入进行过滤转义
4. **CSRF 防护**：使用 Token 验证
5. **接口限流**：防止恶意请求
6. **日志脱敏**：敏感信息（手机号、身份证等）脱敏处理

---

## 十三、版本管理规范

1. 使用 **Git** 进行版本管理
2. 分支命名规范：
   - `main`/`master`：主分支
   - `develop`：开发分支
   - `feature/功能名`：功能分支
   - `bugfix/问题描述`：修复分支
   - `release/版本号`：发布分支
3. 提交信息规范：
   - `feat: 新功能`
   - `fix: 修复Bug`
   - `docs: 文档更新`
   - `refactor: 代码重构`
   - `test: 测试相关`
   - `chore: 构建/工具相关`

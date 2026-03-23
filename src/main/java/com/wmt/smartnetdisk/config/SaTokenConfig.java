package com.wmt.smartnetdisk.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 * <p>
 * 配置路由拦截，设置需要登录才能访问的接口
 * </p>
 *
 * @author wmt
 * @since 1.0.0
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册 Sa-Token 拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 管理员接口：需要 admin 角色
            SaRouter.match("/admin/**").check(r -> StpUtil.checkRole("admin"));

            // 路由拦截规则
            SaRouter
                    // 拦截所有路由
                    .match("/**")
                    // 排除不需要登录的接口
                    .notMatch(
                            // 认证相关接口
                            "/auth/register",
                            "/auth/login",
                            "/auth/check",
                            // 分享访问接口（公开）
                            "/s/**",
                            // 静态资源
                            "/static/**",
                            "/favicon.ico",
                            // Swagger/OpenAPI
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/doc.html",
                            // 健康检查
                            "/actuator/**")
                    // 执行登录校验
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}

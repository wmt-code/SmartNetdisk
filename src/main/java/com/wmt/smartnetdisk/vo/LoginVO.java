package com.wmt.smartnetdisk.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录响应视图对象
 *
 * @author wmt
 * @since 1.0.0
 */
@Data
public class LoginVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录 Token
     */
    private String token;

    /**
     * Token 名称（用于请求头）
     */
    private String tokenName;

    /**
     * 用户信息
     */
    private UserVO userInfo;

    /**
     * 创建登录响应
     *
     * @param token     登录 Token
     * @param tokenName Token 名称
     * @param userInfo  用户信息
     * @return 登录响应对象
     */
    public static LoginVO of(String token, String tokenName, UserVO userInfo) {
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setTokenName(tokenName);
        vo.setUserInfo(userInfo);
        return vo;
    }
}

package com.caiya.session.test.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;

/**
 * cookie常量类.
 *
 * @author wangnan
 * @since 1.0
 */
public class CookieConstant {

    /**
     * 会话客户端标识
     */
    public static final String COOKIE_4_SESSION_NAME = "auth";

    /**
     * 图片验证码客户端标识
     */
    public static final String COOKIE_4_CAPTCHA_NAME = "mmcc";

    /**
     * 根域名
     */
    public static final String BASE_DOMAIN = "test.com";

    /**
     * 当前域名
     */
    public static final String CURRENT_DOMAIN = "www.test.com";

    /**
     * 默认的cookie的session的存货时间，默认为7天
     */
    public static final Duration DEFAULT_COOKIE_SESSION_AGE = Duration.ofDays(7);

    private static String getCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (null == cookies || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 获取用户会话的客户端标识
     */
    public static String getSessionId(HttpServletRequest request) {
        return getCookie(request, COOKIE_4_SESSION_NAME);
    }

    /**
     * 获取验证码的客户端标识
     */
    public static String getCaptchaId(HttpServletRequest request) {
        return getCookie(request, COOKIE_4_CAPTCHA_NAME);
    }


}

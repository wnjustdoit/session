package com.caiya.session.test.controller;

import com.caiya.session.Session;
import com.caiya.session.test.component.UserSessionHolder;
import com.caiya.session.test.pojo.User;
import com.caiya.session.test.util.CookieConstant;
import com.caiya.session.test.util.SessionConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BaseController {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private UserSessionHolder userSessionHolder;

    protected Session getSession(boolean create) {
        Session session = userSessionHolder.getSession(CookieConstant.getSessionId(getHttpServletRequest()));
        if (session == null && create) {
            session = userSessionHolder.createSession();
        }
        if (session != null) {
            // 设置客户端cookie
            getHttpServletResponse().setHeader("Set-Cookie", CookieConstant.COOKIE_4_SESSION_NAME + "=" + session.getId()
                    + ";Path=/"
                    // 设置是否写在根域名下，共享cookie
                    // + ";Domain=" + CookieConstant.BASE_DOMAIN
                    + ";HTTPOnly");
        }
        return session;
    }

    protected User getUser() {
        Session session = getSession(false);
        if (session == null) {
            return null;
        }
        Object user = session.getAttribute(SessionConstant.USER);
        if (user == null) {
            return null;
        }
        return (User) user;
    }

    private static HttpServletRequest getHttpServletRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    private static HttpServletResponse getHttpServletResponse() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
    }

}

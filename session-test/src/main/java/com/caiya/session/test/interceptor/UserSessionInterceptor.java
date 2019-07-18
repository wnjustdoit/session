package com.caiya.session.test.interceptor;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户会话拦截器，拦截器主要检测请求参数中是否有会话数据.
 *
 * @author wangnan
 * @since 1.0
 */
public class UserSessionInterceptor extends HandlerInterceptorAdapter {

    private static final Logger logger = LoggerFactory.getLogger(UserSessionInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            logger.error("lose Authentication!" + ", handler:{}", handler);
            response.setContentType("text/html;charset=utf-8");
            Map<String, Object> result = new HashMap<>();
            result.put("result", 8);
            result.put("data", new Object());
            result.put("message", "用户会话失效！");
            String error = JSON.toJSONString(result);
            if (StringUtils.isNotBlank(request.getParameter("callback"))) {
                error = request.getParameter("callback") + "(" + error + ")";
            }

            PrintWriter writer = null;
            try {
                writer = response.getWriter();
                writer.print(error);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                try {
                    if (writer != null) {
                        writer.flush();
                        writer.close();
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            return false;
        }

        return true;
    }
}

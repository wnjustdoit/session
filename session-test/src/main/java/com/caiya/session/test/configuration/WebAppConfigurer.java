package com.caiya.session.test.configuration;

import com.caiya.cache.redis.spring.RedisCache;
import com.caiya.session.SessionManager;
import com.caiya.session.redis.RedisSession;
import com.caiya.session.redis.RedisSessionManager;
import com.caiya.session.test.util.Constant;
import com.caiya.session.web.http.CookieHttpSessionIdResolver;
import com.caiya.session.web.http.DefaultCookieSerializer;
import com.caiya.session.web.http.SessionRepositoryFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.Resource;
import java.time.Duration;

/**
 * web相关配置.
 *
 * @author wangnan
 * @since 1.0
 */
@Configuration
public class WebAppConfigurer extends WebMvcConfigurerAdapter {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private SessionManager<RedisSession> sessionManager() {
        RedisSessionManager sessionManager = new RedisSessionManager(new RedisCache<>(redisTemplate));
        sessionManager.setDefaultMaxInactiveInterval(Duration.ofHours(2));
        sessionManager.setRedisKeyNamespace(Constant.DEFAULT_SESSION_NAMESPACE);
        return sessionManager;
    }

    @Bean
    public FilterRegistrationBean sessionRepositoryFilter() {
        SessionRepositoryFilter<RedisSession> sessionRepositoryFilter = new SessionRepositoryFilter<>(sessionManager());
        CookieHttpSessionIdResolver httpSessionIdResolver = new CookieHttpSessionIdResolver();
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        cookieSerializer.setCookieName(Constant.DEFAULT_COOKIE_NAME);
        httpSessionIdResolver.setCookieSerializer(cookieSerializer);
        sessionRepositoryFilter.setHttpSessionIdResolver(httpSessionIdResolver);
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        // 一般必须放在所有的filter的首位
        filterRegistrationBean.setFilter(sessionRepositoryFilter);
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.setOrder(SessionRepositoryFilter.DEFAULT_ORDER);
        return filterRegistrationBean;
    }


}

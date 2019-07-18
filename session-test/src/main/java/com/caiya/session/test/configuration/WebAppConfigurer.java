package com.caiya.session.test.configuration;

import com.caiya.cache.redis.spring.RedisCache;
import com.caiya.session.SessionManager;
import com.caiya.session.redis.RedisSession;
import com.caiya.session.redis.RedisSessionManager;
import com.caiya.session.test.interceptor.UserSessionInterceptor;
import com.caiya.session.test.util.CacheConstant;
import com.caiya.session.test.util.CookieConstant;
import com.caiya.session.test.util.SessionConstant;
import com.caiya.session.web.http.CookieHttpSessionIdResolver;
import com.caiya.session.web.http.DefaultCookieSerializer;
import com.caiya.session.web.http.SessionRepositoryFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserSessionInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/test", "/error", "/user/login", "/user/logout");
    }

    private SessionManager<RedisSession> sessionManager() {
        RedisSessionManager sessionManager = new RedisSessionManager(new RedisCache<>(CacheConstant.DEFAULT_CACHE_NAME, (CacheConstant.DEFAULT_CACHE_NAME + ":").getBytes(StandardCharsets.UTF_8), redisTemplate));
        sessionManager.setDefaultMaxInactiveInterval(SessionConstant.DEFAULT_EXPIRATION);
        sessionManager.setRedisKeyNamespace(SessionConstant.DEFAULT_SESSION_NAMESPACE);
        return sessionManager;
    }

    @Bean
    public FilterRegistrationBean sessionRepositoryFilter() {
        SessionRepositoryFilter<RedisSession> sessionRepositoryFilter = new SessionRepositoryFilter<>(sessionManager());
        CookieHttpSessionIdResolver httpSessionIdResolver = new CookieHttpSessionIdResolver();
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        // 设置cookie是否写在根域名下，以共享cookie
//        cookieSerializer.setDomainName(CookieConstant.BASE_DOMAIN);
        cookieSerializer.setCookieName(CookieConstant.COOKIE_4_SESSION_NAME);
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

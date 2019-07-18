package com.caiya.session.test.configuration;

import com.caiya.session.test.component.UserSessionHolder;
import com.caiya.session.test.interceptor.UserSessionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.Resource;

/**
 * web相关配置.
 *
 * @author wangnan
 * @since 1.0
 */
@Configuration
public class WebAppConfigurer extends WebMvcConfigurerAdapter {

    @Resource
    private UserSessionHolder userSessionHolder;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserSessionInterceptor(userSessionHolder))
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/error", "/test", "/user/login", "/user/logout");
    }


}

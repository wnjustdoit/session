package com.caiya.session.test.component;

import com.caiya.cache.redis.spring.RedisCache;
import com.caiya.session.Session;
import com.caiya.session.SessionException;
import com.caiya.session.SessionManager;
import com.caiya.session.redis.RedisSessionManager;
import com.caiya.session.test.util.CacheConstant;
import com.caiya.session.test.util.SessionConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Component
public class UserSessionHolder {

    private static final Logger logger = LoggerFactory.getLogger(UserSessionHolder.class);

    private final SessionManager sessionManager;

    @Autowired
    public UserSessionHolder(RedisTemplate<String, Object> redisTemplate) {
        sessionManager = new RedisSessionManager(new RedisCache<>(CacheConstant.DEFAULT_CACHE_NAME, (CacheConstant.DEFAULT_CACHE_NAME + ":").getBytes(StandardCharsets.UTF_8), redisTemplate));
        sessionManager.setDefaultMaxInactiveInterval(Duration.ofSeconds(SessionConstant.DEFAULT_EXPIRATION));
        ((RedisSessionManager) sessionManager).setRedisKeyNamespace(SessionConstant.DEFAULT_SESSION_NAMESPACE);
    }

    /**
     * 根据sessionId获取session
     *
     * @param sessionId sessionId
     * @return Session
     * @throws SessionException SessionException
     */
    public Session getSession(String sessionId) throws SessionException {
        if (sessionId == null || sessionId.trim().equals("")) {
            return null;
        }
        Session session = sessionManager.findById(sessionId);
        if (session != null) {
            // touch session
            session.setLastAccessedTime(Instant.now());
        }
        return session;
    }

    /**
     * 创建session
     *
     * @return Session
     * @throws SessionException SessionException
     */
    public Session createSession() throws SessionException {
        return sessionManager.createSession();
    }

}

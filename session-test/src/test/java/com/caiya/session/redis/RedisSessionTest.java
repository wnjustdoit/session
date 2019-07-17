package com.caiya.session.redis;

import com.caiya.cache.redis.spring.RedisCache;
import com.caiya.session.Session;
import com.caiya.session.SessionException;
import com.caiya.session.SessionManager;
import com.caiya.session.test.BaseTest;
import com.caiya.session.test.util.Constant;
import com.caiya.session.util.StandardSessionIdGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * RedisSessionTest.
 *
 * @author wangnan
 * @since 1.0
 */
public class RedisSessionTest extends BaseTest {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private SessionManager sessionManager;

    private Session session;

    private static String sessionId;

    private static Duration maxInactiveInterval = Duration.ofSeconds(Constant.DEFAULT_EXPIRATION);

    private static Instant lastAccessedTime;

    @Before
    public void before() throws SessionException {
        sessionManager = new RedisSessionManager(new RedisCache<>(redisTemplate));
        ((RedisSessionManager) sessionManager).setRedisKeyNamespace(Constant.DEFAULT_SESSION_NAMESPACE);
        session = sessionManager.createSession();
    }

    @After
    public void after() {

    }

    @Test
    public void test_A_New() throws InterruptedException {
        session = sessionManager.createSession();

        TimeUnit.SECONDS.sleep(1);// sleep 1 second

        // set first session id
        sessionId = session.getId();
        // set first accessed time
        lastAccessedTime = session.getLastAccessedTime();
        // set first maxInactiveInterval
        maxInactiveInterval = session.getMaxInactiveInterval();

        testCommon();
    }

    @Test
    public void test_B_Old() {
        session = sessionManager.findById(sessionId);

        assertEquals(sessionId, session.getId());

        testCommon();

        // other set operations
        String newId = new StandardSessionIdGenerator().generateSessionId();
        session.changeId(newId);
        assertEquals(session.getOriginalId(), sessionId);
        assertNotEquals(session.getId(), session.getOriginalId());
        assertEquals(session.getId(), newId);

        Instant now = Instant.now();
        session.setLastAccessedTime(now);
        assertNotEquals(lastAccessedTime, session.getLastAccessedTime());
        assertEquals(now, session.getLastAccessedTime());
        Duration duration = session.getIdleTime().minus(session.getMaxInactiveInterval());
        assertTrue(duration.getSeconds() <= 0 && duration.getSeconds() > -10);// nearly test

        session.setMaxInactiveInterval(Duration.ofHours(1));
        assertNotEquals(maxInactiveInterval, session.getMaxInactiveInterval());
        duration = session.getIdleTime().minus(session.getMaxInactiveInterval());
        assertTrue(duration.getSeconds() <= 0 && duration.getSeconds() > -10);// nearly test
    }

    private void testCommon() {
        // base info
        assertEquals(session.getId(), session.getOriginalId());

        assertEquals(session.getCreationTime(), session.getLastAccessedTime());
        long d1 = session.getCreationTime().getEpochSecond() - Instant.now().getEpochSecond();
        assertTrue(d1 <= 0 && d1 > -10);// nearly test

        assertEquals(maxInactiveInterval, session.getMaxInactiveInterval());

        Duration duration = session.getIdleTime().minus(session.getMaxInactiveInterval());
        assertTrue(duration.getSeconds() <= 0 && duration.getSeconds() > -10);// nearly test

        // attribute info
        assertEquals(0, session.getAttributeNames().size());

        String attrName = "user";
        User user = new User(10001L, "我是张三");

        session.setAttribute(attrName, user);
        assertEquals(user, session.getAttribute(attrName));

        assertEquals(1, session.getAttributeNames().size());
        assertTrue(session.getAttributeNames().contains(attrName));

        session.removeAttribute(attrName);
        assertNull(session.getAttribute(attrName));
        assertEquals(0, session.getAttributeNames().size());
    }

    public static class User implements Serializable {

        private static final long serialVersionUID = -8741794542106019386L;

        private Long id;

        private String name;

        User(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof User)) {
                return false;
            }

            User param = (User) obj;
            return Objects.equals(id, param.id) && Objects.equals(name, param.name);
        }
    }

    @Test
    public void test_C_Manager() throws InterruptedException {
        sessionManager.setDefaultMaxInactiveInterval(Duration.ofHours(3));
        test_A_New();
        assertEquals(session.getMaxInactiveInterval(), Duration.ofHours(3));
        test_B_Old();

        sessionManager.setSessionIdGenerator(new StandardSessionIdGenerator() {
            @Override
            public String generateSessionId(String route) {
                String routeSuffix = (route == null || route.isEmpty()) ? "" : route;
                return UUID.randomUUID().toString() + (routeSuffix);
            }
        });
        test_A_New();
        test_B_Old();

        assertNotNull(sessionManager.findById(session.getId()));
        sessionManager.deleteById(session.getId());
        assertNull(sessionManager.findById(session.getId()));

    }

}

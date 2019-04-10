package com.caiya.session.redis;

import com.caiya.cache.redis.JedisCache;
import com.caiya.serialization.jdk.JdkSerializationSerializer;
import com.caiya.serialization.jdk.StringSerializer;
import com.caiya.session.Session;

import static org.junit.Assert.*;

import com.caiya.session.SessionManager;
import com.caiya.session.redis.util.Constant;
import com.caiya.session.util.StandardSessionIdGenerator;
import org.junit.*;
import org.junit.runners.MethodSorters;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * RedisSessionTest.
 *
 * @author wangnan
 * @since 1.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RedisSessionTest {

    private SessionManager sessionManager;

    private Session session;

    private static String sessionId;

    private static Duration maxInactiveInterval = Duration.ofSeconds(Constant.DEFAULT_EXPIRATION);

    private static Instant lastAccessedTime;

    @Before
    public void before() {
        Set<HostAndPort> hostAndPorts = new HashSet<>();
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7000));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7001));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7002));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7003));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7004));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7005));
        JedisCluster jedisCluster = new JedisCluster(hostAndPorts);
        JedisCache<String, Object> cache = new JedisCache<>(jedisCluster);
        StringSerializer stringSerializer = new StringSerializer();
        JdkSerializationSerializer jdkSerializationSerializer = new JdkSerializationSerializer();
        cache.setKeySerializer(stringSerializer);
        cache.setValueSerializer(jdkSerializationSerializer);
        cache.setHashKeySerializer(stringSerializer);
        cache.setHashValueSerializer(jdkSerializationSerializer);
        cache.setKeyPrefix((Constant.DEFAULT_CACHE_NAME + ":").getBytes(StandardCharsets.UTF_8));
        sessionManager = new RedisSessionManager(cache);
        sessionManager.setDefaultMaxInactiveInterval(maxInactiveInterval);
        ((RedisSessionManager) sessionManager).setRedisKeyNamespace(Constant.DEFAULT_SESSION_NAMESPACE);
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
        assertFalse(maxInactiveInterval.equals(session.getMaxInactiveInterval()));
        duration = session.getIdleTime().minus(session.getMaxInactiveInterval());
        assertTrue(duration.getSeconds() <= 0 && duration.getSeconds() > -10);// nearly test
    }

    private void testCommon() {
        // base info
        assertEquals(session.getId(), session.getOriginalId());

        assertTrue(session.getCreationTime().equals(session.getLastAccessedTime()));
        long d1 = session.getCreationTime().getEpochSecond() - Instant.now().getEpochSecond();
        assertTrue(d1 <= 0 && d1 > -10);// nearly test

        assertTrue(maxInactiveInterval.equals(session.getMaxInactiveInterval()));

        Duration duration = session.getIdleTime().minus(session.getMaxInactiveInterval());
        assertTrue(duration.getSeconds() <= 0 && duration.getSeconds() > -10);// nearly test

        // attribute info
        assertTrue(session.getAttributeNames().size() == 0);

        String attrName = "user";
        User user = new User(10001L, "我是张三");

        session.setAttribute(attrName, user);
        assertTrue(user.equals(session.getAttribute(attrName)));

        assertTrue(session.getAttributeNames().size() == 1);
        assertTrue(session.getAttributeNames().contains(attrName));

        session.removeAttribute(attrName);
        assertNull(session.getAttribute(attrName));
        assertTrue(session.getAttributeNames().size() == 0);
    }

    public static class User implements Serializable {

        private static final long serialVersionUID = -8741794542106019386L;

        private Long id;

        private String name;

        public User(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof User)) {
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
        assertTrue(session.getMaxInactiveInterval().equals(Duration.ofHours(3)));
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

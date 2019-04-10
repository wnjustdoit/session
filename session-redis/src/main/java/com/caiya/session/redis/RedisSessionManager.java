package com.caiya.session.redis;

import com.caiya.cache.CacheApi;
import com.caiya.session.Session;
import com.caiya.session.SessionException;
import com.caiya.session.SessionIdGenerator;
import com.caiya.session.SessionManager;
import com.caiya.session.util.StandardSessionIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis Session Manager.
 *
 * @author wangnan
 * @since 1.0
 */
public class RedisSessionManager implements SessionManager<RedisSession> {

    protected static final Logger logger = LoggerFactory.getLogger(RedisSessionManager.class);

    /**
     * Default {@link #setDefaultMaxInactiveInterval(Duration)} (30 minutes).
     */
    public static final int DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS = 1800;

    /**
     * The default namespace for each key and channel in Redis used by User Session.
     */
    public static final String DEFAULT_NAMESPACE = "project:session";

    /**
     * The key in the Hash representing
     * {@link Session#getCreationTime()}.
     */
    static final String CREATION_TIME_ATTR = "creationTime";

    /**
     * The key in the Hash representing
     * {@link Session#getMaxInactiveInterval()}
     * .
     */
    static final String MAX_INACTIVE_ATTR = "maxInactiveInterval";

    /**
     * The key in the Hash representing
     * {@link Session#getLastAccessedTime()}.
     */
    static final String LAST_ACCESSED_ATTR = "lastAccessedTime";

    /**
     * The prefix of the key used for session attributes. The suffix is the name of
     * the session attribute. For example, if the session contained an attribute named
     * attributeName, then there would be an entry in the hash named
     * sessionAttr:attributeName that mapped to its value.
     */
    static final String SESSION_ATTR_PREFIX = "sessionAttr:";

    /**
     * The namespace for every key used by Spring Session in Redis.
     */
    private String namespace = DEFAULT_NAMESPACE + ":";

    private CacheApi<String, Object> sessionCache;

    /**
     * If non-null, this value is used to override the default value for
     * {@link RedisSession#setMaxInactiveInterval}.
     */
    private Duration defaultMaxInactiveInterval = Duration.ofSeconds(DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS);

    protected SessionIdGenerator sessionIdGenerator;

    protected Class<? extends SessionIdGenerator> sessionIdGeneratorClass;

    RedisSessionExpirationPolicy redisSessionExpirationPolicy;

    @SuppressWarnings("unused")
    public CacheApi getSessionCache() {
        return sessionCache;
    }

    @Override
    public void setDefaultMaxInactiveInterval(Duration defaultMaxInactiveInterval) {
        this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
    }

    public void setRedisKeyNamespace(String namespace) {
        if (namespace == null || namespace.trim().isEmpty())
            throw new IllegalArgumentException("namespace cannot be null or empty");

        this.namespace = namespace.trim() + ":";
    }

    /**
     * Gets the Hash key for this session by prefixing it appropriately.
     *
     * @param sessionId the session id
     * @return the Hash key for this session by prefixing it appropriately.
     */
    String getSessionKey(String sessionId) {
        return this.namespace + "sessions:" + sessionId;
    }

    String getExpiredKey(String sessionId) {
        return getExpiredKeyPrefix() + sessionId;
    }

    private String getExpiredKeyPrefix() {
        return this.namespace + "sessions:" + "expires:";
    }

    /**
     * Gets the key for the specified session attribute.
     *
     * @param attributeName the attribute name
     * @return the attribute key name
     */
    static String getSessionAttrNameKey(String attributeName) {
        return SESSION_ATTR_PREFIX + attributeName;
    }

    public RedisSessionManager(CacheApi<String, Object> sessionCache) {
        if (sessionCache == null)
            throw new IllegalArgumentException("sessionCache cannot be null");

        this.sessionCache = sessionCache;
        this.redisSessionExpirationPolicy = new RedisSessionExpirationPolicy();
    }

    @Override
    public RedisSession createSession(String sessionId, Duration maxInactiveInterval) {
        if (sessionId == null || sessionId.isEmpty())
            throw new IllegalArgumentException("session id can not be empty");
        // TODOs 限制最大会话数量

        return new RedisSession(sessionCache, sessionId, maxInactiveInterval, this);
    }

    @Override
    public RedisSession createSession(String sessionId) {
        return createSession(sessionId, defaultMaxInactiveInterval);
    }

    @Override
    public RedisSession createSession() {
        return createSession(generateSessionId());
    }

    @Override
    public RedisSession findById(String id) {
        if (sessionCache.ttl(getExpiredKey(id)) <= 0) {
            return null;
        }

        String key = getSessionKey(id);
        Map<String, Object> entries = sessionCache.hGetAll(key);
        if (entries.isEmpty()) {
            return null;
        }

        return new RedisSession(sessionCache, id, entries, this);
    }

    @Override
    public void deleteById(String id) {
        RedisSession session = findById(id);
        if (session == null) {
            return;
        }

        String expireKey = getExpiredKey(session.getId());
        this.sessionCache.del(expireKey);
    }

    @Override
    public SessionIdGenerator getSessionIdGenerator() {
        if (sessionIdGenerator != null) {
            return sessionIdGenerator;
        } else if (sessionIdGeneratorClass != null) {
            try {
                sessionIdGenerator = sessionIdGeneratorClass.newInstance();
                return sessionIdGenerator;
            } catch (IllegalAccessException | InstantiationException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        return null;
    }


    @Override
    public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
        this.sessionIdGenerator = sessionIdGenerator;
        sessionIdGeneratorClass = sessionIdGenerator.getClass();
    }

    @Override
    public void add(RedisSession session) {
        session.setManager(this);
        // TODOs may do anything else

    }

    @Override
    public String changeSessionId(RedisSession session) {
        int maxTryTimes = 0;
        do {
            maxTryTimes++;
            String newId = generateSessionId();
            if (changeSessionId(session, newId)) {
                return newId;
            }
        } while (maxTryTimes <= 3);

        // should seldom reach
        throw new IllegalStateException("unfortunately, this time does not generate a valid session id, may try again");
    }

    @Override
    public boolean changeSessionId(RedisSession session, String newId) {
        if (session == null)
            throw new IllegalArgumentException("session cannot be null");
        if (newId == null || newId.trim().isEmpty())
            throw new IllegalArgumentException("the new session id cannot be empty");

        // check if exists and change it, attention this is not thread-safe.
        boolean exists = sessionCache.exists(getSessionKey(newId));
        if (!exists) {
            session.changeId(newId);
            return true;
        }

        return false;
    }

    /**
     * Generate and return a new session identifier.
     *
     * @return a new session id
     */
    protected String generateSessionId() {
        String result = null;

        SessionIdGenerator sessionIdGenerator = getSessionIdGenerator();
        if (sessionIdGenerator == null) {
            sessionIdGenerator = new StandardSessionIdGenerator();
        }
        int tryTimes = 0;
        do {
            if (result != null) {
                // should rarely be here
                logger.warn("duplicated session id!!");
                if (++tryTimes >= 10) {
                    throw new SessionException("generate duplicated session id too many times!!");
                }
            }
            result = sessionIdGenerator.generateSessionId();
        } while (sessionCache.exists(result));

        return result;
    }

    public void saveChangeSessionId(String sessionId, String originalSessionId) {
        String originalExpiredKey = getExpiredKey(originalSessionId);
        String expiredKey = getExpiredKey(sessionId);
        sessionCache.rename(originalExpiredKey, expiredKey);
    }

    final class RedisSessionExpirationPolicy {

        public void onExpirationUpdated(Session session) {
            String sessionKey = getExpiredKey(session.getId());
            long sessionExpireInSeconds = session.getMaxInactiveInterval().getSeconds();
            long fiveMinutesAfterExpires = sessionExpireInSeconds + TimeUnit.MINUTES.toSeconds(5);
            if (sessionExpireInSeconds == 0) {
                sessionCache.del(sessionKey);
            } else {
                // define 30 mins
                sessionCache.append(sessionKey, "");
                sessionCache.expire(sessionKey, sessionExpireInSeconds);

                // define 35 mins
                sessionCache.expire(getSessionKey(session.getId()), fiveMinutesAfterExpires);
            }

        }

    }


}

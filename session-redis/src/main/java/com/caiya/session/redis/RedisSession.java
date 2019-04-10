package com.caiya.session.redis;

import com.caiya.cache.CacheApi;
import com.caiya.cache.RedisConstant;
import com.caiya.session.Session;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Redis Session implementation of the <b>Session</b> interface.
 *
 * @author wangnan
 * @since 1.0
 */
public class RedisSession implements Session<RedisSessionManager> {

    /**
     * Default {@link #setMaxInactiveInterval(Duration)} (30 minutes).
     */
    public static final int DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS = 1800;

    private String id;
    private String originalId;

    private Duration maxInactiveInterval = Duration.ofSeconds(DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS);
    private Instant creationTime = Instant.now();
    private Instant lastAccessedTime = this.creationTime;
    private Map<String, Object> sessionAttrs = new HashMap<>();

    private CacheApi<String, Object> sessionCache;

    private RedisSessionManager redisSessionManager;


    private RedisSession(CacheApi<String, Object> sessionCache, String id) {
        if (id == null || id.trim().isEmpty())
            throw new IllegalArgumentException("session id cannot be empty");

        this.sessionCache = sessionCache;
        this.id = id;
        this.originalId = id;
    }

    RedisSession(CacheApi<String, Object> sessionCache, String id, Duration maxInactiveInterval, RedisSessionManager sessionManager) {
        // set local value
        this.sessionCache = sessionCache;
        if (id != null && !id.trim().isEmpty()) {
            this.id = id;
        } else {
            this.id = generateId();
        }
        this.originalId = id;
        if (maxInactiveInterval != null) {
            this.maxInactiveInterval = maxInactiveInterval;
        }
        this.redisSessionManager = sessionManager;
        // set redis value
        Map<String, Object> delta = new HashMap<>();
        delta.put(RedisSessionManager.CREATION_TIME_ATTR, getCreationTime().toEpochMilli());
        delta.put(RedisSessionManager.MAX_INACTIVE_ATTR, getMaxInactiveInterval().getSeconds());
        delta.put(RedisSessionManager.LAST_ACCESSED_ATTR, getLastAccessedTime().toEpochMilli());
        saveDelta(delta);
    }

    RedisSession(CacheApi<String, Object> sessionCache, String id, Map<String, Object> entries, RedisSessionManager sessionManager) {
        this(sessionCache, id);

        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            String key = entry.getKey();
            if (RedisSessionManager.CREATION_TIME_ATTR.equals(key)) {
                this.creationTime = Instant.ofEpochMilli((long) entry.getValue());
            } else if (RedisSessionManager.MAX_INACTIVE_ATTR.equals(key)) {
                this.maxInactiveInterval = Duration.ofSeconds((long) entry.getValue());
            } else if (RedisSessionManager.LAST_ACCESSED_ATTR.equals(key)) {
                this.lastAccessedTime = Instant.ofEpochMilli((long) entry.getValue());
            } else if (key.startsWith(RedisSessionManager.SESSION_ATTR_PREFIX)) {
                this.sessionAttrs.put(key.substring(RedisSessionManager.SESSION_ATTR_PREFIX.length()),
                        entry.getValue());
            }
        }

        this.redisSessionManager = sessionManager;
    }

    private void saveDelta(Map<String, Object> delta) {
        if (delta != null && !delta.isEmpty()) {
            // hash set operation
            sessionCache.hMSet(redisSessionManager.getSessionKey(id), delta);
        }
        // expire operation
        redisSessionManager.redisSessionExpirationPolicy.onExpirationUpdated(this);
    }

    @Override
    public Instant getCreationTime() {
        return this.creationTime;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void changeId(String newId) {
        if (this.id.equals(newId))
            return;

        // set redis value
        String originalSessionIdKey = redisSessionManager.getSessionKey(this.id);
        String sessionIdKey = redisSessionManager.getSessionKey(newId);
        sessionCache.rename(originalSessionIdKey, sessionIdKey, RedisConstant.Operation.HASH);
        redisSessionManager.saveChangeSessionId(newId, this.id);
        // set local value
        this.id = newId;
    }

    @Override
    public String getOriginalId() {
        return this.originalId;
    }

    @Override
    public Instant getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    @Override
    public void setLastAccessedTime(Instant lastAccessedTime) {
        // set redis value
        Map<String, Object> delta = new HashMap<>();
        delta.put(RedisSessionManager.LAST_ACCESSED_ATTR, lastAccessedTime.toEpochMilli());
        saveDelta(delta);
        // set local value
        this.lastAccessedTime = lastAccessedTime;
    }

    @Override
    public Duration getIdleTime() {
        return Duration.ofSeconds(sessionCache.ttl(redisSessionManager.getExpiredKey(id)));
    }

    @Override
    public void setMaxInactiveInterval(Duration interval) {
        // set local value(first!)
        this.maxInactiveInterval = interval;
        // set redis value
        Map<String, Object> delta = new HashMap<>();
        delta.put(RedisSessionManager.MAX_INACTIVE_ATTR, interval.getSeconds());
        saveDelta(delta);
    }

    @Override
    public Duration getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name) {
        return (T) this.sessionAttrs.get(name);
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.sessionAttrs.keySet();
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value == null) {
            removeAttribute(name);
        } else {
            // set redis value
            Map<String, Object> delta = new HashMap<>();
            delta.put(RedisSessionManager.getSessionAttrNameKey(name), value);
            saveDelta(delta);
            // set local value
            this.sessionAttrs.put(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        // set redis value
        sessionCache.hDel(redisSessionManager.getSessionKey(id), RedisSessionManager.getSessionAttrNameKey(name));
        // set local value
        this.sessionAttrs.remove(name);
    }

    @Override
    public RedisSessionManager getManager() {
        return redisSessionManager;
    }

    @Override
    public void setManager(RedisSessionManager manager) {
        this.redisSessionManager = manager;
    }

    private static String generateId() {
        return UUID.randomUUID().toString();
    }
}

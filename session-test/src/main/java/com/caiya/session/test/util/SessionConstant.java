package com.caiya.session.test.util;

import java.time.Duration;

/**
 * 会话常量类.
 *
 * @author wangnan
 * @since 1.0
 */
public class SessionConstant {

    /**
     * 会话默认的命名空间
     */
    public static final String DEFAULT_SESSION_NAMESPACE = "session";

    /**
     * 会话默认有效时间
     */
    public static final Duration DEFAULT_EXPIRATION = Duration.ofHours(2);

    /**
     * 会话属性用户标识
     */
    public static final String USER = "user";


}

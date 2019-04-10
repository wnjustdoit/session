package com.caiya.session;

/**
 * Session Id Generator Interface.
 *
 * @author wangnan
 * @since 1.0
 */
public interface SessionIdGenerator {

    /**
     * @return the node identifier associated with this node which will be
     * included in the generated session ID.
     */
    String getJvmRoute();

    /**
     * Specify the node identifier associated with this node which will be
     * included in the generated session ID.
     *
     * @param jvmRoute The node identifier
     */
    void setJvmRoute(String jvmRoute);

    /**
     * @return the number of bytes for a session ID
     */
    int getSessionIdLength();

    /**
     * Specify the number of bytes for a session ID
     *
     * @param sessionIdLength Number of bytes
     */
    void setSessionIdLength(int sessionIdLength);

    /**
     * Generate and return a new session identifier.
     *
     * @return the newly generated session id
     */
    String generateSessionId();

    /**
     * Generate and return a new session identifier.
     *
     * @param route node identifier to include in generated id
     * @return the newly generated session id
     */
    String generateSessionId(String route);
}

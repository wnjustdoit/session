package com.caiya.session;

import java.time.Duration;

/**
 * Session Manager Interface.
 */
public interface SessionManager<S extends Session> {

    /**
     * @return the session id generator
     */
    SessionIdGenerator getSessionIdGenerator();

    /**
     * Sets the session id generator
     *
     * @param sessionIdGenerator The session id generator
     */
    void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator);

    /**
     * Add this Session to the set of active Sessions for this Manager.
     *
     * @param session Session to be added
     */
    void add(S session);

    /**
     * Change the session ID of the current session to a new randomly generated
     * session ID.
     *
     * @param session The session to change the session ID for
     * @return the new session id
     */
    String changeSessionId(S session);


    /**
     * Change the session ID of the current session to a specified session ID.
     *
     * @param session The session to change the session ID for
     * @param newId   new session ID
     */
    boolean changeSessionId(S session, String newId);

    /**
     * Sets the default longest time that an expired session had been
     * alive.
     *
     * @param defaultMaxInactiveInterval the Default Longest time that an expired
     *                                   session had been alive.
     */
    void setDefaultMaxInactiveInterval(Duration defaultMaxInactiveInterval);

    /**
     * Construct and return a new session object, based on the default
     * settings specified by this Manager's properties.  The session
     * id specified will be used as the session id.
     * If a new session cannot be created for any reason, return
     * <code>null</code>.
     *
     * @param sessionId           The session id which should be used to create the
     *                            new session; if <code>null</code>, the session
     *                            id will be assigned by this method, and available via the getId()
     *                            method of the returned session.
     * @param maxInactiveInterval Specifies the time, in seconds, between client requests before the
     *                            servlet container will invalidate this session.<p>An <tt>interval</tt> value of zero or less indicates that the
     *                            session should never timeout.The default value is 30 minutes.
     * @return An empty Session object with the given ID or a newly created
     * session ID if none was specified
     * @throws IllegalStateException if a new session cannot be
     *                               instantiated for any reason
     */
    S createSession(String sessionId, Duration maxInactiveInterval);

    /**
     * Construct and return a new session object, based on the default
     * settings specified by this Manager's properties.  The session
     * id specified will be used as the session id.
     * If a new session cannot be created for any reason, return
     * <code>null</code>.
     *
     * @param sessionId The session id which should be used to create the
     *                  new session; if <code>null</code>, the session
     *                  id will be assigned by this method, and available via the getId()
     *                  method of the returned session.
     * @return An empty Session object with the given ID or a newly created
     * session ID if none was specified
     * @throws IllegalStateException if a new session cannot be
     *                               instantiated for any reason
     */
    S createSession(String sessionId);

    /**
     * Construct and return a new session object, based on the default
     * settings specified by this Manager's properties.  The session
     * id specified will be used as the session id.
     * If a new session cannot be created for any reason, return
     * <code>null</code>.
     *
     * @return An empty Session object with a newly created
     * session ID
     * @throws IllegalStateException if a new session cannot be
     *                               instantiated for any reason
     */
    S createSession();


    /**
     * Return the active Session, associated with this Manager, with the
     * specified session id (if any); otherwise return <code>null</code>.
     *
     * @param id The session id for the session to be returned
     * @return the request session or {@code null} if a session with the
     * requested ID could not be found
     * @throws IllegalStateException if a new session cannot be
     *                               instantiated for any reason
     */
    S findById(String id);


    /**
     * Deletes the {@link Session} with the given {@link Session#getId()} or does nothing
     * if the {@link Session} is not found.
     *
     * @param id the {@link Session#getId()} to delete
     */
    void deleteById(String id);


}

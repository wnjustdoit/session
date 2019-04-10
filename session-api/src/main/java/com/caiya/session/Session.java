package com.caiya.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * A <b>Session</b> is the Project-internal facade for an
 * <code>HttpSession</code> that is used to maintain state information
 * between requests for a particular user of a web application.
 *
 * @author wangnan
 * @since 1.1
 */
public interface Session<SM extends SessionManager> {

    /**
     * Returns the time when this session was created, measured
     * in milliseconds since midnight January 1, 1970 GMT.
     *
     * @return a <code>long</code> specifying
     * when this session was created,
     * expressed in
     * milliseconds since 1/1/1970 GMT
     */
    Instant getCreationTime();

    /**
     * Returns a string containing the unique identifier assigned
     * to this session.
     *
     * @return session id
     */
    String getId();

    /**
     * Change the session identifier for this session and notifies any associated
     * listeners that a new session has been created.
     *
     * @param newId The new session identifier
     */
    void changeId(String newId);

    /**
     * Returns a string containing the unique identifier assigned
     * to this session.It may be not the current session id.
     *
     * @return the original session id
     */
    String getOriginalId();

    /**
     * Returns the last time the client sent a request associated with this
     * session, as the number of milliseconds since midnight January 1, 1970
     * GMT, and marked by the time the container received the request.
     * <p>
     * Actions that your application takes, such as getting or setting a value
     * associated with the session, do not affect the access time.
     *
     * @return a <code>long</code> representing the last time the client sent a
     * request associated with this session, expressed in milliseconds
     * since 1/1/1970 GMT
     * @throws IllegalStateException if this method is called on an invalidated session
     */
    Instant getLastAccessedTime();


    /**
     * Sets the last accessed time.
     *
     * @param lastAccessedTime the last accessed time
     */
    void setLastAccessedTime(Instant lastAccessedTime);


    /**
     * @return the idle time (in seconds) from last client access time.
     */
    Duration getIdleTime();


    /**
     * Specifies the time, in seconds, between client requests before the
     * servlet container will invalidate this session.
     * <p>
     * <p>An <tt>interval</tt> value of zero or less indicates that the
     * session should never timeout.
     *
     * @param interval An Duration specifying the number
     *                 of seconds
     */
    void setMaxInactiveInterval(Duration interval);

    /**
     * Returns the maximum time interval, in seconds, that the servlet container
     * will keep this session open between client accesses. After this interval,
     * the servlet container will invalidate the session. The maximum time
     * interval can be set with the <code>setMaxInactiveInterval</code> method.
     * A zero or negative time indicates that the session should never timeout.
     *
     * @return an integer specifying the number of seconds this session remains
     * open between client requests
     */
    Duration getMaxInactiveInterval();

    /**
     * Returns the object bound with the specified name in this session, or
     * <code>null</code> if no object is bound under the name.
     *
     * @param name a string specifying the name of the object
     * @return the object with the specified name
     * @throws IllegalStateException if this method is called on an invalidated session
     */
    <T> T getAttribute(String name);

    /**
     * Returns an <code>Set</code> of <code>String</code> objects
     * containing the names of all the objects bound to this session.
     *
     * @return an <code>Enumeration</code> of <code>String</code> objects
     * specifying the names of all the objects bound to this session
     * @throws IllegalStateException if this method is called on an invalidated session
     */
    Set<String> getAttributeNames();

    /**
     * Binds an object to this session, using the name specified. If an object
     * of the same name is already bound to the session, the object is replaced.
     * <p>
     * After this method executes, and if the new object implements
     * <code>HttpSessionBindingListener</code>, the container calls
     * <code>HttpSessionBindingListener.valueBound</code>. The container then
     * notifies any <code>HttpSessionAttributeListener</code>s in the web
     * application.
     * <p>
     * If an object was already bound to this session of this name that
     * implements <code>HttpSessionBindingListener</code>, its
     * <code>HttpSessionBindingListener.valueUnbound</code> method is called.
     * <p>
     * If the value passed in is null, this has the same effect as calling
     * <code>removeAttribute()</code>.
     *
     * @param name  the name to which the object is bound; cannot be null
     * @param value the object to be bound
     * @throws IllegalStateException if this method is called on an invalidated session
     */
    void setAttribute(String name, Object value);

    /**
     * Removes the object bound with the specified name from this session. If
     * the session does not have an object bound with the specified name, this
     * method does nothing.
     * <p>
     * After this method executes, and if the object implements
     * <code>HttpSessionBindingListener</code>, the container calls
     * <code>HttpSessionBindingListener.valueUnbound</code>. The container then
     * notifies any <code>HttpSessionAttributeListener</code>s in the web
     * application.
     *
     * @param name the name of the object to remove from this session
     * @throws IllegalStateException if this method is called on an invalidated session
     */
    void removeAttribute(String name);


    /**
     * @return the Manager within which this Session is valid.
     */
    SM getManager();


    /**
     * Set the Manager within which this Session is valid.
     *
     * @param manager The new Manager
     */
    void setManager(SM manager);


}

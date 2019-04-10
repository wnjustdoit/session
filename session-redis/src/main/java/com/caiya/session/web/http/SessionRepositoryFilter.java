package com.caiya.session.web.http;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.caiya.session.Session;
import com.caiya.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Switches the {@link javax.servlet.http.HttpSession} implementation to be backed by a
 * {@link Session}.
 * <p>
 * The {@link SessionRepositoryFilter} wraps the
 * {@link javax.servlet.http.HttpServletRequest} and overrides the methods to get an
 * {@link javax.servlet.http.HttpSession} to be backed by a
 * {@link Session} returned by the
 * {@link SessionManager}.
 * <p>
 * The {@link SessionRepositoryFilter} uses a {@link HttpSessionIdResolver} (default
 * {@link CookieHttpSessionIdResolver} to bridge logic between an
 * {@link javax.servlet.http.HttpSession} and the
 * {@link Session} abstraction. Specifically:
 * <p>
 * <ul>
 * <li>The session id is looked up using
 * {@link HttpSessionIdResolver#resolveSessionIds(javax.servlet.http.HttpServletRequest)}
 * . The default is to look in a cookie named SESSION.</li>
 * <li>The session id of newly created {@link Session} is sent
 * to the client using
 * <li>The client is notified that the session id is no longer valid with
 * {@link HttpSessionIdResolver#expireSession(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
 * </li>
 * </ul>
 * <p>
 * <p>
 * The SessionRepositoryFilter must be placed before any Filter that access the
 * HttpSession or that might commit the response to ensure the session is overridden and
 * persisted properly.
 * </p>
 *
 * @param <S> the {@link Session} type.
 * @author Rob Winch
 * @author Vedran Pavic
 * @since 1.0
 */
public class SessionRepositoryFilter<S extends Session> extends OncePerRequestFilter {

    private static final String SESSION_LOGGER_NAME = SessionRepositoryFilter.class
            .getName().concat(".SESSION_LOGGER");

    private static final Logger SESSION_LOGGER = LoggerFactory.getLogger(SESSION_LOGGER_NAME);

    /**
     * The session repository request attribute name.
     */
    public static final String SESSION_MANAGER_ATTR = SessionManager.class
            .getName();

    /**
     * Invalid session id (not backed by the session repository) request attribute name.
     */
    public static final String INVALID_SESSION_ID_ATTR = SESSION_MANAGER_ATTR
            + ".invalidSessionId";

    private static final String CURRENT_SESSION_ATTR = SESSION_MANAGER_ATTR
            + ".CURRENT_SESSION";

    /**
     * The default filter order.
     */
    public static final int DEFAULT_ORDER = Integer.MIN_VALUE + 50;

    private final SessionManager<S> sessionManager;

    private ServletContext servletContext;

    private HttpSessionIdResolver httpSessionIdResolver = new CookieHttpSessionIdResolver();

    /**
     * Creates a new instance.
     *
     * @param sessionManager the <code>SessionManager</code> to use. Cannot be null.
     */
    public SessionRepositoryFilter(SessionManager<S> sessionManager) {
        if (sessionManager == null) {
            throw new IllegalArgumentException("sessionRepository cannot be null");
        }
        this.sessionManager = sessionManager;
    }

    /**
     * Sets the {@link HttpSessionIdResolver} to be used. The default is a
     * {@link CookieHttpSessionIdResolver}.
     *
     * @param httpSessionIdResolver the {@link HttpSessionIdResolver} to use. Cannot be
     *                              null.
     */
    public void setHttpSessionIdResolver(HttpSessionIdResolver httpSessionIdResolver) {
        if (httpSessionIdResolver == null) {
            throw new IllegalArgumentException("httpSessionIdResolver cannot be null");
        }
        this.httpSessionIdResolver = httpSessionIdResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        request.setAttribute(SESSION_MANAGER_ATTR, this.sessionManager);

        SessionRepositoryRequestWrapper wrappedRequest = new SessionRepositoryRequestWrapper(
                request, response, this.servletContext);
        SessionRepositoryResponseWrapper wrappedResponse = new SessionRepositoryResponseWrapper(
                wrappedRequest, response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            wrappedRequest.commitSession();
        }
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Allows ensuring that the session is saved if the response is committed.
     *
     * @author Rob Winch
     * @since 1.0
     */
    private final class SessionRepositoryResponseWrapper
            extends OnCommittedResponseWrapper {

        private final SessionRepositoryRequestWrapper request;

        /**
         * Create a new {@link SessionRepositoryResponseWrapper}.
         *
         * @param request  the request to be wrapped
         * @param response the response to be wrapped
         */
        SessionRepositoryResponseWrapper(SessionRepositoryRequestWrapper request,
                                         HttpServletResponse response) {
            super(response);
            if (request == null) {
                throw new IllegalArgumentException("request cannot be null");
            }
            this.request = request;
        }

        @Override
        protected void onResponseCommitted() {
            this.request.commitSession();
        }
    }

    /**
     * A {@link javax.servlet.http.HttpServletRequest} that retrieves the
     * {@link javax.servlet.http.HttpSession} using a
     * {@link SessionManager}.
     *
     * @author Rob Winch
     * @since 1.0
     */
    private final class SessionRepositoryRequestWrapper
            extends HttpServletRequestWrapper {

        private final HttpServletResponse response;

        private final ServletContext servletContext;

        private S requestedSession;

        private boolean requestedSessionCached;

        private Boolean requestedSessionIdValid;

        private boolean requestedSessionInvalidated;

        private SessionRepositoryRequestWrapper(HttpServletRequest request,
                                                HttpServletResponse response, ServletContext servletContext) {
            super(request);
            this.response = response;
            this.servletContext = servletContext;
        }

        /**
         * Uses the {@link HttpSessionIdResolver} to write the session id to the response
         * and persist the Session.
         */
        private void commitSession() {
            HttpSessionWrapper wrappedSession = getCurrentSession();
            if (wrappedSession == null) {
                if (isInvalidateClientSession()) {
                    SessionRepositoryFilter.this.httpSessionIdResolver.expireSession(this,
                            this.response);
                }
            } else {
                S session = wrappedSession.getSession();
                clearRequestedSessionCache();
                SessionRepositoryFilter.this.sessionManager.add(session);
                String sessionId = session.getId();
                if (!isRequestedSessionIdValid()
                        || !sessionId.equals(getRequestedSessionId())) {
                    SessionRepositoryFilter.this.httpSessionIdResolver.setSessionId(this,
                            this.response, sessionId);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private HttpSessionWrapper getCurrentSession() {
            return (HttpSessionWrapper) getAttribute(CURRENT_SESSION_ATTR);
        }

        private void setCurrentSession(HttpSessionWrapper currentSession) {
            if (currentSession == null) {
                removeAttribute(CURRENT_SESSION_ATTR);
            } else {
                setAttribute(CURRENT_SESSION_ATTR, currentSession);
            }
        }

        @Override
        @SuppressWarnings({"unused", "unchecked"})
        public String changeSessionId() {
            HttpSession session = getSession(false);

            if (session == null) {
                throw new IllegalStateException(
                        "Cannot change session ID. There is no session associated with this request.");
            }
            S currentSession = getCurrentSession().getSession();
            return currentSession.getManager().changeSessionId(currentSession);
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            if (this.requestedSessionIdValid == null) {
                S requestedSession = getRequestedSession();
                if (requestedSession != null) {
                    requestedSession.setLastAccessedTime(Instant.now());
                }
                return isRequestedSessionIdValid(requestedSession);
            }

            return this.requestedSessionIdValid;
        }

        private boolean isRequestedSessionIdValid(S session) {
            if (this.requestedSessionIdValid == null) {
                this.requestedSessionIdValid = session != null;
            }
            return this.requestedSessionIdValid;
        }

        private boolean isInvalidateClientSession() {
            return getCurrentSession() == null && this.requestedSessionInvalidated;
        }

        @Override
        public HttpSessionWrapper getSession(boolean create) {
            HttpSessionWrapper currentSession = getCurrentSession();
            if (currentSession != null) {
                return currentSession;
            }
            S requestedSession = getRequestedSession();
            if (requestedSession != null) {
                if (getAttribute(INVALID_SESSION_ID_ATTR) == null) {
                    requestedSession.setLastAccessedTime(Instant.now());
                    this.requestedSessionIdValid = true;
                    currentSession = new HttpSessionWrapper(requestedSession, getServletContext());
                    currentSession.setNew(false);
                    setCurrentSession(currentSession);
                    return currentSession;
                }
            } else {
                // This is an invalid session id. No need to ask again if
                // request.getSession is invoked for the duration of this request
                if (SESSION_LOGGER.isDebugEnabled()) {
                    SESSION_LOGGER.debug(
                            "No session found by id: Caching result for getSession(false) for this HttpServletRequest.");
                }
                setAttribute(INVALID_SESSION_ID_ATTR, "true");
            }
            if (!create) {
                return null;
            }
            if (SESSION_LOGGER.isDebugEnabled()) {
                SESSION_LOGGER.debug(
                        "A new session was created. To help you troubleshoot where the session was created we provided a StackTrace (this is not an error). You can prevent this from appearing by disabling DEBUG logging for "
                                + SESSION_LOGGER_NAME,
                        new RuntimeException(
                                "For debugging purposes only (not an error)"));
            }
            S session;
            session = SessionRepositoryFilter.this.sessionManager.createSession();
            session.setLastAccessedTime(Instant.now());
            currentSession = new HttpSessionWrapper(session, getServletContext());
            setCurrentSession(currentSession);
            return currentSession;
        }

        @Override
        public ServletContext getServletContext() {
            if (this.servletContext != null) {
                return this.servletContext;
            }
            // Servlet 3.0+
            return super.getServletContext();
        }

        @Override
        public HttpSessionWrapper getSession() {
            return getSession(true);
        }

        @Override
        public String getRequestedSessionId() {
            S requestedSession = getRequestedSession();
            return (requestedSession != null ? requestedSession.getId() : null);
        }

        private S getRequestedSession() {
            if (!this.requestedSessionCached) {
                List<String> sessionIds = SessionRepositoryFilter.this.httpSessionIdResolver
                        .resolveSessionIds(this);
                for (String sessionId : sessionIds) {
                    S session = SessionRepositoryFilter.this.sessionManager
                            .findById(sessionId);
                    if (session != null) {
                        this.requestedSession = session;
                        break;
                    }
                }
                this.requestedSessionCached = true;
            }
            return this.requestedSession;
        }

        private void clearRequestedSessionCache() {
            this.requestedSessionCached = false;
            this.requestedSession = null;
        }

        /**
         * Allows creating an HttpSession from a Session instance.
         *
         * @author Rob Winch
         * @since 1.0
         */
        private final class HttpSessionWrapper extends HttpSessionAdapter<S> {

            HttpSessionWrapper(S session, ServletContext servletContext) {
                super(session, servletContext);
            }

            @Override
            public void invalidate() {
                super.invalidate();
                SessionRepositoryRequestWrapper.this.requestedSessionInvalidated = true;
                setCurrentSession(null);
                clearRequestedSessionCache();
                SessionRepositoryFilter.this.sessionManager.deleteById(getId());
            }
        }

    }

}

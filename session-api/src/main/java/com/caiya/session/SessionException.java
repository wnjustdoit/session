package com.caiya.session;

/**
 * Session Exception.
 */
public class SessionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SessionException(String msg) {
        super(msg);
    }

    public SessionException(Exception e) {
        super(e);
    }

    public SessionException(String msg, Exception e) {
        super(msg, e);
    }
}

package com.caiya.session.util;

import com.caiya.session.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Basic Abstract Class of the Session Id Generator.
 *
 * @author wangnan
 * @since 1.0
 */
public abstract class SessionIdGeneratorBase implements SessionIdGenerator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Node identifier when in a cluster. Defaults to the empty string.
     */
    private String jvmRoute = "";


    /**
     * Number of bytes in a session ID. Defaults to 16.
     */
    private int sessionIdLength = 16;

    /**
     * Queue of random number generator objects to be used when creating session
     * identifiers. If the queue is empty when a random number generator is
     * required, a new random number generator object is created. This is
     * designed this way since random number generators use a sync to make them
     * thread-safe and the sync makes using a a single object slow(er).
     */
    private final Queue<SecureRandom> randoms = new ConcurrentLinkedQueue<>();

    private String secureRandomClass = null;

    private String secureRandomAlgorithm = "SHA1PRNG";

    private String secureRandomProvider = null;

    @Override
    public String getJvmRoute() {
        return jvmRoute;
    }

    @Override
    public void setJvmRoute(String jvmRoute) {
        this.jvmRoute = jvmRoute;
    }

    @Override
    public int getSessionIdLength() {
        return sessionIdLength;
    }

    @Override
    public void setSessionIdLength(int sessionIdLength) {
        this.sessionIdLength = sessionIdLength;
    }

    @Override
    public String generateSessionId() {
        return generateSessionId(jvmRoute);
    }

    /**
     * Get the name of the algorithm used to create the {@link SecureRandom}
     * instances which generate new session IDs.
     *
     * @return The name of the algorithm. {@code null} or the empty string means
     * that platform default will be used
     */
    public String getSecureRandomAlgorithm() {
        return secureRandomAlgorithm;
    }


    /**
     * Specify a non-default algorithm to use to create instances of
     * {@link SecureRandom} which are used to generate session IDs. If no
     * algorithm is specified, SHA1PRNG is used. To use the platform default
     * (which may be SHA1PRNG), specify {@code null} or the empty string. If an
     * invalid algorithm and/or provider is specified the {@link SecureRandom}
     * instances will be created using the defaults for this
     * {@link SessionIdGenerator} implementation. If that fails, the
     * {@link SecureRandom} instances will be created using platform defaults.
     *
     * @param secureRandomAlgorithm The name of the algorithm
     */
    public void setSecureRandomAlgorithm(String secureRandomAlgorithm) {
        this.secureRandomAlgorithm = secureRandomAlgorithm;
    }

    protected void getRandomBytes(byte bytes[]) {

        SecureRandom random = randoms.poll();
        if (random == null) {
            random = createSecureRandom();
        }
        random.nextBytes(bytes);
        randoms.add(random);
    }


    /**
     * Create a new random number generator instance we should use for
     * generating session identifiers.
     */
    private SecureRandom createSecureRandom() {

        SecureRandom result = null;

        long t1 = System.currentTimeMillis();
        if (secureRandomClass != null) {
            try {
                // Construct and seed a new random number generator
                Class<?> clazz = Class.forName(secureRandomClass);
                result = (SecureRandom) clazz.newInstance();
            } catch (Exception e) {
                logger.error("sessionIdGeneratorBase.random, secureRandomClass:{}",
                        secureRandomClass, e);
            }
        }

        if (result == null) {
            // No secureRandomClass or creation failed. Use SecureRandom.
            try {
                if (secureRandomProvider != null &&
                        secureRandomProvider.length() > 0) {
                    result = SecureRandom.getInstance(secureRandomAlgorithm,
                            secureRandomProvider);
                } else if (secureRandomAlgorithm != null &&
                        secureRandomAlgorithm.length() > 0) {
                    result = SecureRandom.getInstance(secureRandomAlgorithm);
                }
            } catch (NoSuchAlgorithmException e) {
                logger.error("sessionIdGeneratorBase.randomAlgorithm, secureRandomAlgorithm:{}",
                        secureRandomAlgorithm, e);
            } catch (NoSuchProviderException e) {
                logger.error("sessionIdGeneratorBase.randomProvider, secureRandomProvider:{}",
                        secureRandomProvider, e);
            }
        }

        if (result == null) {
            // Invalid provider / algorithm
            try {
                result = SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException e) {
                logger.error("sessionIdGeneratorBase.randomAlgorithm, secureRandomAlgorithm:{}",
                        secureRandomAlgorithm, e);
            }
        }

        if (result == null) {
            // Nothing works - use platform default
            result = new SecureRandom();
        }

        // Force seeding to take place
        result.nextInt();

        long t2 = System.currentTimeMillis();
        if ((t2 - t1) > 100)
            logger.info("sessionIdGeneratorBase.createRandom, algorithm:{}, duration:{}",
                    result.getAlgorithm(), Long.valueOf(t2 - t1));
        return result;
    }
}

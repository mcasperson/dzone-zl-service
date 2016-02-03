package com.matthewcasperson.dzonezl.services;

import org.apache.http.HttpEntity;

import java.io.IOException;

/**
 * A service that exposes some common HTTP entity utility methods
 */
public interface HttpEntityUtils {
    /**
     *
     * @param responseEntity The entity to be converted to a string
     * @return The string representation of the entity
     * @throws IOException
     */
    String responseToString(final HttpEntity responseEntity) throws IOException;
}

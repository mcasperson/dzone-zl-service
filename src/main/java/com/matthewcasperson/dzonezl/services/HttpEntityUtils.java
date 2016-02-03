package com.matthewcasperson.dzonezl.services;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.util.Optional;

/**
 * A service that exposes some common HTTP entity utility methods
 */
public interface HttpEntityUtils {
    /**
     * Converts an entity to a string
     * @param responseEntity The entity to be converted to a string
     * @return The string representation of the entity
     * @throws IOException
     */
    String responseToString(final HttpEntity responseEntity) throws IOException;

    Optional<String> getCookie(final HttpResponse httpResponse, final String cookieName);

    HttpResponse makeRequest(final HttpUriRequest request) throws IOException;
}

package com.matthewcasperson.dzonezl.services;

import org.apache.http.HttpEntity;

import java.io.IOException;

/**
 * A service that exposes some common HTTP entity utility methods
 */
public interface HttpEntityUtils {
    String responseToString(final HttpEntity responseEntity) throws IOException;
}

package com.matthewcasperson.dzonezl.services;

import org.apache.http.HttpEntity;

import java.io.IOException;

/**
 * Created by Matthew on 3/02/2016.
 */
public interface HttpEntityUtils {
    String responseToString(final HttpEntity responseEntity) throws IOException;
}

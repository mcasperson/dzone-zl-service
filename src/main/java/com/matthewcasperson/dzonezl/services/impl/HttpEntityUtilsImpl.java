package com.matthewcasperson.dzonezl.services.impl;

import com.matthewcasperson.dzonezl.services.HttpEntityUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * A service that exposes some common HTTP entity utility methods
 */
@Component
public class HttpEntityUtilsImpl implements HttpEntityUtils {
    @Override
    public String responseToString(final HttpEntity responseEntity) throws IOException {

        try (final InputStream instream = responseEntity.getContent()){
            final String responseText = IOUtils.toString(instream);
            return responseText;
        }
    }
}

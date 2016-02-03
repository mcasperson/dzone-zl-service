package com.matthewcasperson.dzonezl.services.impl;

import com.matthewcasperson.dzonezl.services.HttpEntityUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A service that exposes some common HTTP entity utility methods
 */
@Component
public class HttpEntityUtilsImpl implements HttpEntityUtils {
    @Override
    public String responseToString(final HttpEntity responseEntity) throws IOException {
        checkNotNull(responseEntity);

        try (final InputStream instream = responseEntity.getContent()){
            final String responseText = IOUtils.toString(instream);
            return responseText;
        }
    }

    @Override
    public Optional<String> getCookie(final HttpResponse httpResponse, final String cookieName) {
        final Header[] headers = httpResponse.getHeaders("Set-Cookie");
        for (final Header header : headers) {
            final String[] cookies = header.getValue().split(",");
            for (final String cookie : cookies) {
                final String[] cookieDetails = cookie.split("=");
                if (cookieDetails.length >= 2 && cookieName.equals(cookieDetails[0])) {
                    /*
                        We only want the value, not any other details like paths
                     */
                    return Optional.of(cookieDetails[1].split(";")[0]);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public HttpResponse makeRequest(final HttpUriRequest request) throws IOException {
        final CloseableHttpClient httpclient = HttpClients.createDefault();
        final CloseableHttpResponse response = httpclient.execute(request);
        try {
            return response;
        } finally {
            response.close();
        }
    }
}

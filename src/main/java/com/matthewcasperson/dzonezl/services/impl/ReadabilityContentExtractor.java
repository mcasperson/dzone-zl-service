package com.matthewcasperson.dzonezl.services.impl;

import com.matthewcasperson.dzonezl.Constants;
import com.matthewcasperson.dzonezl.entities.ContentImport;
import com.matthewcasperson.dzonezl.services.ContentExtractor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A content extractor that makes use of the Readability API
 */
@Component(value="readabilityContentExtractor")
public class ReadabilityContentExtractor implements ContentExtractor {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ReadabilityContentExtractor.class);

    @Override
    public Optional<ContentImport> extractContent(final String url, final Map<String, String> data) {
        checkArgument(StringUtils.isNotBlank(url));
        checkNotNull(data);
        checkArgument(data.containsKey(Constants.READABILITY_TOKEN_NAME));

        try {
        /*
            Do the initial login to get any security cookies
         */

            final HttpGet importGet = new HttpGet("https://www.readability.com/api/content/v1/parser?token=" +
                    data.get(Constants.READABILITY_TOKEN_NAME) + "&url=" + url);

            final CloseableHttpClient httpclient = HttpClients.createDefault();

            try (final CloseableHttpResponse response = httpclient.execute(importGet)) {
                try (final InputStream instream = response.getEntity().getContent()) {
                    final JsonReader jsonReader = Json.createReader(instream);
                    final JsonObject topLevelObject = jsonReader.readObject();

                    final Optional<JsonString> htmlContent = Optional.of(topLevelObject.getJsonString("content"));
                    final Optional<JsonString> titleContent = Optional.of(topLevelObject.getJsonString("title"));

                    if (htmlContent.isPresent() &&
                            StringUtils.isNotBlank(htmlContent.get().getString()) &&
                            titleContent.isPresent() &&
                            StringUtils.isNotBlank(titleContent.get().getString())) {
                        LOGGER.info("Successfully extracted content via Readability");
                        return Optional.of(new ContentImport(
                                htmlContent.get().getString(),
                                titleContent.get().getString()
                        ));
                    }
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception thrown", ex);
        }

        LOGGER.info("Failed to extracted content via Readability");
        return Optional.empty();
    }
}

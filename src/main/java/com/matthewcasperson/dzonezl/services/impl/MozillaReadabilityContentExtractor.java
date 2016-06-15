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
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A content extractor based on the reader view provided in Firefox
 */
@Component(value="mozillaReadabilityContentExtractor")
public class MozillaReadabilityContentExtractor implements ContentExtractor {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MozillaReadabilityContentExtractor.class);

    /**
     * Our instance of https://github.com/n1k0/readable-proxy/
     */
    private static final String URL = "https://polar-river-15245.herokuapp.com/api/get";

    @Override
    public Optional<ContentImport> extractContent(final String url, final Map<String, String> data) {
        checkArgument(StringUtils.isNotBlank(url));

        try {
        /*
            Do the initial login to get any security cookies
         */

            final String readabilityUrl = URL + "?santitze=y&url=" + url;

            LOGGER.info("Querying Mozilla Readability via " + readabilityUrl);

            final HttpGet importGet = new HttpGet(readabilityUrl);

            final CloseableHttpClient httpclient = HttpClients.createDefault();

            try (final CloseableHttpResponse response = httpclient.execute(importGet)) {
                try (final InputStream instream = response.getEntity().getContent()) {
                    final JsonReader jsonReader = Json.createReader(instream);
                    final JsonObject topLevelObject = jsonReader.readObject();

                    final Optional<JsonString> htmlContent = Optional.ofNullable(topLevelObject.getJsonString("content"));
                    final Optional<JsonString> titleContent = Optional.ofNullable(topLevelObject.getJsonString("title"));

                    if (htmlContent.isPresent() &&
                            StringUtils.isNotBlank(htmlContent.get().getString()) &&
                            titleContent.isPresent() &&
                            StringUtils.isNotBlank(titleContent.get().getString())) {
                        LOGGER.info("Successfully extracted content via Mozilla Readability");
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

        LOGGER.info("Failed to extracted content via Mozilla Readability");
        return Optional.empty();
    }
}

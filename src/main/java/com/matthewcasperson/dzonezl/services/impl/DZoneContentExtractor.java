package com.matthewcasperson.dzonezl.services.impl;

import com.matthewcasperson.dzonezl.Constants;
import com.matthewcasperson.dzonezl.entities.ContentImport;
import com.matthewcasperson.dzonezl.services.ContentExtractor;
import com.matthewcasperson.dzonezl.services.HttpEntityUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
 * A content extractor that makes use of the DZone API
 */
@Component(value="dZoneContentExtractor")
public class DZoneContentExtractor implements ContentExtractor {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DZoneContentExtractor.class);

    @Override
    public Optional<ContentImport> extractContent(final String url, final Map<String, String> data) {
        checkArgument(StringUtils.isNotBlank(url));
        checkNotNull(data);
        checkArgument(data.containsKey(Constants.AWSELB_COOKIE), "AWS ELB Cookie required");
        checkArgument(data.containsKey(Constants.TH_CSRF_COOKIE), "Spring CSRF Cookie required");
        checkArgument(data.containsKey(Constants.SPRING_SECUITY_COOKIE), "Spring security cookie required");
        checkArgument(data.containsKey(Constants.JSESSIONID_COOKIE), "Java JSESSION ID cookie required");

        try {
            /*
                Do the initial login to get any security cookies
             */

            final HttpPost importPost = new HttpPost("https://dzone.com/services/internal/action/links-getData");

            final String importJson = "{\"url\":\"" + url + "\",\"parse\":true}";

            final StringEntity requestEntity = new StringEntity(importJson);
            requestEntity.setContentType(MediaType.APPLICATION_JSON_VALUE);
            importPost.setEntity(requestEntity);

            importPost.setHeader(Constants.COOKIE_HEADER,
                    Constants.AWSELB_COOKIE + "=" + data.get(Constants.AWSELB_COOKIE) + "; " +
                    Constants.TH_CSRF_COOKIE + "=" + data.get(Constants.TH_CSRF_COOKIE) + "; " +
                    Constants.SPRING_SECUITY_COOKIE + "=" + data.get(Constants.SPRING_SECUITY_COOKIE) + "; " +
                    Constants.JSESSIONID_COOKIE + "=" + data.get(Constants.JSESSIONID_COOKIE) + "; " +
                    Constants.SESSION_STARTED_COOKIE + "=true");

            importPost.addHeader(Constants.X_TH_CSRF_HEADER, data.get(Constants.TH_CSRF_COOKIE));

            final CloseableHttpClient httpclient = HttpClients.createDefault();
            final CloseableHttpResponse response = httpclient.execute(importPost);
            try {
                try (final InputStream instream = response.getEntity().getContent()) {
                    final JsonReader jsonReader = Json.createReader(instream);
                    final JsonObject topLevelObject = jsonReader.readObject();

                    /*
                        We don't actually know if the result will contain the json elements
                        accessed here, so we use Optional and map() to deal with null
                        values.
                     */
                    final Optional<JsonObject> dataObject =
                            Optional.of(topLevelObject.getJsonObject("result"))
                            .map(n -> n.getJsonObject("data"));
                    final Optional<JsonString> htmlContent = dataObject.map(n -> n.getJsonString("htmlContent"));
                    final Optional<JsonString> titleContent = dataObject.map(n -> n.getJsonString("titleContent"));

                    if (htmlContent.isPresent() &&
                            StringUtils.isNotBlank(htmlContent.get().getString()) &&
                            titleContent.isPresent() &&
                            StringUtils.isNotBlank(titleContent.get().getString())) {
                        LOGGER.info("Successfully extracted content via DZone");
                        return Optional.of(new ContentImport(htmlContent.get().getString(), titleContent.get().getString()));
                    }
                }
            } finally {
                response.close();
            }
        } catch (final IOException ex) {
            LOGGER.error("Exception thrown", ex);
        }

        return Optional.empty();
    }
}

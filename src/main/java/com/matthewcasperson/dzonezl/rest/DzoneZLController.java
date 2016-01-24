package com.matthewcasperson.dzonezl.rest;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hibernate.SessionFactory;
import org.hibernate.internal.util.collections.ConcurrentReferenceHashMap;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Matthew on 24/01/2016.
 */
@RestController
public class DzoneZLController {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DzoneZLController.class);

    private static final String SPRING_SECUITY_COOKIE = "SPRING_SECURITY_REMEMBER_ME_COOKIE";
    private static final String AWSELB_COOKIE = "AWSELB";
    private static final String TH_CSRF_COOKIE = "TH_CSRF";
    private static final String JSESSIONID_COOKIE = "JSESSIONID";
    private static final String SESSION_STARTED_COOKIE = "SESSION_STARTED";
    private static final String X_TH_CSRF_HEADER = "X-TH-CSRF";

    private static final String SUCCESS = "\"success\":true";
    private static final Pattern ID_RE = Pattern.compile("\"id\":(?<id>\\d+)");
    private static final Pattern ID_QUOTE_RE = Pattern.compile("\"id\":\"(?<id>\\d+)\"");
    private static final Pattern DATA_RE = Pattern.compile("\"data\":\"(?<data>.*?)\"");

    @Autowired
    private EntityManagerFactory emf;

    private MultivaluedMap<String, String> fromMap(final Map<String, String> input) {
        return new MultivaluedHashMap<String, String>(input);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            value={"/data/{entity}", "/data/{entity}/relationship/{entity2}", "/data/{entity}/{child}"})
    @Transactional
    public String authors(@RequestParam final Map<String, String> allRequestParams, final HttpServletRequest request) {
        final String restOfTheUrl = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        final SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);

        /* Takes a hibernate session factory */
        final DataStore dataStore = new HibernateStore(sessionFactory);
        final Logger logger = new Slf4jLogger();
        final Elide elide = new Elide(logger, dataStore);
        final MultivaluedMap<String, String> params = fromMap(allRequestParams);

        final ElideResponse response = elide.get(restOfTheUrl.replaceAll("^/data/", ""), params, new Object());

        return response.getBody();
    }

    /**
     * This endpoint simulates a login into DZone. We have to replicate a few redirections, after which we return
     * two cookies: AWSELB, TH_CSRF and SPRING_SECURITY_REMEMBER_ME_COOKIE. These three cookies need to be passed
     * into all future operations against DZone
     * @param username  DZone username
     * @param password  DZone password
     * @return
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/action/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public String login(@RequestParam final String username, @RequestParam final String password) throws IOException {

        /*
            Do the initial login to get any security cookies
         */
        final HttpGet initialGet = new HttpGet("https://dzone.com");
        final HttpResponse initialResponse = makeRequest(initialGet);
        final Optional<String> awselbCookie = getCookie(initialResponse, AWSELB_COOKIE);
        final Optional<String> thCsrfCookie = getCookie(initialResponse, TH_CSRF_COOKIE);
        final Optional<String> jSessionIdCookie = getCookie(initialResponse, JSESSIONID_COOKIE);

        if (awselbCookie.isPresent() && thCsrfCookie.isPresent() && jSessionIdCookie.isPresent()) {
            /*
                Now we do the actual login
             */
            final String loginJson = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

            final HttpPost httppost = new HttpPost("https://dzone.com/services/internal/action/users-login");
            httppost.setHeader("Cookie",
                AWSELB_COOKIE + "=" + awselbCookie.get() + "; " +
                TH_CSRF_COOKIE + "=" + thCsrfCookie.get() + "; " +
                JSESSIONID_COOKIE + "=" + jSessionIdCookie.get() + "; " +
                SESSION_STARTED_COOKIE + "=true");

            final StringEntity requestEntity = new StringEntity(loginJson);
            requestEntity.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httppost.setEntity(requestEntity);

            httppost.addHeader(X_TH_CSRF_HEADER, thCsrfCookie.get());
            httppost.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);

            final CloseableHttpClient httpclient = HttpClients.createDefault();
            final CloseableHttpResponse loginResponse = httpclient.execute(httppost);
            try {
                final String responseBody = responseToString(loginResponse.getEntity());

                LOGGER.info(responseBody);
            } finally {
                loginResponse.close();
            }

            final Optional<String> springSecurityCookie = getCookie(loginResponse, SPRING_SECUITY_COOKIE);

            if (springSecurityCookie.isPresent()) {
                return "{\"" + AWSELB_COOKIE + "\": \"" + awselbCookie.get() + "\", " +
                       "\"" + TH_CSRF_COOKIE + "\": \"" + thCsrfCookie.get() + "\", " +
                        "\"" + JSESSIONID_COOKIE + "\": \"" + jSessionIdCookie.get() + "\", " +
                        "\"" + SPRING_SECUITY_COOKIE + "\": \"" + springSecurityCookie.get() + "\"}";

            }
        }

        return null;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/action/import", produces = MediaType.APPLICATION_JSON_VALUE)
    public String importPost(
        @RequestParam final String awselbCookie,
        @RequestParam final String thCsrfCookie,
        @RequestParam final String springSecurityCookie,
        @RequestParam final String jSessionIdCookie,
        @RequestParam final String url) throws IOException {

        /*
            Do the initial login to get any security cookies
         */
        final HttpPost importPost = new HttpPost("https://dzone.com/services/internal/action/links-getData");

        final String importJson = "{\"url\":\"" + url + "\",\"parse\":true}";

        final StringEntity requestEntity = new StringEntity(importJson);
        requestEntity.setContentType(MediaType.APPLICATION_JSON_VALUE);
        importPost.setEntity(requestEntity);

        importPost.setHeader("Cookie",
                AWSELB_COOKIE + "=" + awselbCookie + "; " +
                TH_CSRF_COOKIE + "=" + thCsrfCookie + "; " +
                SPRING_SECUITY_COOKIE + "=" + springSecurityCookie + "; " +
                JSESSIONID_COOKIE + "=" + jSessionIdCookie + "; " +
                SESSION_STARTED_COOKIE + "=true");

        importPost.addHeader(X_TH_CSRF_HEADER, thCsrfCookie);

        final CloseableHttpClient httpclient = HttpClients.createDefault();
        final CloseableHttpResponse response = httpclient.execute(importPost);
        try {
            final String responseBody = responseToString(response.getEntity());

            LOGGER.info(responseBody);

            return responseBody;
        } finally {
            response.close();
        }
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/action/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    public String submitPost(
            @RequestParam final String awselbCookie,
            @RequestParam final String thCsrfCookie,
            @RequestParam final String springSecurityCookie,
            @RequestParam final String jSessionIdCookie,
            @RequestParam final String title,
            @RequestParam final String content,
            @RequestParam final String url,
            @RequestParam final String topics,
            @RequestParam final String authors,
            @RequestParam final Integer poster,
            @RequestParam final Integer image) throws IOException {

        final Optional<String> newImageId = uploadImage(awselbCookie, thCsrfCookie, springSecurityCookie, jSessionIdCookie, image);

        if (newImageId.isPresent()) {

            final String submitBody =
                    "{\"type\":\"article\"," +
                            "\"title\":\"" + title + "\"," +
                            "\"body\":\"" + content + "\"," +
                            "\"topics\":\"" + topics + "\"," +
                            "\"portal\":null," +
                            "\"thumb\":" + newImageId.get() + "," +
                            "\"sources\":[]," +
                            "\"notes\":\"\"," +
                            "\"editorsPick\":false," +
                            "\"metaDescription\":\"\"," +
                            "\"tldr\":\"\"," +
                            "\"originalSource\":\"" + url + "\"," +
                            "\"visibility\":\"draft\"}";

            final HttpPut httppost = new HttpPut("https://dzone.com/services/internal/ctype/article");
            httppost.setHeader("Cookie",
                    AWSELB_COOKIE + "=" + awselbCookie + "; " +
                            TH_CSRF_COOKIE + "=" + thCsrfCookie + "; " +
                            SPRING_SECUITY_COOKIE + "=" + springSecurityCookie + "; " +
                            JSESSIONID_COOKIE + "=" + jSessionIdCookie + "; " +
                            SESSION_STARTED_COOKIE + "=true");

            final StringEntity requestEntity = new StringEntity(submitBody);
            requestEntity.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httppost.setEntity(requestEntity);

            httppost.addHeader(X_TH_CSRF_HEADER, thCsrfCookie);
            httppost.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);

            final CloseableHttpClient httpclient = HttpClients.createDefault();
            final CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                final String responseBody = responseToString(response.getEntity());
                final Matcher idMatcher = ID_QUOTE_RE.matcher(responseBody);

                if (responseBody.indexOf(SUCCESS) != -1) {

                    if (idMatcher.find()) {
                        try {
                            final Integer articleId = Integer.parseInt(idMatcher.group("id"));

                            /*
                                Associate the poster with the article
                             */
                            if (!addPosterToArticle(articleId, poster, false, awselbCookie, thCsrfCookie, springSecurityCookie, jSessionIdCookie)) {
                                throw new Exception("Failed to associated poster with article");
                            }

                            /*
                                Associate all the authors with the article
                             */
                            final String[] authorsCollection = authors.split(",");
                            for (final String author : authorsCollection) {
                                final Integer authorId = Integer.parseInt(author);
                                if (!addPosterToArticle(articleId, authorId, true, awselbCookie, thCsrfCookie, springSecurityCookie, jSessionIdCookie)) {
                                    throw new Exception("Failed to associated author with article");
                                }
                            }

                        } catch (final Exception ex) {
                            /*
                                We didn't find the expected article id
                             */
                            return "{\"success\":false}";
                        }
                    }

                }


                LOGGER.info(responseBody);

            /*
                Return failure to the client
             */
                return responseBody;
            } finally {
                response.close();
            }
        }

        return "{\"success\":false}";
    }

    private Optional<String> getImageUploadTrackingCode(final String awselbCookie,
                                                        final String thCsrfCookie,
                                                        final String springSecurityCookie,
                                                        final String jSessionIdCookie) throws IOException {
        final HttpGet getImageId = new HttpGet("https://dzone.com/services/internal/data/uploads-authorize?image=true&type=node");
        getImageId.setHeader("Cookie",
                AWSELB_COOKIE + "=" + awselbCookie + "; " +
                TH_CSRF_COOKIE + "=" + thCsrfCookie + "; " +
                JSESSIONID_COOKIE + "=" + jSessionIdCookie + "; " +
                SPRING_SECUITY_COOKIE + "=" + springSecurityCookie + "; " +
                SESSION_STARTED_COOKIE + "=true");


        getImageId.addHeader(X_TH_CSRF_HEADER, thCsrfCookie);
        getImageId.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);

        final CloseableHttpClient httpclient = HttpClients.createDefault();
        final CloseableHttpResponse loginResponse = httpclient.execute(getImageId);
        try {
            final String imageIdResponse = responseToString(loginResponse.getEntity());

            final Matcher dataMatcher = DATA_RE.matcher(imageIdResponse);
            if (dataMatcher.find()) {
                final String dataId = dataMatcher.group("data");
                return Optional.of(dataId);

            }
        } finally {
            loginResponse.close();
        }

        return Optional.empty();
    }

    private Optional<String> uploadImage(final String awselbCookie,
                                         final String thCsrfCookie,
                                         final String springSecurityCookie,
                                         final String jSessionIdCookie,
                                         final String tackingId,
                                         final File imageFile) throws IOException {
        final HttpPost uploadPost = new HttpPost("https://dzone.com/uploadFile.json?trackingId=" + tackingId);

        final FileBody fileBody = new FileBody(imageFile);

        final HttpEntity fileEntity = MultipartEntityBuilder.create()
            .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            .addPart("file", fileBody)
            .build();

        uploadPost.setEntity(fileEntity);

        uploadPost.setHeader("Cookie",
                AWSELB_COOKIE + "=" + awselbCookie + "; " +
                TH_CSRF_COOKIE + "=" + thCsrfCookie + "; " +
                SPRING_SECUITY_COOKIE + "=" + springSecurityCookie + "; " +
                JSESSIONID_COOKIE + "=" + jSessionIdCookie + "; " +
                SESSION_STARTED_COOKIE + "=true");

        uploadPost.addHeader(X_TH_CSRF_HEADER, thCsrfCookie);

        final CloseableHttpClient httpclient = HttpClients.createDefault();
        final CloseableHttpResponse response = httpclient.execute(uploadPost);
        try {
            final String responseBody = responseToString(response.getEntity());
            final Matcher idMatcher = ID_RE.matcher(responseBody);
            if (idMatcher.find()) {
                return Optional.of(idMatcher.group("id"));
            }
        } finally {
            response.close();
        }

        return Optional.empty();
    }

    /**
     * This is a reasonably complex process. We need to
     * 1. Download the image already hosted by DZone
     * 2. Get a tracking id for the upload
     * 3. Upload the file
     * 4. Return the new ID
     * @param awselbCookie
     * @param thCsrfCookie
     * @param springSecurityCookie
     * @param jSessionIdCookie
     * @throws IOException
     */
    private Optional<String> uploadImage(final String awselbCookie,
                             final String thCsrfCookie,
                             final String springSecurityCookie,
                             final String jSessionIdCookie,
                             final Integer imageId) throws IOException {

        /*
            1. Download the existing file
         */
        final File imageFile = File.createTempFile("dzoneTempImage", ".img");
        IOUtils.copy(
                new URL("https://dz2cdn1.dzone.com/thumbnail?fid=" + imageId + "&w=600").openStream(),
                new FileOutputStream(imageFile));

        /*
            2. Get a tracking code
         */
        final Optional<String> trackingId = getImageUploadTrackingCode(awselbCookie, thCsrfCookie, springSecurityCookie, jSessionIdCookie);

        /*
            3. Upload the file, and get the new image id
         */
        if (trackingId.isPresent()) {
            final Optional<String> newImageId = uploadImage(awselbCookie, thCsrfCookie, springSecurityCookie, jSessionIdCookie, trackingId.get(), imageFile);

            return newImageId;
        }

        return Optional.empty();
    }

    private boolean addPosterToArticle(final Integer articleId,
                                        final Integer user,
                                        final boolean author,
                                        final String awselbCookie,
                                        final String thCsrfCookie,
                                        final String springSecurityCookie,
                                        final String jSessionIdCookie) throws IOException {

        final String posterBody = author ?
                "{\"user\": " + user + ", \"type\": \"author\"}" :
                "{\"user\": " + user + ", \"type\": \"op\"}";

        final HttpPost posterAssignment = new HttpPost("https://dzone.com/services/internal/node/" + articleId + "/authors-addAuthor");
        posterAssignment.setHeader("Cookie",
                AWSELB_COOKIE + "=" + awselbCookie + "; " +
                TH_CSRF_COOKIE + "=" + thCsrfCookie + "; " +
                SPRING_SECUITY_COOKIE + "=" + springSecurityCookie + "; " +
                JSESSIONID_COOKIE + "=" + jSessionIdCookie + "; " +
                SESSION_STARTED_COOKIE + "=true");

        final StringEntity posterEntity = new StringEntity(posterBody);
        posterEntity.setContentType(MediaType.APPLICATION_JSON_VALUE);
        posterAssignment.setEntity(posterEntity);

        posterAssignment.addHeader(X_TH_CSRF_HEADER, thCsrfCookie);
        posterAssignment.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);

        final CloseableHttpClient httpclient = HttpClients.createDefault();
        final CloseableHttpResponse posterResponse = httpclient.execute(posterAssignment);
        try {
            final String responseBody = responseToString(posterResponse.getEntity());
            return responseBody.indexOf(SUCCESS) != -1;
        } finally {
            posterResponse.close();
        }
    }

    private Optional<String> getCookie(final HttpResponse httpResponse, final String cookieName) {
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

    private HttpResponse makeRequest(final HttpUriRequest request) throws IOException {
        final CloseableHttpClient httpclient = HttpClients.createDefault();
        final CloseableHttpResponse response = httpclient.execute(request);
        try {
            return response;
        } finally {
            response.close();
        }
    }

    private String responseToString(final HttpEntity responseEntity) throws IOException {
        final InputStream instream = responseEntity.getContent();
        try {
            final String responseText = IOUtils.toString(instream);
            return responseText;
        } finally {
            instream.close();
        }
    }
}

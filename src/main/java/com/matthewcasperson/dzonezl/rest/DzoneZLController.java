package com.matthewcasperson.dzonezl.rest;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hibernate.SessionFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Optional;

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

    @Autowired
    private EntityManagerFactory emf;

    private MultivaluedMap<String, String> fromMap(final Map<String, String> input) {
        final MultivaluedMap queryParams = new MultivaluedMapImpl();
        for (final String key : input.keySet()) {
            queryParams.put(key, input.get(key));
        }
        return queryParams;
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

            final HttpResponse loginResponse = makeRequest(httppost);

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

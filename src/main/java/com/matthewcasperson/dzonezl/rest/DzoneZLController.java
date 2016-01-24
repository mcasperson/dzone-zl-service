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

        if (awselbCookie.isPresent()) {
            /*
                Simulate the redirect, after which we'll get a csrf cookie
             */
            final HttpGet secondLoad = new HttpGet("https://dzone.com");
            secondLoad.setHeader("Cookie", AWSELB_COOKIE + "=" + awselbCookie.get());
            final HttpResponse secondLoadResponse = makeRequest(secondLoad);
            final Optional<String> thCsrfCookie = getCookie(secondLoadResponse, TH_CSRF_COOKIE);

            if (thCsrfCookie.isPresent()) {
                /*
                    Now we do the actual login
                 */
                final String loginJson = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

                final HttpPost httppost = new HttpPost("https://dzone.com/services/internal/action/users-login");
                httppost.setHeader("Cookie", AWSELB_COOKIE + "=" + awselbCookie.get());
                httppost.setHeader("Cookie", TH_CSRF_COOKIE + "=" + thCsrfCookie.get());

                final StringEntity requestEntity = new StringEntity(loginJson);

                requestEntity.setContentType(MediaType.APPLICATION_JSON_VALUE);
                httppost.setEntity(requestEntity);

                httppost.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);

                final HttpResponse loginResponse = makeRequest(httppost);

                final Optional<String> springSecurityCookie = getCookie(loginResponse, SPRING_SECUITY_COOKIE);

                if (springSecurityCookie.isPresent()) {
                    return "{\"" + AWSELB_COOKIE + "\": \"" + awselbCookie.get() + "\", " +
                           "\"" + TH_CSRF_COOKIE + "\": \"" + thCsrfCookie.get() + "\", " +
                            "\"" + SPRING_SECUITY_COOKIE + "\": \"" + springSecurityCookie.get() + "\"}";
                }
            }
        }

        return null;
    }

    private Optional<String> getCookie(final HttpResponse httpResponse, final String cookie) {
        final Header[] headers = httpResponse.getHeaders("Set-Cookie");
        for (final Header header : headers) {
            final String[] cookieDetails = header.getValue().split("=");
            if (cookieDetails.length == 2 && cookie.equals(cookieDetails[0])) {
                return Optional.of(cookieDetails[1]);
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

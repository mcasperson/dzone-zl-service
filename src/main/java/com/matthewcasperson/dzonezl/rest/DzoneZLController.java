package com.matthewcasperson.dzonezl.rest;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.matthewcasperson.dzonezl.Constants;
import com.matthewcasperson.dzonezl.entities.ContentImport;
import com.matthewcasperson.dzonezl.jpa.Article;
import com.matthewcasperson.dzonezl.services.ContentExtractor;
import com.matthewcasperson.dzonezl.services.HtmlSanitiser;
import com.matthewcasperson.dzonezl.services.HttpEntityUtils;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.SecurityMode;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import net.htmlparser.jericho.Source;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hibernate.SessionFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * The rest interface to DZone and other services
 */
@RestController
public class DzoneZLController {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DzoneZLController.class);

    /**
     * The width of the image we download from DZone. This is just some ridiculous
     * number to get the maximum image size.
     */
    private static final int IMAGE_WIDTH = 1000000000;

    private static final String DZONE_TOPIC_SEARCH = "https://dzone.com/services/internal/data/topics-search?term=";

    /**
     * The alchemy api keyword extraction service
     */
    private static final String ALCHEMY_KEYWORD_ENDPOINT = "http://gateway-a.watsonplatform.net/calls/url/URLGetRankedKeywords";

    private static final String ALCHEMY_API_KEY = "7fad6119690b6b44f6f58cc2ce86ec842d7d1647";

    private static final String WATSON_USERNAME = "f677db8b-0179-40dc-9c14-6ed1f9cc5e02";

    private static final String WATSON_PASSWORD = "6K0Qe7mmulGZ";

    private static final String WATSON_CLASSIFIER_ID = "3a84dfx64-nlc-1569";

    private static final String CLASSIFIER_URL = "https://watson-api-explorer.mybluemix.net/natural-language-classifier/api/v1/classifiers/" + WATSON_CLASSIFIER_ID + "/classify";

    private static final int MAX_CLASSIFIER_SOURCE_LENGTH = 900;

    private static final int MAX_KEYWORDS = 10;

    @Autowired
    private HttpEntityUtils httpEntityUtils;

    @Autowired
    @Qualifier("dZoneContentExtractor")
    private ContentExtractor dZoneContentExtractor;

    @Autowired
    @Qualifier("readabilityContentExtractor")
    private ContentExtractor readabilityContentExtractor;

    @Autowired
    @Qualifier("boilerpipeContentExtractor")
    private ContentExtractor boilerpipeContentExtractor;

    @Autowired
    @Qualifier("mozillaReadabilityContentExtractor")
    private ContentExtractor mozillaReadabilityContentExtractor;

    @Autowired
    private HtmlSanitiser htmlSanitiser;

    @Autowired
    private EntityManagerFactory emf;

    private MultivaluedMap<String, String> fromMap(final Map<String, String> input) {
        return new MultivaluedHashMap<String, String>(input);
    }

    /**
     * Expose the database entities via Elide as a JSON API GET operation
     * @param allRequestParams
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE,
            value={"/data/{entity}", "/data/{entity}/{id}/relationships/{entity2}", "/data/{entity}/{id}/{child}", "/data/{entity}/{id}"})
    @Transactional
    public String jsonApiGet(@RequestParam final Map<String, String> allRequestParams, final HttpServletRequest request) {
        final String restOfTheUrl = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        final SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);

        /* Takes a hibernate session factory */
        final DataStore dataStore = new HibernateStore(sessionFactory);
        final Logger logger = new Slf4jLogger();
        final Elide elide = new Elide(logger, dataStore);
        final MultivaluedMap<String, String> params = fromMap(allRequestParams);

        final String path = restOfTheUrl.replaceAll("^/data/", "");

        final ElideResponse response = elide.get(path, params, new Object());

        return response.getBody();
    }

    /**
     *  Expose the database entities via Elide as a JSON API POST operation
     * @param body
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            value={"/data/{entity}"})
    @Transactional
    public String jsonApiPost(@RequestBody final String body, final HttpServletRequest request) {
        final String restOfTheUrl = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        final SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);

        /* Takes a hibernate session factory */
        final DataStore dataStore = new HibernateStore(sessionFactory);
        final Logger logger = new Slf4jLogger();
        final Elide elide = new Elide(logger, dataStore);

        final ElideResponse response = elide.post(
                restOfTheUrl.replaceAll("^/data/", ""),
                body,
                new Object(),
                SecurityMode.SECURITY_INACTIVE);

        return response.getBody();
    }

    /**
     *  Expose the database entities via Elide as a JSON API PATCH operation
     * @param body
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(
            method = RequestMethod.PATCH,
            produces = MediaType.APPLICATION_JSON_VALUE,
            value={"/data/{entity}/{entityid}"})
    @Transactional
    public String jsonApiPatch(@RequestBody final String body, final HttpServletRequest request) {
        final String restOfTheUrl = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        final SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);

        /* Takes a hibernate session factory */
        final DataStore dataStore = new HibernateStore(sessionFactory);
        final Logger logger = new Slf4jLogger();
        final Elide elide = new Elide(logger, dataStore);

        final ElideResponse response = elide.patch(
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                restOfTheUrl.replaceAll("^/data/", ""),
                body,
                new Object(),
                SecurityMode.SECURITY_INACTIVE);

        return response.getBody();
    }

    /**
     * Converts HTML to plain text
     * @param body
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(
            method = RequestMethod.POST,
            produces = MediaType.TEXT_PLAIN_VALUE,
            value={"/action/htmlToText"})
    public String htmlToText(@RequestBody final String body, final HttpServletRequest request) {
        final Source source = new Source(body);
        source.fullSequentialParse();
        final String text = source.getTextExtractor().setIncludeAttributes(true).toString();
        return text;
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

        checkArgument(StringUtils.isNotBlank(username));
        checkArgument(StringUtils.isNotBlank(password));

        /*
            Do the initial login to get any security cookies
         */
        final HttpGet initialGet = new HttpGet("https://dzone.com");
        final HttpResponse initialResponse = httpEntityUtils.makeRequest(initialGet);
        final Optional<String> awselbCookie = httpEntityUtils.getCookie(initialResponse, Constants.AWSELB_COOKIE);
        final Optional<String> thCsrfCookie = httpEntityUtils.getCookie(initialResponse, Constants.TH_CSRF_COOKIE);
        final Optional<String> jSessionIdCookie = httpEntityUtils.getCookie(initialResponse, Constants.JSESSIONID_COOKIE);

        /*
            Ensure we have what we need to continue
         */
        checkState(awselbCookie.isPresent());
        checkState(thCsrfCookie.isPresent());
        checkState(jSessionIdCookie.isPresent());

        /*
            Now we do the actual login
         */
        final String loginJson = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

        final HttpPost httppost = new HttpPost("https://dzone.com/services/internal/action/users-login");
        httppost.setHeader(Constants.COOKIE_HEADER,
            Constants.AWSELB_COOKIE + "=" + awselbCookie.get() + "; " +
            Constants.TH_CSRF_COOKIE + "=" + thCsrfCookie.get() + "; " +
            Constants.JSESSIONID_COOKIE + "=" + jSessionIdCookie.get() + "; " +
            Constants.SESSION_STARTED_COOKIE + "=true");

        final StringEntity requestEntity = new StringEntity(loginJson);
        requestEntity.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httppost.setEntity(requestEntity);

        httppost.addHeader(Constants.X_TH_CSRF_HEADER, thCsrfCookie.get());
        httppost.addHeader(Constants.ACCEPT_HEADER, MediaType.APPLICATION_JSON_VALUE);

        final CloseableHttpClient httpclient = HttpClients.createDefault();
        final CloseableHttpResponse loginResponse = httpclient.execute(httppost);
        try {
            final String responseBody = httpEntityUtils.responseToString(loginResponse.getEntity());

            LOGGER.info(responseBody);
        } finally {
            loginResponse.close();
        }

        final Optional<String> springSecurityCookie = httpEntityUtils.getCookie(loginResponse, Constants.SPRING_SECUITY_COOKIE);

        /*
            We need this cookie to continue
         */
        checkState(springSecurityCookie.isPresent());

        return "{\"" + Constants.AWSELB_COOKIE + "\": \"" + awselbCookie.get() + "\", " +
               "\"" + Constants.TH_CSRF_COOKIE + "\": \"" + thCsrfCookie.get() + "\", " +
                "\"" + Constants.JSESSIONID_COOKIE + "\": \"" + jSessionIdCookie.get() + "\", " +
                "\"" + Constants.SPRING_SECUITY_COOKIE + "\": \"" + springSecurityCookie.get() + "\"}";

    }

    /**
     *
     * @param awselbCookie The AWS ELB cookie used to access DZone
     * @param thCsrfCookie The CSRF cookie used to access DZone
     * @param springSecurityCookie The Spring security cookie used to access DZone
     * @param jSessionIdCookie  The DZone JSESSION ID cookie
     * @param url The URL that we want to import
     * @return
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/action/import", produces = MediaType.APPLICATION_JSON_VALUE)
    public ContentImport importPost(
        @RequestParam final String awselbCookie,
        @RequestParam final String thCsrfCookie,
        @RequestParam final String springSecurityCookie,
        @RequestParam final String jSessionIdCookie,
        @RequestParam final String url) throws IOException {

        checkArgument(StringUtils.isNotBlank(awselbCookie));
        checkArgument(StringUtils.isNotBlank(thCsrfCookie));
        checkArgument(StringUtils.isNotBlank(springSecurityCookie));
        checkArgument(StringUtils.isNotBlank(jSessionIdCookie));
        checkArgument(StringUtils.isNotBlank(url));

        final Map<String, String> dzoneData = new HashMap<String, String>();
        dzoneData.put(Constants.AWSELB_COOKIE, awselbCookie);
        dzoneData.put(Constants.TH_CSRF_COOKIE, thCsrfCookie);
        dzoneData.put(Constants.JSESSIONID_COOKIE, jSessionIdCookie);
        dzoneData.put(Constants.SPRING_SECUITY_COOKIE, springSecurityCookie);

        final Map<String, String> readabilityData = new HashMap<String, String>();
        readabilityData.put(Constants.READABILITY_TOKEN_NAME, Constants.READABILITY_TOKEN);

        /*
            Try the different importers one after the other
         */
        final ContentImport extractArticle = dZoneContentExtractor.extractContent(url, dzoneData).orElse(
            readabilityContentExtractor.extractContent(url, readabilityData).orElse(
                mozillaReadabilityContentExtractor.extractContent(url, null).orElse(
                    boilerpipeContentExtractor.extractContent(url, null).orElse(
                        new ContentImport()
                    )
                )
            )
        );

        extractArticle.setContent(htmlSanitiser.sanitiseHtml(extractArticle.getContent()));

        return extractArticle;
    }

    /**
     * Posts an article to DZone, and returns the json returned by DZone
     * @param awselbCookie The AWS ELB cookie used to access DZone
     * @param thCsrfCookie The CSRF cookie used to access DZone
     * @param springSecurityCookie The Spring security cookie used to access DZone
     * @param jSessionIdCookie  The DZone JSESSION ID cookie
     * @param title The artile title
     * @param content The article content
     * @param url The originalo article url
     * @param topics The topics associated with the article
     * @param authors The authors of the article
     * @param poster The DZone user submitting the article
     * @param image The image id associated with the article
     * @return
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/action/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    public String submitPost(
            @RequestParam final String awselbCookie,
            @RequestParam final String thCsrfCookie,
            @RequestParam final String springSecurityCookie,
            @RequestParam final String jSessionIdCookie,
            @RequestParam final String title,
            @RequestParam final String tldr,
            @RequestParam final String content,
            @RequestParam final String url,
            @RequestParam final String topics,
            @RequestParam final String authors,
            @RequestParam final Integer poster,
            @RequestParam final Integer image) throws IOException {

        checkArgument(StringUtils.isNotBlank(awselbCookie));
        checkArgument(StringUtils.isNotBlank(thCsrfCookie));
        checkArgument(StringUtils.isNotBlank(springSecurityCookie));
        checkArgument(StringUtils.isNotBlank(jSessionIdCookie));
        checkArgument(StringUtils.isNotBlank(url));
        checkArgument(StringUtils.isNotBlank(topics));
        checkArgument(StringUtils.isNotBlank(authors));
        checkArgument(StringUtils.isNotBlank(tldr));
        checkNotNull(poster);
        checkNotNull(image);

        final Optional<String> newImageId = uploadImage(
                awselbCookie,
                thCsrfCookie,
                springSecurityCookie,
                jSessionIdCookie,
                image,
                httpEntityUtils);

        if (newImageId.isPresent()) {

            final List<String> topicsSplit =  Lists.newArrayList(
                    Splitter.on(',')
                    .trimResults()
                    .omitEmptyStrings()
                    .split(topics)
            );

            final StringBuilder topicsArray = new StringBuilder();
            for (final String topic : topicsSplit) {
                if (topicsArray.length() != 0) {
                    topicsArray.append(",");
                }
                topicsArray.append("\"" + StringEscapeUtils.escapeJson(topic.trim()) + "\"");
            }

            final String submitBody =
                    "{\"type\":\"article\"," +
                            "\"title\":\"" + StringEscapeUtils.escapeJson(title) + "\"," +
                            "\"body\":\"" + StringEscapeUtils.escapeJson(content) + "\"," +
                            "\"topics\":[" + topicsArray.toString() + "]," +
                            "\"portal\":null," +
                            "\"thumb\":" + StringEscapeUtils.escapeJson(newImageId.get()) + "," +
                            "\"sources\":[]," +
                            "\"notes\":\"\"," +
                            "\"editorsPick\":false," +
                            "\"metaDescription\":\"\"," +
                            "\"tldr\":\"" + StringEscapeUtils.escapeJson(tldr) + "\"," +
                            "\"originalSource\":\"" + StringEscapeUtils.escapeJson(url) + "\"," +
                            "\"visibility\":\"draft\"}";

            final HttpPut httppost = new HttpPut("https://dzone.com/services/internal/ctype/article");
            httppost.setHeader(Constants.COOKIE_HEADER,
                    Constants.AWSELB_COOKIE + "=" + awselbCookie + "; " +
                    Constants.TH_CSRF_COOKIE + "=" + thCsrfCookie + "; " +
                    Constants.SPRING_SECUITY_COOKIE + "=" + springSecurityCookie + "; " +
                    Constants.JSESSIONID_COOKIE + "=" + jSessionIdCookie + "; " +
                    Constants.SESSION_STARTED_COOKIE + "=true");

            final StringEntity requestEntity = new StringEntity(submitBody);
            requestEntity.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httppost.setEntity(requestEntity);

            httppost.addHeader(Constants.X_TH_CSRF_HEADER, thCsrfCookie);
            httppost.addHeader(Constants.ACCEPT_HEADER, MediaType.APPLICATION_JSON_VALUE);

            final CloseableHttpClient httpclient = HttpClients.createDefault();
            final CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                final String responseBody = httpEntityUtils.responseToString(response.getEntity());
                final Matcher idMatcher = Constants.ID_QUOTE_RE.matcher(responseBody);

                if (responseBody.indexOf(Constants.SUCCESS) != -1) {

                    if (idMatcher.find()) {
                        try {
                            final Integer articleId = Integer.parseInt(idMatcher.group("id"));

                            /*
                                Associate the poster with the article
                             */
                            if (!addPosterToArticle(
                                    articleId,
                                    poster,
                                    false,
                                    awselbCookie,
                                    thCsrfCookie,
                                    springSecurityCookie,
                                    jSessionIdCookie,
                                    httpEntityUtils)) {
                                throw new Exception("Failed to associated poster with article");
                            }

                            /*
                                Associate all the authors with the article
                             */
                            final String[] authorsCollection = authors.split(",");
                            for (final String author : authorsCollection) {
                                final Integer authorId = Integer.parseInt(author);
                                if (!addPosterToArticle(
                                        articleId,
                                        authorId,
                                        true,
                                        awselbCookie,
                                        thCsrfCookie,
                                        springSecurityCookie,
                                        jSessionIdCookie,
                                        httpEntityUtils)) {
                                    throw new Exception("Failed to associated author with article");
                                }
                            }

                        } catch (final Exception ex) {
                            /*
                                We didn't find the expected article id
                             */
                            LOGGER.error("Exception associating user to article", ex);
                            return Constants.FAILED_RESPONSE;
                        }
                    }

                }

                /*
                    Save the source URL
                 */
                saveContentUrl(url);

                LOGGER.info(responseBody);

                /*
                    Return failure to the client
                 */
                return responseBody;
            } finally {
                response.close();
            }
        } else {
            LOGGER.error("Failed to upload image");
        }

        return Constants.FAILED_RESPONSE;
    }

    /**
     * Uploads an image to dzone, and returns the image id
     * @param awselbCookie The AWS ELB cookie used to access DZone
     * @param thCsrfCookie The CSRF cookie used to access DZone
     * @param springSecurityCookie The Spring security cookie used to access DZone
     * @param jSessionIdCookie  The DZone JSESSION ID cookie
     * @param file The image that we want to upload
     * @return
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/action/uploadImage",
            method=RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public String uploadImageToDzone(
            @RequestParam final String awselbCookie,
            @RequestParam final String thCsrfCookie,
            @RequestParam final String springSecurityCookie,
            @RequestParam final String jSessionIdCookie,
            @RequestParam("file") MultipartFile file) throws IOException {

        checkArgument(StringUtils.isNotBlank(awselbCookie));
        checkArgument(StringUtils.isNotBlank(thCsrfCookie));
        checkArgument(StringUtils.isNotBlank(springSecurityCookie));
        checkArgument(StringUtils.isNotBlank(jSessionIdCookie));
        checkNotNull(file);

        /*
            1. Download the existing file
         */
        final File imageFile = File.createTempFile("dzoneTempImage", ".img");
        IOUtils.copy(
                file.getInputStream(),
                new FileOutputStream(imageFile));

        /*
            2. Get a tracking code
         */
        final Optional<String> trackingId = getImageUploadTrackingCode(
                awselbCookie,
                thCsrfCookie,
                springSecurityCookie,
                jSessionIdCookie,
                httpEntityUtils);

        /*
            3. Upload the file, and get the new image id
         */
        if (trackingId.isPresent()) {
            final Optional<String> newImageId = uploadImage(
                    awselbCookie,
                    thCsrfCookie,
                    springSecurityCookie,
                    jSessionIdCookie,
                    trackingId.get(),
                    imageFile,
                    httpEntityUtils);

            if (newImageId.isPresent()) {
                return newImageId.get();
            }
        }

        return "";
    }

    /**
     * Extracts the keywords in an article using the Alchemy API, and then matches these
     * to Dzone topics.
     * @param url The URL to parse for keywords
     * @return
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/action/getKeywords",
            method=RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<String> extractKeywords(
            @RequestParam final String url) throws IOException {

        checkArgument(StringUtils.isNotBlank(url));

        final List<String> keywords = getAlchemyKeywords(url);
        final Set<String> topics = filterAgainstDZoneTopics(keywords);

        return topics;

    }

    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/action/classifyContent",
            method=RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public String classifyContent(@RequestBody final String content) throws IOException {
        checkArgument(StringUtils.isNotBlank(content));

        final String fixedContent = StringUtils.left(Jsoup.parse(content).text(), MAX_CLASSIFIER_SOURCE_LENGTH)
                .replaceAll("\n", " ");

        final String encoding = Base64.getEncoder().encodeToString((WATSON_USERNAME + ":" + WATSON_PASSWORD).getBytes());

        final HttpPost httppost = new HttpPost(CLASSIFIER_URL);
        httppost.setHeader("Authorization", "Basic " + encoding);

        final StringEntity requestEntity = new StringEntity("{\"text\":\"" + StringEscapeUtils.escapeJson(fixedContent) + "\"}");
        requestEntity.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httppost.setEntity(requestEntity);

        final CloseableHttpClient httpclient = HttpClients.createDefault();
        final CloseableHttpResponse loginResponse = httpclient.execute(httppost);
        try {
            final String responseBody = httpEntityUtils.responseToString(loginResponse.getEntity());

            LOGGER.info(responseBody);

            return responseBody;
        } finally {
            loginResponse.close();
        }
    }

    /**
     * Break down a keyword that might be compund (like "REST API") into
     * a collection that include the original key word and any individual
     * words.
     * @param keyword The original possibly compound keyword
     * @return
     */
    private List<String> decomposeKeyword(final String keyword) {
        final List<String> decomposedKeywords = new ArrayList<String>();
        decomposedKeywords.add(keyword);
        if (keyword.indexOf(" ") != -1) {
            decomposedKeywords.addAll(Arrays.asList(keyword.split(" ")));
        }
        return decomposedKeywords;
    }

    /**
     *
     * @param topic The topic to be checked against the DZone database
     * @return true if the topic is in the DZone database, and false otherwise
     */
    private boolean isDzoneTopic(final String topic) {
        try {
            LOGGER.info("Attempting to match keyword " + topic);

            /*
                Build up the url to get the keywords
             */
            final String keywordUrl = DZONE_TOPIC_SEARCH + URLEncoder.encode(topic);

            final HttpGet keywordExtraction = new HttpGet(keywordUrl);

            final CloseableHttpClient httpclient = HttpClients.createDefault();

            try (final CloseableHttpResponse loginResponse = httpclient.execute(keywordExtraction)) {
                final HttpEntity responseEntity = loginResponse.getEntity();
                final String responseJson = httpEntityUtils.responseToString(responseEntity);
                final JSONObject jsonObject = new JSONObject(responseJson);
                final JSONObject jsonResult = jsonObject.getJSONObject("result");
                final JSONArray jsonData = jsonResult.getJSONArray("data");

                for (int index = 0; index < jsonData.length(); ++index) {
                    final String dzoneTopic = jsonData.getJSONObject(index).getString("title");
                    if (topic.equalsIgnoreCase(dzoneTopic)) {
                        LOGGER.info("Matched keyword " + topic);
                        return true;
                    }
                }
            }
        } catch (final IOException ex) {
            /*
                Do nothing and return false
             */
        }

        return false;
    }

    /**
     *
     * @param keywords A list of keywords, usually found in an article
     * @return The set of keywords that are also DZone topics
     * @throws IOException
     */
    private Set<String> filterAgainstDZoneTopics(final List<String> keywords) throws IOException {
        return keywords.parallelStream()
                .flatMap(e -> decomposeKeyword(e).stream())
                .filter(e -> isDzoneTopic(e))
                .collect(Collectors.toSet());
    }

    /**
     *
     * @param url The article URL
     * @return The list of keywords that Alchemy API found in the article
     * @throws IOException
     */
    private List<String> getAlchemyKeywords(final String url) throws IOException {
        final List<String> keywords = new ArrayList<String>();

        /*
            Build up the url to get the keywords
         */
        final String keywordUrl = ALCHEMY_KEYWORD_ENDPOINT + "?" +
                "url=" + URLEncoder.encode(url) +
                "&apikey=" + ALCHEMY_API_KEY +
                "&outputMode=json" +
                "&maxRetrieve=" + MAX_KEYWORDS;

        final HttpGet keywordExtraction = new HttpGet(keywordUrl);

        final CloseableHttpClient httpclient = HttpClients.createDefault();

        try (final CloseableHttpResponse loginResponse = httpclient.execute(keywordExtraction)) {
            final HttpEntity responseEntity = loginResponse.getEntity();
            final String responseJson = httpEntityUtils.responseToString(responseEntity);
            final JSONObject jsonObject = new JSONObject(responseJson);
            final JSONArray jsonKeywords = jsonObject.getJSONArray("keywords");

            for (int index = 0; index < jsonKeywords.length(); ++index) {
                keywords.add(jsonKeywords.getJSONObject(index).getString("text"));
            }
        }

        return keywords;
    }

    private Optional<String> getImageUploadTrackingCode(final String awselbCookie,
                                                        final String thCsrfCookie,
                                                        final String springSecurityCookie,
                                                        final String jSessionIdCookie,
                                                        final HttpEntityUtils httpEntityUtils) throws IOException {

        checkArgument(StringUtils.isNotBlank(awselbCookie));
        checkArgument(StringUtils.isNotBlank(thCsrfCookie));
        checkArgument(StringUtils.isNotBlank(springSecurityCookie));
        checkArgument(StringUtils.isNotBlank(jSessionIdCookie));
        checkNotNull(httpEntityUtils);

        final HttpGet getImageId = new HttpGet("https://dzone.com/services/internal/data/uploads-authorize?image=true&type=node");
        getImageId.setHeader(Constants.COOKIE_HEADER,
                Constants.AWSELB_COOKIE + "=" + awselbCookie + "; " +
                Constants.TH_CSRF_COOKIE + "=" + thCsrfCookie + "; " +
                Constants.JSESSIONID_COOKIE + "=" + jSessionIdCookie + "; " +
                Constants.SPRING_SECUITY_COOKIE + "=" + springSecurityCookie + "; " +
                Constants.SESSION_STARTED_COOKIE + "=true");

        getImageId.addHeader(Constants.X_TH_CSRF_HEADER, thCsrfCookie);
        getImageId.addHeader(Constants.ACCEPT_HEADER, MediaType.APPLICATION_JSON_VALUE);

        final CloseableHttpClient httpclient = HttpClients.createDefault();

        try (final CloseableHttpResponse loginResponse = httpclient.execute(getImageId)) {
            final HttpEntity responseEntity = loginResponse.getEntity();
            final String imageIdResponse = httpEntityUtils.responseToString(responseEntity);

            final Matcher dataMatcher = Constants.DATA_RE.matcher(imageIdResponse);
            if (dataMatcher.find()) {
                final String dataId = dataMatcher.group("data");
                return Optional.of(dataId);

            }
        }

        return Optional.empty();
    }

    private Optional<String> uploadImage(final String awselbCookie,
                                         final String thCsrfCookie,
                                         final String springSecurityCookie,
                                         final String jSessionIdCookie,
                                         final String tackingId,
                                         final File imageFile,
                                         final HttpEntityUtils httpEntityUtils) throws IOException {

        checkArgument(StringUtils.isNotBlank(awselbCookie));
        checkArgument(StringUtils.isNotBlank(thCsrfCookie));
        checkArgument(StringUtils.isNotBlank(springSecurityCookie));
        checkArgument(StringUtils.isNotBlank(jSessionIdCookie));
        checkArgument(StringUtils.isNotBlank(tackingId));
        checkNotNull(imageFile);
        checkNotNull(httpEntityUtils);

        final HttpPost uploadPost = new HttpPost("https://dzone.com/uploadFile.json?trackingId=" + tackingId);

        final FileBody fileBody = new FileBody(imageFile);

        final HttpEntity fileEntity = MultipartEntityBuilder.create()
            .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            .addPart("file", fileBody)
            .build();

        uploadPost.setEntity(fileEntity);

        uploadPost.setHeader(Constants.COOKIE_HEADER,
                Constants.AWSELB_COOKIE + "=" + awselbCookie + "; " +
                Constants.TH_CSRF_COOKIE + "=" + thCsrfCookie + "; " +
                Constants.SPRING_SECUITY_COOKIE + "=" + springSecurityCookie + "; " +
                Constants.JSESSIONID_COOKIE + "=" + jSessionIdCookie + "; " +
                Constants.SESSION_STARTED_COOKIE + "=true");

        uploadPost.addHeader(Constants.X_TH_CSRF_HEADER, thCsrfCookie);

        final CloseableHttpClient httpclient = HttpClients.createDefault();

        try (final CloseableHttpResponse response = httpclient.execute(uploadPost)){
            final String responseBody = httpEntityUtils.responseToString(response.getEntity());
            final Matcher idMatcher = Constants.ID_RE.matcher(responseBody);
            if (idMatcher.find()) {
                return Optional.of(idMatcher.group("id"));
            }
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
                             final Integer imageId,
                             final HttpEntityUtils httpEntityUtils) throws IOException {

        checkArgument(StringUtils.isNotBlank(awselbCookie));
        checkArgument(StringUtils.isNotBlank(thCsrfCookie));
        checkArgument(StringUtils.isNotBlank(springSecurityCookie));
        checkArgument(StringUtils.isNotBlank(jSessionIdCookie));
        checkNotNull(imageId);
        checkNotNull(httpEntityUtils);

        /*
            1. Download the existing file
         */
        final File imageFile = File.createTempFile("dzoneTempImage", ".img");
        IOUtils.copy(
                new URL("https://dz2cdn1.dzone.com/thumbnail?fid=" + imageId + "&w=" + IMAGE_WIDTH).openStream(),
                new FileOutputStream(imageFile));

        /*
            2. Get a tracking code
         */
        final Optional<String> trackingId = getImageUploadTrackingCode(
                awselbCookie,
                thCsrfCookie,
                springSecurityCookie,
                jSessionIdCookie,
                httpEntityUtils);

        /*
            3. Upload the file, and get the new image id
         */
        if (trackingId.isPresent()) {
            final Optional<String> newImageId = uploadImage(
                    awselbCookie,
                    thCsrfCookie,
                    springSecurityCookie,
                    jSessionIdCookie,
                    trackingId.get(),
                    imageFile,
                    httpEntityUtils);

            LOGGER.info("Image ID: " + newImageId);

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
                                        final String jSessionIdCookie,
                                       final HttpEntityUtils httpEntityUtils) throws IOException {

        checkArgument(StringUtils.isNotBlank(awselbCookie));
        checkArgument(StringUtils.isNotBlank(thCsrfCookie));
        checkArgument(StringUtils.isNotBlank(springSecurityCookie));
        checkArgument(StringUtils.isNotBlank(jSessionIdCookie));
        checkNotNull(user);
        checkNotNull(articleId);
        checkNotNull(httpEntityUtils);

        final String posterBody = author ?
                "{\"user\": " + user + ", \"type\": \"author\"}" :
                "{\"user\": " + user + ", \"type\": \"op\"}";

        final HttpPost posterAssignment = new HttpPost("https://dzone.com/services/internal/node/" + articleId + "/authors-addAuthor");
        posterAssignment.setHeader(Constants.COOKIE_HEADER,
                Constants.AWSELB_COOKIE + "=" + awselbCookie + "; " +
                Constants.TH_CSRF_COOKIE + "=" + thCsrfCookie + "; " +
                Constants.SPRING_SECUITY_COOKIE + "=" + springSecurityCookie + "; " +
                Constants.JSESSIONID_COOKIE + "=" + jSessionIdCookie + "; " +
                Constants.SESSION_STARTED_COOKIE + "=true");

        final StringEntity posterEntity = new StringEntity(posterBody);
        posterEntity.setContentType(MediaType.APPLICATION_JSON_VALUE);
        posterAssignment.setEntity(posterEntity);

        posterAssignment.addHeader(Constants.X_TH_CSRF_HEADER, thCsrfCookie);
        posterAssignment.addHeader(Constants.ACCEPT_HEADER, MediaType.APPLICATION_JSON_VALUE);

        final CloseableHttpClient httpclient = HttpClients.createDefault();

        try (final CloseableHttpResponse posterResponse = httpclient.execute(posterAssignment)) {
            final String responseBody = httpEntityUtils.responseToString(posterResponse.getEntity());

            LOGGER.info("Image Upload Response Body: " + responseBody);

            return responseBody.indexOf(Constants.SUCCESS) != -1;
        }
    }

    private void saveContentUrl(final String contentUrl) {
        EntityManager entityManager = null;
        EntityTransaction tx = null;
        try {
            entityManager = emf.createEntityManager();
            tx = entityManager.getTransaction();
            tx.begin();

            final Article article = new Article();
            article.setSource(contentUrl);
            entityManager.persist(article);

            tx.commit();
        } catch(final Exception ex) {
            tx.rollback();
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }
}

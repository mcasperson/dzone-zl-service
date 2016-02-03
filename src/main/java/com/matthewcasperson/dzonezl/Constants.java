package com.matthewcasperson.dzonezl;

import java.util.regex.Pattern;

/**
 * Created by Matthew on 3/02/2016.
 */
public class Constants {
    public static final String COOKIE_HEADER = "Cookie";
    public static final String ACCEPT_HEADER = "Accept";

    public static final String SPRING_SECUITY_COOKIE = "SPRING_SECURITY_REMEMBER_ME_COOKIE";
    public static final String AWSELB_COOKIE = "AWSELB";
    public static final String TH_CSRF_COOKIE = "TH_CSRF";
    public static final String JSESSIONID_COOKIE = "JSESSIONID";
    public static final String SESSION_STARTED_COOKIE = "SESSION_STARTED";
    public static final String X_TH_CSRF_HEADER = "X-TH-CSRF";

    public static final String FAILED_RESPONSE = "{\"success\":false}";

    public static final String READABILITY_TOKEN_NAME = "READABILITY_TOKEN_NAME";
    public static final String READABILITY_TOKEN = "a5e73f6164904e17aa8f37135d24801330cf2a1f";


    public static final String SUCCESS = "\"success\":true";
    public static final String EMPTY_IMPORT = "\"fullContent\":\"\"";
    public static final Pattern ID_RE = Pattern.compile("\"id\":(?<id>\\d+)");
    public static final Pattern ID_QUOTE_RE = Pattern.compile("\"id\":\"(?<id>\\d+)\"");
    public static final Pattern DATA_RE = Pattern.compile("\"data\":\"(?<data>.*?)\"");
}

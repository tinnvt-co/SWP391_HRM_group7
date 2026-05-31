package config;

public class GoogleOAuthConfig {

    public static final String CLIENT_ID     = "132031612293-1eu6r0nudgut0j9v9fulh3hdkc937136.apps.googleusercontent.com";
    public static final String CLIENT_SECRET = "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";
    public static final String REDIRECT_URI  = "http://localhost:8080/SWP391_HRM_group7/auth/google/callback";

    public static final String AUTH_URL      = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String TOKEN_URL     = "https://oauth2.googleapis.com/token";
    public static final String USERINFO_URL  = "https://www.googleapis.com/oauth2/v3/userinfo";
    public static final String SCOPE         = "openid email profile";
}

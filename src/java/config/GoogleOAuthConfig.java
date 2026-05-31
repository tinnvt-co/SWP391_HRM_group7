package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GoogleOAuthConfig {

    public static final String CLIENT_ID;
    public static final String CLIENT_SECRET;
    public static final String REDIRECT_URI;

    public static final String AUTH_URL      = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String TOKEN_URL     = "https://oauth2.googleapis.com/token";
    public static final String USERINFO_URL  = "https://www.googleapis.com/oauth2/v3/userinfo";
    public static final String SCOPE         = "openid email profile";

    static {
        Properties props = new Properties();
        try (InputStream in = GoogleOAuthConfig.class.getClassLoader()
                .getResourceAsStream("oauth.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignored) {}

        CLIENT_ID     = resolve(props, "google.client.id",     "GOOGLE_CLIENT_ID");
        CLIENT_SECRET = resolve(props, "google.client.secret", "GOOGLE_CLIENT_SECRET");
        REDIRECT_URI  = resolve(props, "google.redirect.uri",  "GOOGLE_REDIRECT_URI");
    }

    private static String resolve(Properties props, String propKey, String envKey) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env;
        String value = props.getProperty(propKey);
        return value != null ? value.trim() : "";
    }
}

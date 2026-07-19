package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MailConfig {

    public static final String SMTP_HOST;
    public static final String SMTP_PORT;
    public static final String USERNAME;
    public static final String PASSWORD;
    public static final String FROM_NAME;

    static {
        Properties props = new Properties();
        try (InputStream in = MailConfig.class.getClassLoader()
                .getResourceAsStream("mail.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignored) {}

        SMTP_HOST = resolve(props, "mail.smtp.host", "MAIL_SMTP_HOST", "smtp.gmail.com");
        SMTP_PORT = resolve(props, "mail.smtp.port", "MAIL_SMTP_PORT", "587");
        USERNAME  = resolve(props, "mail.username",  "MAIL_USERNAME",  "");
        PASSWORD  = normalizePassword(resolve(props, "mail.password", "MAIL_PASSWORD", ""));
        FROM_NAME = resolve(props, "mail.from.name", "MAIL_FROM_NAME", "HRM System");
    }

    private static String resolve(Properties props, String propKey, String envKey, String defaultValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env;
        String value = props.getProperty(propKey);
        return value != null && !value.isBlank() ? value.trim() : defaultValue;
    }

    private static String normalizePassword(String password) {
        return password == null ? "" : password.replaceAll("\\s+", "");
    }
}

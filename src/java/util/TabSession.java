package util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TabSession {

    public static final String PARAM_NAME = "_tab";
    public static final String HEADER_NAME = "X-HRM-Tab-Id";
    public static final String REQUEST_ATTRIBUTE = TabSession.class.getName() + ".tabId";

    private static final String ATTRIBUTE_PREFIX = "__hrm_tab_session__.";
    private static final Pattern SAFE_TAB_ID = Pattern.compile("[A-Za-z0-9_-]{8,80}");
    private static final Pattern OAUTH_STATE_TAB =
            Pattern.compile("^([A-Za-z0-9_-]{8,80})\\.[A-Za-z0-9_-]+(?:-[A-Za-z0-9_-]+)*$");

    private TabSession() {}

    public static String resolveTabId(HttpServletRequest request) {
        Object cached = request.getAttribute(REQUEST_ATTRIBUTE);
        if (cached instanceof String cachedTabId && isValidTabId(cachedTabId)) {
            return cachedTabId;
        }

        String tabId = firstValid(
                request.getParameter(PARAM_NAME),
                request.getHeader(HEADER_NAME),
                extractTabIdFromOAuthState(request.getParameter("state"))
        );

        if (tabId != null) {
            request.setAttribute(REQUEST_ATTRIBUTE, tabId);
        }
        return tabId;
    }

    public static String currentTabId(HttpServletRequest request) {
        Object tabId = request.getAttribute(REQUEST_ATTRIBUTE);
        return tabId instanceof String value && isValidTabId(value) ? value : null;
    }

    public static String scopedAttributeName(String tabId, String name) {
        if (tabId == null || name == null || !isTabScopedAttribute(name)) {
            return name;
        }
        return ATTRIBUTE_PREFIX + tabId + "." + name;
    }

    public static boolean isAttributeForTab(String tabId, String name) {
        return tabId != null && name != null && name.startsWith(ATTRIBUTE_PREFIX + tabId + ".");
    }

    public static String unscopedAttributeName(String tabId, String name) {
        String prefix = ATTRIBUTE_PREFIX + tabId + ".";
        return name != null && name.startsWith(prefix) ? name.substring(prefix.length()) : name;
    }

    public static boolean isTabScopedAttribute(String name) {
        return name != null
                && !name.startsWith(ATTRIBUTE_PREFIX)
                && !name.startsWith("jakarta.")
                && !name.startsWith("javax.")
                && !name.startsWith("org.apache.")
                && !name.startsWith("org.eclipse.")
                && !name.startsWith("com.sun.");
    }

    public static boolean isAuthAttribute(String name) {
        return "currentUser".equals(name) || "permissions".equals(name);
    }

    public static String oauthStateForTab(String tabId, String state) {
        return isValidTabId(tabId) ? tabId + "." + state : state;
    }

    public static String appendTabToRedirect(String location, HttpServletRequest request, String tabId) {
        if (!isValidTabId(tabId) || location == null || location.isBlank() || hasTabParameter(location)) {
            return location;
        }
        if (!isSameApplicationLocation(location, request)) {
            return location;
        }

        String fragment = "";
        String base = location;
        int fragmentIndex = location.indexOf('#');
        if (fragmentIndex >= 0) {
            base = location.substring(0, fragmentIndex);
            fragment = location.substring(fragmentIndex);
        }

        String separator = base.contains("?") ? "&" : "?";
        return base + separator + PARAM_NAME + "="
                + URLEncoder.encode(tabId, StandardCharsets.UTF_8) + fragment;
    }

    private static String firstValid(String... values) {
        for (String value : values) {
            if (isValidTabId(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isValidTabId(String value) {
        return value != null && SAFE_TAB_ID.matcher(value).matches();
    }

    private static String extractTabIdFromOAuthState(String state) {
        if (state == null) {
            return null;
        }
        Matcher matcher = OAUTH_STATE_TAB.matcher(state);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static boolean hasTabParameter(String location) {
        int queryIndex = location.indexOf('?');
        if (queryIndex < 0) {
            return false;
        }
        String query = location.substring(queryIndex + 1);
        int fragmentIndex = query.indexOf('#');
        if (fragmentIndex >= 0) {
            query = query.substring(0, fragmentIndex);
        }
        for (String part : query.split("&")) {
            int equalsIndex = part.indexOf('=');
            String name = equalsIndex >= 0 ? part.substring(0, equalsIndex) : part;
            if (PARAM_NAME.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSameApplicationLocation(String location, HttpServletRequest request) {
        String lower = location.toLowerCase();
        if (lower.startsWith("javascript:")
                || lower.startsWith("mailto:")
                || lower.startsWith("tel:")
                || location.startsWith("#")) {
            return false;
        }

        if (location.startsWith("//")) {
            return false;
        }

        if (location.startsWith(request.getContextPath() + "/")
                || location.equals(request.getContextPath())
                || location.startsWith("/")) {
            return true;
        }

        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return true;
        }

        try {
            URI uri = URI.create(location);
            String requestScheme = request.getScheme();
            String requestHost = request.getServerName();
            int requestPort = normalizedPort(requestScheme, request.getServerPort());
            int uriPort = normalizedPort(uri.getScheme(), uri.getPort());

            return requestScheme.equalsIgnoreCase(uri.getScheme())
                    && requestHost.equalsIgnoreCase(uri.getHost())
                    && requestPort == uriPort
                    && (uri.getPath() == null
                        || uri.getPath().equals(request.getContextPath())
                        || uri.getPath().startsWith(request.getContextPath() + "/"));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static int normalizedPort(String scheme, int port) {
        if (port > 0) {
            return port;
        }
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }
}

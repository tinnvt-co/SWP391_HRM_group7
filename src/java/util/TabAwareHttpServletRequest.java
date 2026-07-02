package util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;

public class TabAwareHttpServletRequest extends HttpServletRequestWrapper {

    private final String tabId;

    public TabAwareHttpServletRequest(HttpServletRequest request, String tabId) {
        super(request);
        this.tabId = tabId;
        if (tabId != null) {
            request.setAttribute(TabSession.REQUEST_ATTRIBUTE, tabId);
        }
    }

    @Override
    public HttpSession getSession(boolean create) {
        HttpSession session = super.getSession(create);
        return session == null ? null : new TabScopedHttpSession(session, tabId);
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }
}

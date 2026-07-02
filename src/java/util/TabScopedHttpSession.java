package util;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class TabScopedHttpSession implements HttpSession {

    private final HttpSession delegate;
    private final String tabId;

    public TabScopedHttpSession(HttpSession delegate, String tabId) {
        this.delegate = delegate;
        this.tabId = tabId;
    }

    @Override
    public long getCreationTime() {
        return delegate.getCreationTime();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public long getLastAccessedTime() {
        return delegate.getLastAccessedTime();
    }

    @Override
    public ServletContext getServletContext() {
        return delegate.getServletContext();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        delegate.setMaxInactiveInterval(interval);
    }

    @Override
    public int getMaxInactiveInterval() {
        return delegate.getMaxInactiveInterval();
    }

    @Override
    public Object getAttribute(String name) {
        Object value = delegate.getAttribute(TabSession.scopedAttributeName(tabId, name));
        if (value == null && tabId != null && TabSession.isAuthAttribute(name)) {
            return delegate.getAttribute(name);
        }
        return value;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        if (tabId == null) {
            return delegate.getAttributeNames();
        }

        List<String> names = new ArrayList<>();
        Enumeration<String> delegateNames = delegate.getAttributeNames();

        while (delegateNames.hasMoreElements()) {
            String name = delegateNames.nextElement();
            if (TabSession.isAttributeForTab(tabId, name)) {
                names.add(TabSession.unscopedAttributeName(tabId, name));
            } else if (!TabSession.isTabScopedAttribute(name)) {
                names.add(name);
            }
        }

        return java.util.Collections.enumeration(names);
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value == null) {
            removeAttribute(name);
            return;
        }
        delegate.setAttribute(TabSession.scopedAttributeName(tabId, name), value);
        if (tabId != null && TabSession.isAuthAttribute(name)) {
            delegate.setAttribute(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        delegate.removeAttribute(TabSession.scopedAttributeName(tabId, name));
        if (tabId != null && TabSession.isAuthAttribute(name)) {
            delegate.removeAttribute(name);
        }
    }

    @Override
    public void invalidate() {
        if (tabId == null) {
            delegate.invalidate();
            return;
        }

        List<String> namesToRemove = new ArrayList<>();
        Enumeration<String> names = delegate.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (TabSession.isAttributeForTab(tabId, name)) {
                namesToRemove.add(name);
            }
        }
        for (String name : namesToRemove) {
            delegate.removeAttribute(name);
        }
    }

    @Override
    public boolean isNew() {
        return delegate.isNew();
    }
}

package util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;

public class TabAwareHttpServletResponse extends HttpServletResponseWrapper {

    private final HttpServletRequest request;
    private final String tabId;

    public TabAwareHttpServletResponse(HttpServletResponse response,
                                       HttpServletRequest request,
                                       String tabId) {
        super(response);
        this.request = request;
        this.tabId = tabId;
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        super.sendRedirect(TabSession.appendTabToRedirect(location, request, tabId));
    }
}

package filter;

import model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebFilter(filterName = "AuthFilter", urlPatterns = {"/*"})
public class AuthFilter implements Filter {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/",
            "/index.jsp",
            "/login",
            "/logout",
            "/forgot-password",
            "/auth/google"
    );

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/assets/"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String contextPath  = request.getContextPath();
        String relativePath = request.getRequestURI().substring(contextPath.length());

        applyNoCacheHeaders(response);

        if (isPublicPath(relativePath)) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session  = request.getSession(false);
        User currentUser     = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            response.sendRedirect(contextPath + "/login");
            return;
        }

        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {}

    private boolean isPublicPath(String path) {
        if (path == null || path.isEmpty()) return true;
        for (String pub : PUBLIC_PATHS) {
            if (path.equals(pub)) return true;
        }
        for (String pub : PUBLIC_PREFIXES) {
            if (path.startsWith(pub)) return true;
        }
        return false;
    }

    private void applyNoCacheHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}

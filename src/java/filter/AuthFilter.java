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
        for (String pub : PUBLIC_PATHS) {
            if (path.startsWith(pub)) return true;
        }
        return false;
    }
}

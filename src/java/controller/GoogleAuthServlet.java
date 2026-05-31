package controller;

import config.GoogleOAuthConfig;
import dao.PermissionDAO;
import dao.UserDAO;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "GoogleAuthServlet", urlPatterns = {"/auth/google", "/auth/google/callback"})
public class GoogleAuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final PermissionDAO permissionDAO = new PermissionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if ("/auth/google".equals(path)) {
            handleAuthorize(request, response);
        } else if ("/auth/google/callback".equals(path)) {
            handleCallback(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/login");
        }
    }

    private void handleAuthorize(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String state = UUID.randomUUID().toString();
        request.getSession().setAttribute("oauthState", state);

        String url = GoogleOAuthConfig.AUTH_URL
                + "?client_id="     + URLEncoder.encode(GoogleOAuthConfig.CLIENT_ID, StandardCharsets.UTF_8)
                + "&redirect_uri="  + URLEncoder.encode(GoogleOAuthConfig.REDIRECT_URI, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope="         + URLEncoder.encode(GoogleOAuthConfig.SCOPE, StandardCharsets.UTF_8)
                + "&state="         + URLEncoder.encode(state, StandardCharsets.UTF_8)
                + "&access_type=online"
                + "&prompt=select_account";

        response.sendRedirect(url);
    }

    private void handleCallback(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String error = request.getParameter("error");
        if (error != null) {
            forwardLoginError(request, response, "Google sign-in was cancelled.");
            return;
        }

        String code  = request.getParameter("code");
        String state = request.getParameter("state");

        HttpSession session = request.getSession(false);
        String savedState = session != null ? (String) session.getAttribute("oauthState") : null;

        if (code == null || code.isBlank() || state == null || savedState == null || !state.equals(savedState)) {
            forwardLoginError(request, response, "Invalid Google sign-in request.");
            return;
        }
        session.removeAttribute("oauthState");

        try {
            String accessToken = exchangeCodeForToken(code);
            if (accessToken == null) {
                forwardLoginError(request, response, "Could not retrieve token from Google.");
                return;
            }

            String email = fetchEmail(accessToken);
            if (email == null || email.isBlank()) {
                forwardLoginError(request, response, "Could not retrieve email from Google.");
                return;
            }

            User user = userDAO.findByEmail(email);
            if (user == null) {
                forwardLoginError(request, response, "No account is linked with this Google email.");
                return;
            }
            if (!user.isActive()) {
                forwardLoginError(request, response, "This account has been deactivated.");
                return;
            }

            List<String> permissions = permissionDAO.findCodesByUserId(user.getUserId());
            userDAO.updateLastLogin(user.getUserId());

            HttpSession newSession = request.getSession();
            newSession.setAttribute("currentUser", user);
            newSession.setAttribute("permissions", permissions);

            response.sendRedirect(request.getContextPath() + "/home");

        } catch (SQLException e) {
            e.printStackTrace();
            forwardLoginError(request, response, "System error, please try again.");
        }
    }

    private String exchangeCodeForToken(String code) throws IOException {
        String body = "code="          + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&client_id="    + URLEncoder.encode(GoogleOAuthConfig.CLIENT_ID, StandardCharsets.UTF_8)
                    + "&client_secret="+ URLEncoder.encode(GoogleOAuthConfig.CLIENT_SECRET, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(GoogleOAuthConfig.REDIRECT_URI, StandardCharsets.UTF_8)
                    + "&grant_type=authorization_code";

        HttpURLConnection conn = (HttpURLConnection) URI.create(GoogleOAuthConfig.TOKEN_URL).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200) return null;

        String json = readResponse(conn);
        return extractJsonValue(json, "access_token");
    }

    private String fetchEmail(String accessToken) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(GoogleOAuthConfig.USERINFO_URL).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        if (conn.getResponseCode() != 200) return null;

        String json = readResponse(conn);
        return extractJsonValue(json, "email");
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private String extractJsonValue(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }

    private void forwardLoginError(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.getRequestDispatcher("/views/auth/login.jsp").forward(request, response);
    }
}

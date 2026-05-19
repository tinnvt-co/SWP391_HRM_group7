package controller;

import dao.PasswordResetTokenDAO;
import dao.UserDAO;
import model.PasswordResetToken;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

@WebServlet(name = "ForgotPasswordServlet", urlPatterns = {"/forgot-password"})
public class ForgotPasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        if (token != null && !token.isBlank()) {
            try {
                PasswordResetToken prt = tokenDAO.findByToken(token);
                if (prt == null || prt.isUsed() || prt.isExpired()) {
                    request.setAttribute("error", "Reset link is invalid or has expired.");
                    request.getRequestDispatcher("/views/auth/forgot-password.jsp").forward(request, response);
                    return;
                }
                request.setAttribute("token", token);
                request.getRequestDispatcher("/views/auth/reset-password.jsp").forward(request, response);
            } catch (SQLException e) {
                request.setAttribute("error", "Something went wrong. Please try again.");
                request.getRequestDispatcher("/views/auth/forgot-password.jsp").forward(request, response);
            }
        } else {
            request.getRequestDispatcher("/views/auth/forgot-password.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("request".equals(action)) {
            handleRequest(request, response);
        } else if ("reset".equals(action)) {
            handleReset(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/forgot-password");
        }
    }

    private void handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");

        if (email == null || email.isBlank()) {
            request.setAttribute("error", "Please enter your email address.");
            request.getRequestDispatcher("/views/auth/forgot-password.jsp").forward(request, response);
            return;
        }

        try {
            User user = userDAO.findByEmail(email.trim());
            if (user == null) {
                request.setAttribute("error", "No account found with that email address.");
                request.setAttribute("email", email);
                request.getRequestDispatcher("/views/auth/forgot-password.jsp").forward(request, response);
                return;
            }

            tokenDAO.invalidateAllForUser(user.getUserId());

            String rawToken = UUID.randomUUID().toString();
            PasswordResetToken prt = new PasswordResetToken();
            prt.setUserId(user.getUserId());
            prt.setResetToken(rawToken);
            prt.setExpiredAt(LocalDateTime.now().plusMinutes(15));
            tokenDAO.insert(prt);

            String resetLink = request.getScheme() + "://" + request.getServerName()
                    + ":" + request.getServerPort()
                    + request.getContextPath()
                    + "/forgot-password?token=" + rawToken;

            request.setAttribute("resetLink", resetLink);
            request.setAttribute("success", "Reset link generated.");
            request.getRequestDispatcher("/views/auth/forgot-password.jsp").forward(request, response);

        } catch (SQLException e) {
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.getRequestDispatcher("/views/auth/forgot-password.jsp").forward(request, response);
        }
    }

    private void handleReset(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token     = request.getParameter("token");
        String newPass   = request.getParameter("newPassword");
        String confirmPass = request.getParameter("confirmPassword");

        if (newPass == null || newPass.isBlank() || confirmPass == null || confirmPass.isBlank()) {
            request.setAttribute("error", "Please fill in all fields.");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/views/auth/reset-password.jsp").forward(request, response);
            return;
        }

        if (!newPass.equals(confirmPass)) {
            request.setAttribute("error", "Passwords do not match.");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/views/auth/reset-password.jsp").forward(request, response);
            return;
        }

        try {
            PasswordResetToken prt = tokenDAO.findByToken(token);
            if (prt == null || prt.isUsed() || prt.isExpired()) {
                request.setAttribute("error", "Reset link is invalid or has expired.");
                request.getRequestDispatcher("/views/auth/forgot-password.jsp").forward(request, response);
                return;
            }

            userDAO.updatePassword(prt.getUserId(), newPass);
            tokenDAO.markAsUsed(prt.getTokenId());

            response.sendRedirect(request.getContextPath() + "/login?reset=success");

        } catch (SQLException e) {
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/views/auth/reset-password.jsp").forward(request, response);
        }
    }
}

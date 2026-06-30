package controller;

import dao.PermissionDAO;
import dao.UserDAO;
import model.User;
import util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final PermissionDAO permissionDAO = new PermissionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }
        request.getRequestDispatcher("/views/auth/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            request.setAttribute("error", "Please enter your username and password.");
            request.getRequestDispatcher("/views/auth/login.jsp").forward(request, response);
            return;
        }

        try {
            User user = userDAO.findActiveByUsername(username.trim());
            if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
                request.setAttribute("error", "Invalid username or password.");
                request.setAttribute("username", username);
                request.getRequestDispatcher("/views/auth/login.jsp").forward(request, response);
                return;
            }

            if (PasswordUtil.needsRehash(user.getPasswordHash())) {
                String hashedPassword = PasswordUtil.hash(password);
                userDAO.updatePassword(user.getUserId(), hashedPassword);
                user.setPasswordHash(hashedPassword);
            }

            List<String> permissions = permissionDAO.findCodesByUserId(user.getUserId());
            userDAO.updateLastLogin(user.getUserId());

            HttpSession session = request.getSession();
            session.setAttribute("currentUser", user);
            session.setAttribute("permissions", permissions);

            response.sendRedirect(request.getContextPath() + "/home");

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "System error, please try again.");
            request.getRequestDispatcher("/views/auth/login.jsp").forward(request, response);
        }
    }
}

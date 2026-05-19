package controller;

import dao.PermissionDAO;
import dao.UserDAO;
import model.User;

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
            request.setAttribute("error", "Vui lòng nhập đầy đủ tài khoản và mật khẩu.");
            request.getRequestDispatcher("/views/auth/login.jsp").forward(request, response);
            return;
        }

        try {
            User user = userDAO.findByUsernameAndPassword(username.trim(), password);
            if (user == null) {
                request.setAttribute("error", "Tài khoản hoặc mật khẩu không đúng.");
                request.setAttribute("username", username);
                request.getRequestDispatcher("/views/auth/login.jsp").forward(request, response);
                return;
            }

            List<String> permissions = permissionDAO.findCodesByUserId(user.getUserId());
            userDAO.updateLastLogin(user.getUserId());

            HttpSession session = request.getSession();
            session.setAttribute("currentUser", user);
            session.setAttribute("permissions", permissions);

            response.sendRedirect(request.getContextPath() + "/home");

        } catch (SQLException e) {
            request.setAttribute("error", "Hệ thống gặp lỗi, vui lòng thử lại.");
            request.getRequestDispatcher("/views/auth/login.jsp").forward(request, response);
        }
    }
}

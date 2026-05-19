package controller;

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

@WebServlet(name = "ChangePasswordServlet", urlPatterns = {"/change-password"})
public class ChangePasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/views/profile/change-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session   = request.getSession(false);
        User currentUser      = (User) session.getAttribute("currentUser");

        String currentPass    = request.getParameter("currentPassword");
        String newPass        = request.getParameter("newPassword");
        String confirmPass    = request.getParameter("confirmPassword");

        if (currentPass == null || currentPass.isBlank()
                || newPass == null || newPass.isBlank()
                || confirmPass == null || confirmPass.isBlank()) {
            request.setAttribute("error", "Please fill in all fields.");
            request.getRequestDispatcher("/views/profile/change-password.jsp").forward(request, response);
            return;
        }

        if (!newPass.equals(confirmPass)) {
            request.setAttribute("error", "New password and confirmation do not match.");
            request.getRequestDispatcher("/views/profile/change-password.jsp").forward(request, response);
            return;
        }

        if (newPass.length() < 6) {
            request.setAttribute("error", "New password must be at least 6 characters.");
            request.getRequestDispatcher("/views/profile/change-password.jsp").forward(request, response);
            return;
        }

        try {
            User dbUser = userDAO.findByUsernameAndPassword(currentUser.getUsername(), currentPass);
            if (dbUser == null) {
                request.setAttribute("error", "Current password is incorrect.");
                request.getRequestDispatcher("/views/profile/change-password.jsp").forward(request, response);
                return;
            }

            userDAO.updatePassword(currentUser.getUserId(), newPass);

            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/login?changed=success");

        } catch (SQLException e) {
            request.setAttribute("error", "Something went wrong. Please try again.");
            request.getRequestDispatcher("/views/profile/change-password.jsp").forward(request, response);
        }
    }
}

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

@WebServlet(name = "ProfileServlet", urlPatterns = {"/profile"})
public class ProfileServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User sessionUser = (User) session.getAttribute("currentUser");

        try {
            User user = userDAO.findById(sessionUser.getUserId());
            request.setAttribute("user", user);
            request.getRequestDispatcher("/views/profile/view-profile.jsp").forward(request, response);
        } catch (SQLException e) {
            request.setAttribute("user", sessionUser);
            request.getRequestDispatcher("/views/profile/view-profile.jsp").forward(request, response);
        }
    }
}

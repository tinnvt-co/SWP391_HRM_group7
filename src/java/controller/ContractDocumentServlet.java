package controller;

import dao.ContractDocumentDAO;
import model.ContractDocument;
import model.User;
import service.ContractDocumentStorage;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "ContractDocumentServlet", urlPatterns = {"/contract-document"})
public class ContractDocumentServlet extends HttpServlet {

    private final ContractDocumentDAO documentDAO = new ContractDocumentDAO();
    private final ContractDocumentStorage storage = new ContractDocumentStorage();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer documentId = parseIntOrNull(request.getParameter("id"));
        if (documentId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            ContractDocument document = documentDAO.findById(documentId);
            if (document == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (!canView(request, document)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            Path file = storage.resolve(getServletContext(), document.getRelativePath());
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            response.setContentType(document.getMimeType());
            response.setHeader("Content-Disposition",
                    "inline; filename*=UTF-8''" + URLEncoder.encode(
                            document.getOriginalFileName(), StandardCharsets.UTF_8).replace("+", "%20"));
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setContentLengthLong(Files.size(file));
            Files.copy(file, response.getOutputStream());
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }

    private boolean canView(HttpServletRequest request, ContractDocument document) {
        User user = currentUser(request);
        if (user == null) return false;
        if (document.getEmployeeUserId() == user.getUserId()
                && hasPermission(request, "VIEW_MY_CONTRACT")) {
            return true;
        }
        return hasPermission(request, "VIEW_CONTRACT_LIST");
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("currentUser");
    }

    private boolean hasPermission(HttpServletRequest request, String permCode) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        List<?> perms = (List<?>) session.getAttribute("permissions");
        return perms != null && perms.contains(permCode);
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException ex) { return null; }
    }
}

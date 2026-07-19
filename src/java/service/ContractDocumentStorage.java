package service;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;
import model.ContractDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class ContractDocumentStorage {

    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");

    public ContractDocument save(ServletContext context, Part part, String namespace, Integer uploadedBy)
            throws IOException {
        if (part == null || part.getSize() <= 0) {
            throw new IOException("Contract document is required.");
        }
        if (part.getSize() > MAX_SIZE) {
            throw new IOException("Contract document must be 10 MB or smaller.");
        }

        String originalName = fileName(part.getSubmittedFileName());
        String extension = extension(originalName);
        String mimeType = normalizeMimeType(part.getContentType(), extension);
        if (!ALLOWED_EXTENSIONS.contains(extension) || mimeType == null) {
            throw new IOException("Only PDF, JPG, JPEG, and PNG contract documents are accepted.");
        }

        String cleanNamespace = sanitizePathPart(namespace);
        String storedName = UUID.randomUUID() + "." + extension;
        String relativePath = cleanNamespace + "/" + storedName;
        Path target = root(context).resolve(relativePath).normalize();
        if (!target.startsWith(root(context))) {
            throw new IOException("Invalid contract document path.");
        }

        Files.createDirectories(target.getParent());
        try (var input = part.getInputStream()) {
            Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        ContractDocument doc = new ContractDocument();
        doc.setOriginalFileName(originalName);
        doc.setStoredFileName(storedName);
        doc.setRelativePath(relativePath.replace('\\', '/'));
        doc.setMimeType(mimeType);
        doc.setFileSize(part.getSize());
        doc.setUploadedBy(uploadedBy);
        return doc;
    }

    public Path resolve(ServletContext context, String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IOException("Missing contract document path.");
        }
        Path root = root(context);
        Path resolved = root.resolve(relativePath.replace('/', java.io.File.separatorChar)).normalize();
        if (!resolved.startsWith(root)) {
            throw new IOException("Invalid contract document path.");
        }
        return resolved;
    }

    private Path root(ServletContext context) throws IOException {
        String realPath = context.getRealPath("/WEB-INF/uploads/contracts");
        if (realPath == null) {
            throw new IOException("Contract document storage is not available.");
        }
        return Paths.get(realPath).toAbsolutePath().normalize();
    }

    private String fileName(String submittedName) {
        if (submittedName == null || submittedName.isBlank()) return "contract-document";
        String name = submittedName.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        return name.replaceAll("[\\r\\n]", "").trim();
    }

    private String extension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeMimeType(String contentType, String extension) {
        String lower = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "pdf" -> lower.contains("pdf") || lower.equals("application/octet-stream")
                    ? "application/pdf" : null;
            case "jpg", "jpeg" -> lower.startsWith("image/") || lower.equals("application/octet-stream")
                    ? "image/jpeg" : null;
            case "png" -> lower.startsWith("image/") || lower.equals("application/octet-stream")
                    ? "image/png" : null;
            default -> null;
        };
    }

    private String sanitizePathPart(String value) {
        if (value == null || value.isBlank()) return "general";
        String cleaned = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        return cleaned.isBlank() ? "general" : cleaned;
    }
}

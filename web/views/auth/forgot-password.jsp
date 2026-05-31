<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Forgot Password &mdash; HRM System</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body {
            background: linear-gradient(135deg, #1a3c5e 0%, #2d6a9f 100%);
            min-height: 100vh; display: flex; align-items: center; justify-content: center;
        }
        .card { width: 100%; max-width: 440px; border: none; border-radius: 16px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }
        .card-header-custom {
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            border-radius: 16px 16px 0 0; padding: 2rem; text-align: center;
        }
        .btn-primary-custom {
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            border: none; border-radius: 8px; padding: 0.65rem;
            font-size: 1rem; font-weight: 600;
        }
        .btn-primary-custom:hover { opacity: 0.88; }
        .input-group-text { background-color: #f8f9fa; border-right: none; }
        .form-control { border-left: none; }
        .form-control:focus { border-color: #2d6a9f; box-shadow: 0 0 0 0.2rem rgba(45,106,159,0.25); }
        .reset-link-box {
            background: #f0f9ff; border: 1px solid #bae6fd;
            border-radius: 8px; padding: 0.75rem 1rem;
            word-break: break-all; font-size: 0.82rem; color: #0369a1;
        }
    </style>
</head>
<body>
<div class="card mx-3">
    <div class="card-header-custom">
        <div class="mb-3">
            <div class="bg-white rounded-circle d-inline-flex align-items-center justify-content-center"
                 style="width:64px;height:64px;">
                <i class="bi bi-key text-primary" style="font-size:1.8rem;"></i>
            </div>
        </div>
        <h4 class="text-white fw-bold mb-1">Forgot Password</h4>
        <p class="text-white-50 small mb-0">Enter your email to receive a reset link</p>
    </div>

    <div class="card-body p-4">

        <c:if test="${not empty error}">
            <div class="alert alert-danger d-flex align-items-center gap-2 py-2">
                <i class="bi bi-exclamation-circle-fill"></i><span>${error}</span>
            </div>
        </c:if>

        <c:if test="${not empty success}">
            <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
                <i class="bi bi-check-circle-fill"></i><span>${success}</span>
            </div>
        </c:if>

        <c:if test="${empty success}">
            <form action="${pageContext.request.contextPath}/forgot-password" method="post">
                <input type="hidden" name="action" value="request">
                <div class="mb-4">
                    <label for="email" class="form-label fw-medium">Email Address</label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-envelope text-secondary"></i></span>
                        <input type="email" id="email" name="email" class="form-control"
                               placeholder="Enter your email address"
                               value="${not empty email ? email : ''}" required autofocus>
                    </div>
                </div>
                <div class="d-grid mb-3">
                    <button type="submit" class="btn btn-primary btn-primary-custom text-white">
                        <i class="bi bi-send me-2"></i>Send Reset Link
                    </button>
                </div>
            </form>
        </c:if>

        <div class="text-center">
            <a href="${pageContext.request.contextPath}/login"
               class="text-decoration-none small text-secondary">
                <i class="bi bi-arrow-left me-1"></i>Back to Sign In
            </a>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

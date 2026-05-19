<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reset Password &mdash; HRM System</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body {
            background: linear-gradient(135deg, #1a3c5e 0%, #2d6a9f 100%);
            min-height: 100vh; display: flex; align-items: center; justify-content: center;
        }
        .card { width: 100%; max-width: 420px; border: none; border-radius: 16px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }
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
    </style>
</head>
<body>
<div class="card mx-3">
    <div class="card-header-custom">
        <div class="mb-3">
            <div class="bg-white rounded-circle d-inline-flex align-items-center justify-content-center"
                 style="width:64px;height:64px;">
                <i class="bi bi-lock text-primary" style="font-size:1.8rem;"></i>
            </div>
        </div>
        <h4 class="text-white fw-bold mb-1">Set New Password</h4>
        <p class="text-white-50 small mb-0">Choose a strong new password</p>
    </div>

    <div class="card-body p-4">

        <c:if test="${not empty error}">
            <div class="alert alert-danger d-flex align-items-center gap-2 py-2">
                <i class="bi bi-exclamation-circle-fill"></i><span>${error}</span>
            </div>
        </c:if>

        <form action="${pageContext.request.contextPath}/forgot-password" method="post">
            <input type="hidden" name="action" value="reset">
            <input type="hidden" name="token" value="${token}">

            <div class="mb-3">
                <label for="newPassword" class="form-label fw-medium">New Password</label>
                <div class="input-group">
                    <span class="input-group-text"><i class="bi bi-lock text-secondary"></i></span>
                    <input type="password" id="newPassword" name="newPassword" class="form-control"
                           placeholder="Enter new password" minlength="6" required autofocus>
                    <button class="btn btn-outline-secondary" type="button" onclick="toggle('newPassword','eye1')">
                        <i class="bi bi-eye" id="eye1"></i>
                    </button>
                </div>
            </div>

            <div class="mb-4">
                <label for="confirmPassword" class="form-label fw-medium">Confirm Password</label>
                <div class="input-group">
                    <span class="input-group-text"><i class="bi bi-lock-fill text-secondary"></i></span>
                    <input type="password" id="confirmPassword" name="confirmPassword" class="form-control"
                           placeholder="Confirm new password" required>
                    <button class="btn btn-outline-secondary" type="button" onclick="toggle('confirmPassword','eye2')">
                        <i class="bi bi-eye" id="eye2"></i>
                    </button>
                </div>
            </div>

            <div class="d-grid mb-3">
                <button type="submit" class="btn btn-primary btn-primary-custom text-white">
                    <i class="bi bi-check-lg me-2"></i>Reset Password
                </button>
            </div>
        </form>

        <div class="text-center">
            <a href="${pageContext.request.contextPath}/login"
               class="text-decoration-none small text-secondary">
                <i class="bi bi-arrow-left me-1"></i>Back to Sign In
            </a>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    function toggle(inputId, iconId) {
        const input = document.getElementById(inputId);
        const icon  = document.getElementById(iconId);
        const isPass = input.type === 'password';
        input.type = isPass ? 'text' : 'password';
        icon.className = isPass ? 'bi bi-eye-slash' : 'bi bi-eye';
    }
</script>
</body>
</html>

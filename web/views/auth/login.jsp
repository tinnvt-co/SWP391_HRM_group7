<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In &mdash; HRM System</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body {
            background: linear-gradient(135deg, #1a3c5e 0%, #2d6a9f 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .login-card {
            width: 100%;
            max-width: 420px;
            border: none;
            border-radius: 16px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        .login-header {
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            border-radius: 16px 16px 0 0;
            padding: 2rem;
            text-align: center;
        }
        .form-control:focus {
            border-color: #2d6a9f;
            box-shadow: 0 0 0 0.2rem rgba(45,106,159,0.25);
        }
        .btn-login {
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            border: none;
            border-radius: 8px;
            padding: 0.65rem;
            font-size: 1rem;
            font-weight: 600;
        }
        .btn-login:hover { opacity: 0.88; }
        .input-group-text {
            background-color: #f8f9fa;
            border-right: none;
        }
        .form-control { border-left: none; }
    </style>
</head>
<body>
<div class="login-card card mx-3">
    <div class="login-header">
        <div class="mb-3">
            <div class="bg-white rounded-circle d-inline-flex align-items-center justify-content-center"
                 style="width:64px;height:64px;">
                <i class="bi bi-building text-primary" style="font-size:1.8rem;"></i>
            </div>
        </div>
        <h4 class="text-white fw-bold mb-1">HRM System</h4>
        <p class="text-white-50 small mb-0">Human Resource Management</p>
    </div>

    <div class="card-body p-4">
        <h5 class="fw-semibold mb-4 text-center text-secondary">Sign in to your account</h5>

        <c:if test="${not empty error}">
            <div class="alert alert-danger d-flex align-items-center gap-2 py-2" role="alert">
                <i class="bi bi-exclamation-circle-fill"></i>
                <span>${error}</span>
            </div>
        </c:if>

        <form action="${pageContext.request.contextPath}/login" method="post" novalidate>
            <div class="mb-3">
                <label for="username" class="form-label fw-medium">Username</label>
                <div class="input-group">
                    <span class="input-group-text"><i class="bi bi-person text-secondary"></i></span>
                    <input type="text" id="username" name="username" class="form-control"
                           placeholder="Enter your username"
                           value="${not empty username ? username : ''}"
                           required autofocus>
                </div>
            </div>

            <div class="mb-4">
                <label for="password" class="form-label fw-medium">Password</label>
                <div class="input-group">
                    <span class="input-group-text"><i class="bi bi-lock text-secondary"></i></span>
                    <input type="password" id="password" name="password" class="form-control"
                           placeholder="Enter your password" required>
                    <button class="btn btn-outline-secondary" type="button" id="togglePassword">
                        <i class="bi bi-eye" id="eyeIcon"></i>
                    </button>
                </div>
            </div>

            <div class="d-grid mb-3">
                <button type="submit" class="btn btn-primary btn-login text-white">
                    <i class="bi bi-box-arrow-in-right me-2"></i>Sign In
                </button>
            </div>

            <div class="d-flex justify-content-between align-items-center">
                <a href="${pageContext.request.contextPath}/"
                   class="text-decoration-none small text-secondary">
                    <i class="bi bi-arrow-left me-1"></i>Back to Home
                </a>
                <a href="${pageContext.request.contextPath}/forgot-password"
                   class="text-decoration-none small text-secondary">
                    Forgot password?
                </a>
            </div>
        </form>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const toggleBtn = document.getElementById('togglePassword');
    const passwordInput = document.getElementById('password');
    const eyeIcon = document.getElementById('eyeIcon');
    toggleBtn.addEventListener('click', () => {
        const isPassword = passwordInput.type === 'password';
        passwordInput.type = isPassword ? 'text' : 'password';
        eyeIcon.className = isPassword ? 'bi bi-eye-slash' : 'bi bi-eye';
    });
</script>
</body>
</html>

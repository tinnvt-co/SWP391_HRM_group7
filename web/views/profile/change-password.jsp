<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="changePassword" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Change Password &mdash; HRM System</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body { background-color: #f4f6f9; }
        .sidebar {
            width: 240px; min-height: calc(100vh - 56px);
            background-color: #1a3c5e; position: fixed;
            top: 56px; left: 0; padding-top: 1rem; z-index: 100;
        }
        .sidebar .nav-link {
            color: rgba(255,255,255,0.75); padding: 0.6rem 1.25rem;
            border-radius: 6px; margin: 2px 10px; font-size: 0.9rem; transition: all 0.2s;
        }
        .sidebar .nav-link:hover, .sidebar .nav-link.active {
            color: #fff; background-color: rgba(255,255,255,0.12);
        }
        .sidebar .nav-link i { width: 20px; }
        .sidebar-label {
            font-size: 0.7rem; text-transform: uppercase; letter-spacing: 1px;
            color: rgba(255,255,255,0.4); padding: 0.75rem 1.25rem 0.25rem;
        }
        .main-content { margin-left: 240px; padding: 2rem; }
        .input-group-text { background-color: #f8f9fa; border-right: none; }
        .form-control { border-left: none; }
        .form-control:focus { border-color: #2d6a9f; box-shadow: 0 0 0 0.2rem rgba(45,106,159,0.25); }
        .strength-bar { height: 4px; border-radius: 2px; transition: all 0.3s; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="mb-4">
        <h5 class="fw-bold text-dark mb-0">Change Password</h5>
        <small class="text-muted">Update your account password</small>
    </div>

    <div class="row justify-content-center">
        <div class="col-lg-6">
            <div class="card border-0 shadow-sm rounded-3 p-4">
                <div class="text-center mb-4">
                    <div class="d-inline-flex align-items-center justify-content-center rounded-circle mb-3"
                         style="width:56px;height:56px;background:#e3f0fb;">
                        <i class="bi bi-key-fill text-primary fs-4"></i>
                    </div>
                    <p class="text-muted small mb-0">After changing your password, you will be signed out automatically.</p>
                </div>

                <c:if test="${not empty error}">
                    <div class="alert alert-danger d-flex align-items-center gap-2 py-2">
                        <i class="bi bi-exclamation-circle-fill"></i><span>${error}</span>
                    </div>
                </c:if>

                <form action="${pageContext.request.contextPath}/change-password" method="post" id="changePasswordForm">
                    <div class="mb-3">
                        <label for="currentPassword" class="form-label fw-medium">Current Password</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-lock text-secondary"></i></span>
                            <input type="password" id="currentPassword" name="currentPassword"
                                   class="form-control" placeholder="Enter current password" required autofocus>
                            <button class="btn btn-outline-secondary" type="button" onclick="toggle('currentPassword','eye0')">
                                <i class="bi bi-eye" id="eye0"></i>
                            </button>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label for="newPassword" class="form-label fw-medium">New Password</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-lock-fill text-secondary"></i></span>
                            <input type="password" id="newPassword" name="newPassword"
                                   class="form-control" placeholder="Enter new password (min. 6 characters)"
                                   minlength="6" required oninput="checkStrength(this.value)">
                            <button class="btn btn-outline-secondary" type="button" onclick="toggle('newPassword','eye1')">
                                <i class="bi bi-eye" id="eye1"></i>
                            </button>
                        </div>
                        <div class="mt-2">
                            <div class="d-flex gap-1">
                                <div class="strength-bar flex-fill bg-secondary" id="bar1"></div>
                                <div class="strength-bar flex-fill bg-secondary" id="bar2"></div>
                                <div class="strength-bar flex-fill bg-secondary" id="bar3"></div>
                                <div class="strength-bar flex-fill bg-secondary" id="bar4"></div>
                            </div>
                            <small class="text-muted" id="strengthLabel"></small>
                        </div>
                    </div>

                    <div class="mb-4">
                        <label for="confirmPassword" class="form-label fw-medium">Confirm New Password</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-lock-fill text-secondary"></i></span>
                            <input type="password" id="confirmPassword" name="confirmPassword"
                                   class="form-control" placeholder="Confirm new password" required>
                            <button class="btn btn-outline-secondary" type="button" onclick="toggle('confirmPassword','eye2')">
                                <i class="bi bi-eye" id="eye2"></i>
                            </button>
                        </div>
                        <div id="matchMsg" class="small mt-1"></div>
                    </div>

                    <div class="d-grid">
                        <button type="submit" class="btn btn-primary fw-semibold"
                                style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;border-radius:8px;padding:0.65rem;">
                            <i class="bi bi-check-lg me-2"></i>Update Password
                        </button>
                    </div>
                </form>
            </div>
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

    function checkStrength(val) {
        let score = 0;
        if (val.length >= 6)  score++;
        if (val.length >= 10) score++;
        if (/[A-Z]/.test(val) && /[0-9]/.test(val)) score++;
        if (/[^A-Za-z0-9]/.test(val)) score++;

        const colors = ['', 'danger', 'warning', 'info', 'success'];
        const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
        for (let i = 1; i <= 4; i++) {
            document.getElementById('bar' + i).className =
                'strength-bar flex-fill ' + (i <= score ? 'bg-' + colors[score] : 'bg-secondary');
        }
        document.getElementById('strengthLabel').textContent = val.length > 0 ? labels[score] : '';
        document.getElementById('strengthLabel').className = 'small text-' + (colors[score] || 'muted');
    }

    document.getElementById('confirmPassword').addEventListener('input', function() {
        const match = this.value === document.getElementById('newPassword').value;
        const el = document.getElementById('matchMsg');
        if (this.value.length === 0) { el.textContent = ''; return; }
        el.textContent = match ? '✓ Passwords match' : '✗ Passwords do not match';
        el.className = 'small mt-1 ' + (match ? 'text-success' : 'text-danger');
    });
</script>
</body>
</html>

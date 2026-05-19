<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>HRM System</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body {
            background-color: #f4f6f9;
            min-height: 100vh;
        }
        .hero {
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }
        .topbar {
            background-color: #fff;
            border-bottom: 1px solid #e5e7eb;
            padding: 1rem 2rem;
        }
        .hero-body {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            background: linear-gradient(135deg, #f4f6f9 0%, #e8f0f8 100%);
        }
        .hero-card {
            max-width: 560px;
            width: 100%;
            text-align: center;
            padding: 3rem 2rem;
        }
        .logo-circle {
            width: 80px;
            height: 80px;
            border-radius: 20px;
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            display: inline-flex;
            align-items: center;
            justify-content: center;
            margin-bottom: 1.5rem;
        }
        .btn-login-hero {
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            border: none;
            border-radius: 10px;
            padding: 0.75rem 2.5rem;
            font-size: 1rem;
            font-weight: 600;
            color: #fff;
            letter-spacing: 0.3px;
            transition: opacity 0.2s;
        }
        .btn-login-hero:hover { opacity: 0.88; color: #fff; }
        .feature-item {
            display: flex;
            align-items: center;
            gap: 0.75rem;
            padding: 0.6rem 1rem;
            background: #fff;
            border-radius: 10px;
            box-shadow: 0 1px 6px rgba(0,0,0,0.06);
            font-size: 0.9rem;
            color: #374151;
        }
        .feature-icon {
            width: 36px;
            height: 36px;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
        }
        footer {
            text-align: center;
            padding: 1rem;
            font-size: 0.8rem;
            color: #9ca3af;
            background: #fff;
            border-top: 1px solid #e5e7eb;
        }
    </style>
</head>
<body>
<div class="hero">

    <div class="topbar d-flex align-items-center justify-content-between">
        <div class="d-flex align-items-center gap-2">
            <div style="width:34px;height:34px;border-radius:8px;background:linear-gradient(135deg,#1a3c5e,#2d6a9f);
                        display:inline-flex;align-items:center;justify-content:center;">
                <i class="bi bi-building text-white" style="font-size:1rem;"></i>
            </div>
            <span class="fw-bold text-dark" style="font-size:1.05rem;">HRM System</span>
        </div>
        <a href="<%= request.getContextPath() %>/login" class="btn btn-sm btn-outline-primary px-3 fw-medium">
            Sign In
        </a>
    </div>

    <div class="hero-body">
        <div class="hero-card">
            <div class="logo-circle">
                <i class="bi bi-building text-white" style="font-size:2rem;"></i>
            </div>
            <h2 class="fw-bold mb-2" style="color:#1a3c5e;">Human Resource Management</h2>
            <p class="text-muted mb-4" style="font-size:1rem;">
                A centralized platform to manage employees, roles, attendance, and payroll efficiently.
            </p>

            <a href="<%= request.getContextPath() %>/login" class="btn btn-login-hero mb-5">
                <i class="bi bi-box-arrow-in-right me-2"></i>Sign In to Continue
            </a>

            <div class="row g-2 text-start mt-2">
                <div class="col-6">
                    <div class="feature-item">
                        <div class="feature-icon" style="background:#e3f0fb;">
                            <i class="bi bi-people-fill text-primary"></i>
                        </div>
                        Employee Management
                    </div>
                </div>
                <div class="col-6">
                    <div class="feature-item">
                        <div class="feature-icon" style="background:#e6f9f0;">
                            <i class="bi bi-shield-check text-success"></i>
                        </div>
                        Role & Permissions
                    </div>
                </div>
                <div class="col-6">
                    <div class="feature-item">
                        <div class="feature-icon" style="background:#fff8e1;">
                            <i class="bi bi-calendar-check text-warning"></i>
                        </div>
                        Attendance Tracking
                    </div>
                </div>
                <div class="col-6">
                    <div class="feature-item">
                        <div class="feature-icon" style="background:#fce8e8;">
                            <i class="bi bi-cash-coin text-danger"></i>
                        </div>
                        Payroll Management
                    </div>
                </div>
            </div>
        </div>
    </div>

    <footer>&copy; 2026 HRM System &mdash; Group 7</footer>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

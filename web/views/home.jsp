<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="home" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard &mdash; HRM System</title>
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
        .stat-card {
            border: none; border-radius: 12px;
            box-shadow: 0 2px 12px rgba(0,0,0,0.07); transition: transform 0.2s;
        }
        .stat-card:hover { transform: translateY(-3px); }
        .stat-icon {
            width: 52px; height: 52px; border-radius: 12px;
            display: flex; align-items: center; justify-content: center; font-size: 1.4rem;
        }
        .welcome-card {
            border: none; border-radius: 16px;
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            color: white; box-shadow: 0 4px 20px rgba(26,60,94,0.3);
        }
        .role-badge {
            background-color: rgba(255,255,255,0.2); color: white;
            padding: 4px 12px; border-radius: 20px; font-size: 0.8rem; font-weight: 500;
        }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">

    <div class="welcome-card card p-4 mb-4">
        <div class="d-flex align-items-center justify-content-between flex-wrap gap-3">
            <div>
                <p class="mb-1 opacity-75 small">Welcome back,</p>
                <h4 class="fw-bold mb-2">${currentUser.fullName}</h4>
                <span class="role-badge">${currentUser.role.roleName}</span>
            </div>
            <div class="text-end opacity-75">
                <i class="bi bi-building" style="font-size:4rem;"></i>
            </div>
        </div>
    </div>

    <c:if test="${not empty dashboardScope}">
        <div class="row g-3 mb-4">
            <c:if test="${not empty employeeCardLabel}">
                <div class="col-sm-6 col-xl-4">
                    <div class="stat-card card p-3">
                        <div class="d-flex align-items-center gap-3">
                            <div class="stat-icon" style="background:#e3f0fb;">
                                <i class="bi bi-people-fill text-primary"></i>
                            </div>
                            <div>
                                <div class="text-muted small">${employeeCardLabel}</div>
                                <div class="fw-bold fs-5">${employeeCardValue}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </c:if>
            <c:if test="${not empty departmentCardLabel}">
                <div class="col-sm-6 col-xl-4">
                    <div class="stat-card card p-3">
                        <div class="d-flex align-items-center gap-3">
                            <div class="stat-icon" style="background:#fff8e1;">
                                <i class="bi bi-diagram-3 text-warning"></i>
                            </div>
                            <div>
                                <div class="text-muted small">${departmentCardLabel}</div>
                                <div class="fw-bold fs-5">${departmentCardValue}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </c:if>
            <c:if test="${not empty roleCardLabel}">
                <div class="col-sm-6 col-xl-4">
                    <div class="stat-card card p-3">
                        <div class="d-flex align-items-center gap-3">
                            <div class="stat-icon" style="background:#e6f9f0;">
                                <i class="bi bi-shield-check text-success"></i>
                            </div>
                            <div>
                                <div class="text-muted small">${roleCardLabel}</div>
                                <div class="fw-bold fs-5">${roleCardValue}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </c:if>
        </div>
    </c:if>

    <div class="card border-0 shadow-sm rounded-3 p-4">
        <h6 class="fw-semibold mb-3 text-secondary">
            <i class="bi bi-info-circle me-2"></i>Account Info
        </h6>
        <div class="row g-2 small">
            <div class="col-md-3 text-muted">Email</div>
            <div class="col-md-9">${currentUser.email}</div>
            <div class="col-md-3 text-muted">Phone</div>
            <div class="col-md-9">${not empty currentUser.phone ? currentUser.phone : '&mdash;'}</div>
            <div class="col-md-3 text-muted">Last Login</div>
            <div class="col-md-9">
                <c:choose>
                    <c:when test="${not empty currentUser.lastLogin}">${currentUser.lastLogin}</c:when>
                    <c:otherwise>&mdash;</c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="/views/common/tab-session.jsp" %>
<style>
    body {
        padding-top: 56px;
        background-color: #f4f6f9;
    }

    .app-navbar {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        z-index: 1030;
        height: 56px;
    }

    .sidebar {
        position: fixed !important;
        top: 56px !important;
        bottom: 0;
        height: calc(100vh - 56px) !important;
        min-height: auto !important;
        overflow-x: hidden;
        overflow-y: auto;
        padding-bottom: 1rem;
    }

    .main-content {
        min-height: calc(100vh - 56px);
    }

    @media (max-width: 768px) {
        body {
            padding-top: 0;
        }

        .app-navbar {
            position: sticky;
        }

        .sidebar {
            position: static !important;
            width: 100% !important;
            height: auto !important;
            min-height: auto !important;
            overflow: visible;
        }

        .main-content {
            margin-left: 0 !important;
            min-height: auto;
        }
    }
</style>
<nav class="navbar navbar-dark app-navbar" style="background-color: #1a3c5e;">
    <div class="container-fluid px-4 d-flex align-items-center justify-content-between">
        <a class="navbar-brand fw-bold mb-0" href="${pageContext.request.contextPath}/home">
            <i class="bi bi-building me-2"></i>HRM System
        </a>
        <div class="dropdown">
            <a class="nav-link dropdown-toggle d-flex align-items-center gap-2 text-white" href="#"
               data-bs-toggle="dropdown">
                <div class="rounded-circle bg-warning text-dark d-flex align-items-center justify-content-center fw-bold"
                     style="width:32px;height:32px;font-size:13px;">
                    ${fn:substring(currentUser.fullName, 0, 1)}
                </div>
                <span>${currentUser.fullName}</span>
            </a>
            <ul class="dropdown-menu dropdown-menu-end">
                <li>
                    <span class="dropdown-item-text text-muted small">${currentUser.role.roleName}</span>
                </li>
                <li><hr class="dropdown-divider"></li>
                <li>
                    <a class="dropdown-item" href="${pageContext.request.contextPath}/profile">
                        <i class="bi bi-person me-2"></i>My Profile
                    </a>
                </li>
                <li>
                    <a class="dropdown-item" href="${pageContext.request.contextPath}/change-password">
                        <i class="bi bi-key me-2"></i>Change Password
                    </a>
                </li>
                <li><hr class="dropdown-divider"></li>
                <li>
                    <a class="dropdown-item text-danger" href="${pageContext.request.contextPath}/logout">
                        <i class="bi bi-box-arrow-right me-2"></i>Sign Out
                    </a>
                </li>
            </ul>
        </div>
    </div>
</nav>

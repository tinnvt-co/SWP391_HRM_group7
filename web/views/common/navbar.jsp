<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<nav class="navbar navbar-expand-lg navbar-dark" style="background-color: #1a3c5e;">
    <div class="container-fluid px-4">
        <a class="navbar-brand fw-bold" href="${pageContext.request.contextPath}/home">
            <i class="bi bi-building me-2"></i>HRM System
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarMain">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarMain">
            <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                <li class="nav-item">
                    <a class="nav-link" href="${pageContext.request.contextPath}/home">
                        <i class="bi bi-house-door me-1"></i>Home
                    </a>
                </li>
                <c:if test="${fn:contains(permissions, 'VIEW_USER_LIST')}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/users">
                            <i class="bi bi-people me-1"></i>Employees
                        </a>
                    </li>
                </c:if>
                <c:if test="${fn:contains(permissions, 'VIEW_ROLE_LIST')}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/roles">
                            <i class="bi bi-shield-check me-1"></i>Roles
                        </a>
                    </li>
                </c:if>
            </ul>
            <ul class="navbar-nav ms-auto align-items-center">
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle d-flex align-items-center gap-2" href="#"
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
                </li>
            </ul>
        </div>
    </div>
</nav>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="roles" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Role Permissions &mdash; HRM System</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body { background-color: #f4f6f9; }
        .sidebar {
            width: 240px; min-height: calc(100vh - 56px); background-color: #1a3c5e;
            position: fixed; top: 56px; left: 0; padding-top: 1rem; z-index: 100;
        }
        .sidebar .nav-link {
            color: rgba(255,255,255,0.75); padding: 0.6rem 1.25rem;
            border-radius: 6px; margin: 2px 10px; font-size: 0.9rem; transition: all 0.2s;
        }
        .sidebar .nav-link:hover, .sidebar .nav-link.active { color: #fff; background-color: rgba(255,255,255,0.12); }
        .sidebar .nav-link i { width: 20px; }
        .sidebar-label {
            font-size: 0.7rem; text-transform: uppercase; letter-spacing: 1px;
            color: rgba(255,255,255,0.4); padding: 0.75rem 1.25rem 0.25rem;
        }
        .main-content { margin-left: 240px; padding: 2rem; }
        .role-header {
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            border-radius: 14px; color: white; padding: 1.5rem;
        }
        .perm-item {
            display: flex; align-items: flex-start; gap: 0.75rem;
            padding: 0.75rem 1rem; border-radius: 10px; background: #f8fafc;
            border: 1px solid #e9ecef; transition: background 0.15s;
        }
        .perm-item:hover { background: #f0f6ff; }
        .perm-icon {
            width: 34px; height: 34px; border-radius: 8px; background: #e3f0fb;
            display: flex; align-items: center; justify-content: center;
            color: #1a3c5e; font-size: 0.9rem; flex-shrink: 0;
        }
        .perm-code { font-size: 0.72rem; font-family: monospace; color: #6b7280; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center gap-2 mb-4">
        <a href="${pageContext.request.contextPath}/roles" class="btn btn-sm btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Back
        </a>
        <div>
            <h5 class="fw-bold text-dark mb-0">Role Permissions</h5>
            <small class="text-muted">Permissions assigned to this role</small>
        </div>
    </div>

    <div class="row g-4">
        <div class="col-lg-4">
            <div class="role-header mb-3">
                <div class="d-flex align-items-center gap-3 mb-3">
                    <div class="bg-white bg-opacity-25 rounded-3 d-flex align-items-center justify-content-center"
                         style="width:48px;height:48px;">
                        <i class="bi bi-shield-check fs-4"></i>
                    </div>
                    <div>
                        <div class="fw-bold fs-5">${role.roleName}</div>
                        <div class="opacity-75 small">${not empty role.description ? role.description : 'No description'}</div>
                    </div>
                </div>
                <div class="d-flex gap-2 flex-wrap">
                    <span class="badge bg-white bg-opacity-25 text-white px-3">
                        <i class="bi bi-key me-1"></i>${fn:length(permissions)} permissions
                    </span>
                    <c:choose>
                        <c:when test="${role.active}">
                            <span class="badge bg-success px-3">Active</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-secondary px-3">Inactive</span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <div class="card border-0 shadow-sm rounded-3 p-3">
                <div class="small text-muted mb-2 fw-medium">Quick Actions</div>
                <c:if test="${fn:contains(sessionScope.permissions, 'UPDATE_ROLE_INFORMATION')}">
                    <a href="${pageContext.request.contextPath}/roles?action=edit&id=${role.roleId}"
                       class="btn btn-outline-primary btn-sm w-100 mb-2">
                        <i class="bi bi-pencil me-2"></i>Edit Role Info
                    </a>
                </c:if>
                <a href="${pageContext.request.contextPath}/roles"
                   class="btn btn-outline-secondary btn-sm w-100">
                    <i class="bi bi-grid me-2"></i>All Roles
                </a>
            </div>
        </div>

        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-3 p-4">
                <h6 class="fw-semibold mb-3 text-secondary">
                    <i class="bi bi-key me-2"></i>Assigned Permissions
                    <span class="badge bg-primary-subtle text-primary ms-2">${fn:length(permissions)}</span>
                </h6>

                <c:choose>
                    <c:when test="${empty permissions}">
                        <div class="text-center text-muted py-4">
                            <i class="bi bi-key fs-2 d-block mb-2 opacity-25"></i>
                            No permissions assigned to this role.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="d-flex flex-column gap-2">
                            <c:forEach var="p" items="${permissions}">
                                <div class="perm-item">
                                    <div class="perm-icon">
                                        <i class="bi bi-check-circle-fill"></i>
                                    </div>
                                    <div>
                                        <div class="fw-medium" style="font-size:0.9rem;">${p.permissionName}</div>
                                        <div class="perm-code">${p.permissionCode}</div>
                                        <c:if test="${not empty p.description}">
                                            <div class="text-muted" style="font-size:0.8rem;">${p.description}</div>
                                        </c:if>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

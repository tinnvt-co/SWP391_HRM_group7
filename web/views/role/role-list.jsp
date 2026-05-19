<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="roles" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Role List &mdash; HRM System</title>
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
        .table th { font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.5px; color: #6b7280; font-weight: 600; }
        .table td { vertical-align: middle; font-size: 0.9rem; }
        .role-icon {
            width: 38px; height: 38px; border-radius: 10px;
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            display: inline-flex; align-items: center; justify-content: center;
            color: white; font-size: 1rem; flex-shrink: 0;
        }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="mb-4">
        <h5 class="fw-bold text-dark mb-0">Role Management</h5>
        <small class="text-muted">View and manage system roles</small>
    </div>

    <c:if test="${param.updated == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>Role updated successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.toggled == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>Role status updated successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.permsUpdated == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>Role permissions updated successfully.</span>
        </div>
    </c:if>

    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">#</th>
                            <th>Role</th>
                            <th>Description</th>
                            <th>Status</th>
                            <th class="text-end pe-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="r" items="${roles}" varStatus="s">
                            <tr>
                                <td class="ps-4 text-muted">${s.index + 1}</td>
                                <td>
                                    <div class="d-flex align-items-center gap-2">
                                        <div class="role-icon"><i class="bi bi-shield-check"></i></div>
                                        <span class="fw-medium">${r.roleName}</span>
                                    </div>
                                </td>
                                <td class="text-muted">${not empty r.description ? r.description : '—'}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${r.active}">
                                            <span class="badge bg-success-subtle text-success border border-success-subtle">Active</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle">Inactive</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end pe-4">
                                    <div class="d-flex justify-content-end gap-1">
                                        <c:if test="${fn:contains(permissions, 'VIEW_ROLE_PERMISSIONS')}">
                                            <a href="${pageContext.request.contextPath}/roles?action=permissions&id=${r.roleId}"
                                               class="btn btn-sm btn-outline-info" title="View Permissions">
                                                <i class="bi bi-key me-1"></i>Permissions
                                            </a>
                                        </c:if>
                                        <c:if test="${fn:contains(permissions, 'EDIT_ROLE_PERMISSIONS')}">
                                            <a href="${pageContext.request.contextPath}/roles?action=editPerms&id=${r.roleId}"
                                               class="btn btn-sm btn-outline-warning" title="Edit Permissions">
                                                <i class="bi bi-pencil-square me-1"></i>Edit Perms
                                            </a>
                                        </c:if>
                                        <c:if test="${fn:contains(permissions, 'UPDATE_ROLE_INFORMATION')}">
                                            <a href="${pageContext.request.contextPath}/roles?action=edit&id=${r.roleId}"
                                               class="btn btn-sm btn-outline-primary" title="Edit Role">
                                                <i class="bi bi-pencil me-1"></i>Edit
                                            </a>
                                        </c:if>
                                        <c:if test="${fn:contains(permissions, 'ACTIVE_DEACTIVE_ROLE')}">
                                            <form method="post" action="${pageContext.request.contextPath}/roles?action=toggle"
                                                  class="d-inline" onsubmit="return confirmToggle('${r.roleName}', ${r.active})">
                                                <input type="hidden" name="roleId" value="${r.roleId}">
                                                <input type="hidden" name="currentStatus" value="${r.active}">
                                                <c:choose>
                                                    <c:when test="${r.active}">
                                                        <button type="submit" class="btn btn-sm btn-outline-danger" title="Deactivate">
                                                            <i class="bi bi-toggle-on me-1"></i>Deactivate
                                                        </button>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <button type="submit" class="btn btn-sm btn-outline-success" title="Activate">
                                                            <i class="bi bi-toggle-off me-1"></i>Activate
                                                        </button>
                                                    </c:otherwise>
                                                </c:choose>
                                            </form>
                                        </c:if>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty roles}">
                            <tr>
                                <td colspan="5" class="text-center text-muted py-5">
                                    <i class="bi bi-shield fs-2 d-block mb-2 opacity-25"></i>No roles found.
                                </td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    function confirmToggle(roleName, isActive) {
        const action = isActive ? 'deactivate' : 'activate';
        return confirm('Are you sure you want to ' + action + ' role "' + roleName + '"?');
    }
</script>
</body>
</html>

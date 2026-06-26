<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="users" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>User List &mdash; HRM System</title>
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
        .avatar-sm {
            width: 34px; height: 34px; border-radius: 50%;
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            display: inline-flex; align-items: center; justify-content: center;
            font-size: 0.8rem; font-weight: 700; color: white; flex-shrink: 0;
        }
        .badge-role { background-color: #e3f0fb; color: #1a3c5e; padding: 3px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 600; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
            <h5 class="fw-bold text-dark mb-0">User Management</h5>
            <small class="text-muted">Manage all system users</small>
        </div>
        <c:if test="${permissions.contains('ADD_NEW_USER')}">
            <a href="${pageContext.request.contextPath}/users?action=add"
               class="btn btn-primary btn-sm px-3 fw-medium"
               style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                <i class="bi bi-person-plus me-2"></i>Add New User
            </a>
        </c:if>
    </div>

    <c:if test="${param.added == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>User created successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.updated == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>User updated successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.toggled == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>User status updated successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.toggleError == 'self'}">
        <div class="alert alert-warning d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-triangle-fill"></i><span>You cannot change your own active status.</span>
        </div>
    </c:if>

    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="p-3 border-bottom d-flex align-items-center gap-2">
                <i class="bi bi-search text-muted"></i>
                <input type="text" id="searchInput" class="form-control form-control-sm border-0 shadow-none"
                       placeholder="Search by name, username, email..." style="max-width:320px;">
            </div>
            <div class="table-responsive">
                <table class="table table-hover mb-0" id="userTable">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">#</th>
                            <th>User</th>
                            <th>Email</th>
                            <th>Phone</th>
                            <th>Role</th>
                            <th>Status</th>
                            <th class="text-end pe-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="u" items="${users}" varStatus="s">
                            <tr>
                                <td class="ps-4 text-muted">${(currentPage - 1) * 10 + s.index + 1}</td>
                                <td>
                                    <div class="d-flex align-items-center gap-2">
                                        <div class="avatar-sm">${fn:substring(u.fullName, 0, 1)}</div>
                                        <div>
                                            <div class="fw-medium">${u.fullName}</div>
                                            <div class="text-muted" style="font-size:0.78rem;">@${u.username}</div>
                                        </div>
                                    </div>
                                </td>
                                <td>${u.email}</td>
                                <td>${not empty u.phone ? u.phone : '—'}</td>
                                <td><span class="badge-role">${u.role.roleName}</span></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${u.active}">
                                            <span class="badge bg-success-subtle text-success border border-success-subtle">Active</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle">Inactive</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end pe-4">
                                    <div class="d-flex justify-content-end gap-1">
                                        <c:if test="${permissions.contains('VIEW_USER_INFORMATION')}">
                                            <a href="${pageContext.request.contextPath}/users?action=view&id=${u.userId}"
                                               class="btn btn-sm btn-outline-primary">
                                                <i class="bi bi-eye me-1"></i>View
                                            </a>
                                        </c:if>
                                        <c:if test="${permissions.contains('UPDATE_USER_INFORMATION')}">
                                            <a href="${pageContext.request.contextPath}/users?action=edit&id=${u.userId}"
                                               class="btn btn-sm btn-outline-secondary">
                                                <i class="bi bi-pencil me-1"></i>Edit
                                            </a>
                                        </c:if>
                                        <c:if test="${permissions.contains('ACTIVE_DEACTIVE_USER')}">
                                            <form method="post" action="${pageContext.request.contextPath}/users?action=toggle"
                                                  class="d-inline"
                                                  onsubmit="return confirm('${u.active ? 'Deactivate' : 'Activate'} user \'${u.fullName}\'?')">
                                                <input type="hidden" name="userId" value="${u.userId}">
                                                <input type="hidden" name="currentStatus" value="${u.active}">
                                                <c:choose>
                                                    <c:when test="${u.active}">
                                                        <button type="submit" class="btn btn-sm btn-outline-danger">
                                                            <i class="bi bi-toggle-on me-1"></i>Deactivate
                                                        </button>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <button type="submit" class="btn btn-sm btn-outline-success">
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
                        <c:if test="${empty users}">
                            <tr>
                                <td colspan="7" class="text-center text-muted py-5">
                                    <i class="bi bi-people fs-2 d-block mb-2 opacity-25"></i>No users found.
                                </td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
        <c:if test="${totalPages > 1}">
            <div class="d-flex align-items-center justify-content-between px-3 py-3 border-top flex-wrap gap-2">
                <small class="text-muted">
                    Page ${currentPage} of ${totalPages} &middot; ${totalUsers} users
                </small>
                <nav>
                    <ul class="pagination pagination-sm mb-0">
                        <li class="page-item ${currentPage == 1 ? 'disabled' : ''}">
                            <a class="page-link" href="?page=${currentPage - 1}">Previous</a>
                        </li>
                        <c:forEach var="p" begin="1" end="${totalPages}">
                            <li class="page-item ${p == currentPage ? 'active' : ''}">
                                <a class="page-link" href="?page=${p}">${p}</a>
                            </li>
                        </c:forEach>
                        <li class="page-item ${currentPage == totalPages ? 'disabled' : ''}">
                            <a class="page-link" href="?page=${currentPage + 1}">Next</a>
                        </li>
                    </ul>
                </nav>
            </div>
        </c:if>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    document.getElementById('searchInput').addEventListener('input', function () {
        const q = this.value.toLowerCase();
        document.querySelectorAll('#userTable tbody tr').forEach(row => {
            row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
        });
    });
</script>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="roles" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit Role Permissions &mdash; HRM System</title>
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
        .perm-card {
            border: 2px solid #e5e7eb; border-radius: 10px; padding: 0.75rem 1rem;
            cursor: pointer; transition: all 0.15s; user-select: none;
        }
        .perm-card:hover { border-color: #2d6a9f; background: #f0f6ff; }
        .perm-card.selected { border-color: #1a3c5e; background: #e3f0fb; }
        .perm-card input[type=checkbox] { display: none; }
        .perm-code { font-size: 0.72rem; font-family: monospace; color: #6b7280; }
        .check-indicator {
            width: 22px; height: 22px; border-radius: 6px; border: 2px solid #d1d5db;
            display: flex; align-items: center; justify-content: center;
            font-size: 0.75rem; color: white; flex-shrink: 0; transition: all 0.15s;
        }
        .perm-card.selected .check-indicator {
            background: #1a3c5e; border-color: #1a3c5e;
        }
        .role-badge-header {
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            border-radius: 12px; color: white; padding: 1rem 1.25rem;
        }
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
            <h5 class="fw-bold text-dark mb-0">Edit Role Permissions</h5>
            <small class="text-muted">Select permissions to assign to this role</small>
        </div>
    </div>

    <div class="role-badge-header d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div class="d-flex align-items-center gap-3">
            <div class="bg-white bg-opacity-25 rounded-3 d-flex align-items-center justify-content-center"
                 style="width:44px;height:44px;">
                <i class="bi bi-shield-check fs-5"></i>
            </div>
            <div>
                <div class="fw-bold">${role.roleName}</div>
                <div class="opacity-75 small">${not empty role.description ? role.description : 'No description'}</div>
            </div>
        </div>
        <span class="badge bg-white bg-opacity-25 text-white px-3" id="selectedCount">
            <i class="bi bi-key me-1"></i><span id="countNum">${fn:length(assignedIds)}</span> selected
        </span>
    </div>

    <form action="${pageContext.request.contextPath}/roles?action=editPerms" method="post">
        <input type="hidden" name="roleId" value="${role.roleId}">

        <div class="card border-0 shadow-sm rounded-3 p-4 mb-4">
            <div class="d-flex align-items-center justify-content-between mb-3 flex-wrap gap-2">
                <h6 class="fw-semibold text-secondary mb-0">
                    <i class="bi bi-key me-2"></i>Available Permissions
                </h6>
                <div class="d-flex gap-2">
                    <button type="button" class="btn btn-sm btn-outline-primary" onclick="selectAll()">
                        <i class="bi bi-check-all me-1"></i>Select All
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-secondary" onclick="clearAll()">
                        <i class="bi bi-x-lg me-1"></i>Clear All
                    </button>
                </div>
            </div>

            <div class="row g-2">
                <c:forEach var="p" items="${allPermissions}">
                    <c:set var="isAssigned" value="false"/>
                    <c:forEach var="aid" items="${assignedIds}">
                        <c:if test="${aid == p.permissionId}"><c:set var="isAssigned" value="true"/></c:if>
                    </c:forEach>
                    <div class="col-md-6 col-lg-4">
                        <label class="perm-card d-flex align-items-start gap-2 w-100 ${isAssigned ? 'selected' : ''}">
                            <input type="checkbox" name="permissionIds" value="${p.permissionId}"
                                   ${isAssigned ? 'checked' : ''}>
                            <div class="check-indicator mt-1">
                                <i class="bi bi-check-lg"></i>
                            </div>
                            <div class="flex-grow-1">
                                <div class="fw-medium" style="font-size:0.88rem;">${p.permissionName}</div>
                                <div class="perm-code">${p.permissionCode}</div>
                            </div>
                        </label>
                    </div>
                </c:forEach>
            </div>
        </div>

        <div class="d-flex justify-content-end gap-2">
            <a href="${pageContext.request.contextPath}/roles" class="btn btn-outline-secondary px-4">Cancel</a>
            <button type="submit" class="btn btn-primary px-4 fw-medium"
                    style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;"
                    onclick="return confirm('Save permission changes for role \'${role.roleName}\'?')">
                <i class="bi bi-check-lg me-2"></i>Save Permissions
            </button>
        </div>
    </form>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    document.querySelectorAll('.perm-card').forEach(card => {
        card.addEventListener('click', () => {
            const cb = card.querySelector('input[type=checkbox]');
            cb.checked = !cb.checked;
            card.classList.toggle('selected', cb.checked);
            updateCount();
        });
    });

    function updateCount() {
        const count = document.querySelectorAll('.perm-card.selected').length;
        document.getElementById('countNum').textContent = count;
    }

    function selectAll() {
        document.querySelectorAll('.perm-card').forEach(card => {
            card.querySelector('input[type=checkbox]').checked = true;
            card.classList.add('selected');
        });
        updateCount();
    }

    function clearAll() {
        document.querySelectorAll('.perm-card').forEach(card => {
            card.querySelector('input[type=checkbox]').checked = false;
            card.classList.remove('selected');
        });
        updateCount();
    }
</script>
</body>
</html>

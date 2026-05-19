<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="roles" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit Role &mdash; HRM System</title>
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
        .form-label { font-weight: 500; font-size: 0.9rem; }
        .required::after { content: ' *'; color: #dc3545; }
        .form-control:focus, .form-select:focus {
            border-color: #2d6a9f; box-shadow: 0 0 0 0.2rem rgba(45,106,159,0.2);
        }
        .info-badge {
            background: #f0f6ff; border: 1px solid #bfdbfe;
            border-radius: 10px; padding: 0.75rem 1rem; font-size: 0.85rem;
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
            <h5 class="fw-bold text-dark mb-0">Edit Role</h5>
            <small class="text-muted">Update role name and description</small>
        </div>
    </div>

    <div class="row justify-content-center">
        <div class="col-lg-6">

            <div class="info-badge d-flex align-items-center gap-2 mb-4 text-primary">
                <i class="bi bi-info-circle-fill"></i>
                <span>Only <strong>role name</strong> and <strong>description</strong> can be updated here.
                      To manage permissions, use the Permissions page.</span>
            </div>

            <div class="card border-0 shadow-sm rounded-3 p-4">

                <c:if test="${not empty error}">
                    <div class="alert alert-danger d-flex align-items-center gap-2 py-2 mb-4">
                        <i class="bi bi-exclamation-circle-fill"></i><span>${error}</span>
                    </div>
                </c:if>

                <form action="${pageContext.request.contextPath}/roles?action=edit" method="post">
                    <input type="hidden" name="roleId" value="${role.roleId}">

                    <div class="mb-3">
                        <label class="form-label text-muted small">Role ID</label>
                        <input type="text" class="form-control bg-light" value="${role.roleId}" disabled>
                    </div>

                    <div class="mb-3">
                        <label for="roleName" class="form-label required">Role Name</label>
                        <input type="text" id="roleName" name="roleName" class="form-control"
                               placeholder="Enter role name"
                               value="${not empty param.roleName ? param.roleName : role.roleName}" required>
                    </div>

                    <div class="mb-4">
                        <label for="description" class="form-label">Description</label>
                        <textarea id="description" name="description" class="form-control" rows="3"
                                  placeholder="Enter role description">${not empty param.description ? param.description : role.description}</textarea>
                    </div>

                    <div class="d-flex justify-content-end gap-2">
                        <a href="${pageContext.request.contextPath}/roles"
                           class="btn btn-outline-secondary px-4">Cancel</a>
                        <button type="submit" class="btn btn-primary px-4 fw-medium"
                                style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                            <i class="bi bi-check-lg me-2"></i>Save Changes
                        </button>
                    </div>
                </form>
            </div>

            <div class="card border-0 shadow-sm rounded-3 p-3 mt-3">
                <div class="small text-muted fw-medium mb-2">Other Actions</div>
                <a href="${pageContext.request.contextPath}/roles?action=permissions&id=${role.roleId}"
                   class="btn btn-outline-info btn-sm w-100">
                    <i class="bi bi-key me-2"></i>View Permissions
                </a>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

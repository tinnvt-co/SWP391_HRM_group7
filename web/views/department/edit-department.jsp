<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="departments" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit Department &mdash; HRM System</title>
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
        .section-title {
            font-size: 0.75rem; font-weight: 600; text-transform: uppercase;
            letter-spacing: 0.8px; color: #6b7280;
            padding-bottom: 0.5rem; border-bottom: 1px solid #e5e7eb; margin-bottom: 1.25rem;
        }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center gap-2 mb-4">
        <a href="${pageContext.request.contextPath}/departments" class="btn btn-sm btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Back
        </a>
        <div>
            <h5 class="fw-bold text-dark mb-0">Edit Department</h5>
            <small class="text-muted">Update information for ${department.departmentName}</small>
        </div>
    </div>

    <div class="row justify-content-center">
        <div class="col-xl-8">
            <div class="card border-0 shadow-sm rounded-3 p-4">

                <c:if test="${not empty error}">
                    <div class="alert alert-danger d-flex align-items-center gap-2 py-2 mb-4">
                        <i class="bi bi-exclamation-circle-fill"></i><span>${error}</span>
                    </div>
                </c:if>

                <form action="${pageContext.request.contextPath}/departments?action=edit" method="post" novalidate>
                    <input type="hidden" name="departmentId" value="${department.departmentId}">

                    <div class="section-title">Department Information</div>
                    <div class="row g-3 mb-4">
                        <div class="col-md-6">
                            <label class="form-label text-muted">Department Code</label>
                            <input type="text" class="form-control bg-light" value="${department.departmentCode}" disabled>
                            <div class="form-text">Department code cannot be changed.</div>
                        </div>
                        <div class="col-md-6">
                            <label for="departmentName" class="form-label required">Department Name</label>
                            <input type="text" id="departmentName" name="departmentName" class="form-control"
                                   maxlength="150" value="${department.departmentName}" required>
                        </div>
                        <div class="col-md-6">
                            <label for="managerId" class="form-label">Manager</label>
                            <select id="managerId" name="managerId" class="form-select">
                                <option value="">-- No manager --</option>
                                <c:forEach var="m" items="${managers}">
                                    <option value="${m.userId}" ${department.managerId == m.userId ? 'selected' : ''}>
                                        ${m.fullName} — ${m.role.roleName}
                                    </option>
                                </c:forEach>
                            </select>
                            <div class="form-text">HR department must be headed by an HR Manager; other departments by a Manager.</div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label text-muted">Status</label>
                            <div>
                                <c:choose>
                                    <c:when test="${department.active}">
                                        <span class="badge bg-success-subtle text-success border border-success-subtle px-3 py-2">Active</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle px-3 py-2">Inactive</span>
                                    </c:otherwise>
                                </c:choose>
                                <div class="form-text">Change status from the department list (Activate / Deactivate).</div>
                            </div>
                        </div>
                        <div class="col-12">
                            <label for="description" class="form-label">Description</label>
                            <textarea id="description" name="description" class="form-control" rows="3"
                                      maxlength="255">${department.description}</textarea>
                        </div>
                    </div>

                    <div class="d-flex justify-content-end gap-2">
                        <a href="${pageContext.request.contextPath}/departments"
                           class="btn btn-outline-secondary px-4">Cancel</a>
                        <button type="submit" class="btn btn-primary px-4 fw-medium"
                                style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                            <i class="bi bi-check-lg me-2"></i>Save Changes
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

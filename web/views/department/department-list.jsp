<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="departments" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Department List &mdash; HRM System</title>
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
        .dept-icon {
            width: 38px; height: 38px; border-radius: 10px;
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            display: inline-flex; align-items: center; justify-content: center;
            color: white; font-size: 1rem; flex-shrink: 0;
        }
        .code-badge { background-color: #e3f0fb; color: #1a3c5e; padding: 3px 10px; border-radius: 6px; font-size: 0.75rem; font-weight: 600; font-family: monospace; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
            <h5 class="fw-bold text-dark mb-0">Department Management</h5>
            <small class="text-muted">View and manage company departments</small>
        </div>
        <c:if test="${permissions.contains('CREATE_DEPARTMENT')}">
            <a href="${pageContext.request.contextPath}/departments?action=add"
               class="btn btn-primary btn-sm px-3 fw-medium"
               style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                <i class="bi bi-plus-circle me-2"></i>Add Department
            </a>
        </c:if>
    </div>

    <c:if test="${param.added == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>Department created successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.updated == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>Department updated successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.toggled == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>Department status updated successfully.</span>
        </div>
    </c:if>

    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="p-3 border-bottom d-flex align-items-center gap-2">
                <i class="bi bi-search text-muted"></i>
                <input type="text" id="searchInput" class="form-control form-control-sm border-0 shadow-none"
                       placeholder="Search by name, code, manager..." style="max-width:320px;">
            </div>
            <div class="table-responsive">
                <table class="table table-hover mb-0" id="deptTable">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">#</th>
                            <th>Department</th>
                            <th>Code</th>
                            <th>Manager</th>
                            <th>Employees</th>
                            <th>Status</th>
                            <th class="text-end pe-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="d" items="${departments}" varStatus="s">
                            <tr>
                                <td class="ps-4 text-muted">${s.index + 1}</td>
                                <td>
                                    <div class="d-flex align-items-center gap-2">
                                        <div class="dept-icon"><i class="bi bi-diagram-3"></i></div>
                                        <div>
                                            <div class="fw-medium">${d.departmentName}</div>
                                            <div class="text-muted" style="font-size:0.78rem;">
                                                ${not empty d.description ? d.description : '—'}
                                            </div>
                                        </div>
                                    </div>
                                </td>
                                <td><span class="code-badge">${d.departmentCode}</span></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty d.managerName}">
                                            <i class="bi bi-person-badge me-1 text-secondary"></i>${d.managerName}
                                        </c:when>
                                        <c:otherwise><span class="text-muted">— Not assigned</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle">
                                        <i class="bi bi-people me-1"></i>${d.employeeCount}
                                    </span>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${d.active}">
                                            <span class="badge bg-success-subtle text-success border border-success-subtle">Active</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle">Inactive</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end pe-4">
                                    <div class="d-flex justify-content-end gap-1">
                                        <c:if test="${permissions.contains('UPDATE_DEPARTMENT')}">
                                            <a href="${pageContext.request.contextPath}/departments?action=edit&id=${d.departmentId}"
                                               class="btn btn-sm btn-outline-primary" title="Edit">
                                                <i class="bi bi-pencil me-1"></i>Edit
                                            </a>
                                        </c:if>
                                        <c:if test="${permissions.contains('ACTIVE_DEACTIVE_DEPARTMENT')}">
                                            <form method="post" action="${pageContext.request.contextPath}/departments?action=toggle"
                                                  class="d-inline"
                                                  onsubmit="return confirm('${d.active ? 'Deactivate' : 'Activate'} department \'${d.departmentName}\'?')">
                                                <input type="hidden" name="departmentId" value="${d.departmentId}">
                                                <input type="hidden" name="currentStatus" value="${d.active}">
                                                <c:choose>
                                                    <c:when test="${d.active}">
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
                        <c:if test="${empty departments}">
                            <tr>
                                <td colspan="7" class="text-center text-muted py-5">
                                    <i class="bi bi-diagram-3 fs-2 d-block mb-2 opacity-25"></i>No departments found.
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
    document.getElementById('searchInput').addEventListener('input', function () {
        const q = this.value.toLowerCase();
        document.querySelectorAll('#deptTable tbody tr').forEach(row => {
            row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
        });
    });
</script>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="hrEmployees" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Employees &mdash; HRM System</title>
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
        .code-badge { background-color: #e3f0fb; color: #1a3c5e; padding: 3px 10px; border-radius: 6px; font-size: 0.75rem; font-weight: 600; font-family: monospace; }
        .status-pill { padding: 4px 12px; border-radius: 20px; font-size: 0.75rem; font-weight: 600; }
        .status-working   { background:#e6f9f0; color:#166534; }
        .status-probation { background:#fff8e1; color:#a16207; }
        .status-resigned  { background:#fee2e2; color:#b91c1c; }
        .status-suspended { background:#e5e7eb; color:#4b5563; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="mb-4">
        <h5 class="fw-bold text-dark mb-0">Employees</h5>
        <small class="text-muted">View employee profiles and manage employment status</small>
    </div>

    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="p-3 border-bottom d-flex align-items-center gap-2">
                <i class="bi bi-search text-muted"></i>
                <input type="text" id="searchInput" class="form-control form-control-sm border-0 shadow-none"
                       placeholder="Search by name, code, email, department..." style="max-width:340px;">
            </div>
            <div class="table-responsive">
                <table class="table table-hover mb-0" id="empTable">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">#</th>
                            <th>Employee</th>
                            <th>Code</th>
                            <th>Department</th>
                            <th>Email</th>
                            <th>Employment Status</th>
                            <th class="text-end pe-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="e" items="${employees}" varStatus="s">
                            <tr>
                                <td class="ps-4 text-muted">${s.index + 1}</td>
                                <td>
                                    <div class="d-flex align-items-center gap-2">
                                        <div class="avatar-sm">${fn:substring(e.fullName, 0, 1)}</div>
                                        <div>
                                            <div class="fw-medium">${e.fullName}</div>
                                            <div class="text-muted" style="font-size:0.78rem;">@${e.username}</div>
                                        </div>
                                    </div>
                                </td>
                                <td><span class="code-badge">${e.employeeCode}</span></td>
                                <td>${e.departmentName}</td>
                                <td>${e.email}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${e.employmentStatus == 'Working'}">
                                            <span class="status-pill status-working">Working</span>
                                        </c:when>
                                        <c:when test="${e.employmentStatus == 'Probation'}">
                                            <span class="status-pill status-probation">Probation</span>
                                        </c:when>
                                        <c:when test="${e.employmentStatus == 'Resigned'}">
                                            <span class="status-pill status-resigned">Resigned</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="status-pill status-suspended">Suspended</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end pe-4">
                                    <c:if test="${fn:contains(permissions, 'VIEW_EMPLOYEE_INFORMATION')}">
                                        <a href="${pageContext.request.contextPath}/hr/employees?action=view&id=${e.employeeId}"
                                           class="btn btn-sm btn-outline-primary">
                                            <i class="bi bi-eye me-1"></i>View
                                        </a>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty employees}">
                            <tr>
                                <td colspan="7" class="text-center text-muted py-5">
                                    <i class="bi bi-people fs-2 d-block mb-2 opacity-25"></i>No employees found.
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
        document.querySelectorAll('#empTable tbody tr').forEach(row => {
            row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
        });
    });
</script>
</body>
</html>

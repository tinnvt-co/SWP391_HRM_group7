<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="activePage" value="contracts" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Contract List &mdash; HRM System</title>
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
        .table th { font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.5px; color: #6b7280; font-weight: 600; }
        .table td { vertical-align: middle; font-size: 0.9rem; }
        .avatar-sm {
            width: 34px; height: 34px; border-radius: 50%;
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            display: inline-flex; align-items: center; justify-content: center;
            font-size: 0.8rem; font-weight: 700; color: white; flex-shrink: 0;
        }
        .code-badge { background-color: #e3f0fb; color: #1a3c5e; padding: 3px 10px; border-radius: 6px; font-size: 0.75rem; font-weight: 600; font-family: monospace; }
        .status-pill { padding: 4px 12px; border-radius: 20px; font-size: 0.75rem; font-weight: 600; }
        .status-active     { background:#e6f9f0; color:#166534; }
        .status-expired    { background:#fff8e1; color:#a16207; }
        .status-terminated { background:#fee2e2; color:#b91c1c; }
        .filter-btn { border: 1px solid #e5e7eb; background: white; border-radius: 20px; padding: 4px 14px; font-size: 0.82rem; color: #4b5563; cursor: pointer; transition: all 0.15s; text-decoration:none; }
        .filter-btn:hover { border-color: #2d6a9f; color: #1a3c5e; }
        .filter-btn.active { background: #1a3c5e; color: white; border-color: #1a3c5e; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
            <h5 class="fw-bold text-dark mb-0">Contract Management</h5>
            <small class="text-muted">Manage all employee contracts</small>
        </div>
        <c:if test="${fn:contains(permissions, 'CREATE_CONTRACT')}">
            <a href="${pageContext.request.contextPath}/contracts?action=add"
               class="btn btn-primary btn-sm px-3 fw-medium"
               style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                <i class="bi bi-file-earmark-plus me-2"></i>New Contract
            </a>
        </c:if>
    </div>

    <c:if test="${param.added == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>Contract created successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.updated == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>Contract updated successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.terminated == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>Contract terminated successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.error == 'not-active'}">
        <div class="alert alert-warning d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-triangle-fill"></i><span>Only active contracts can be updated.</span>
        </div>
    </c:if>

    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="p-3 border-bottom d-flex align-items-center gap-2 flex-wrap">
                <div class="d-flex align-items-center gap-2 flex-grow-1">
                    <i class="bi bi-search text-muted"></i>
                    <input type="text" id="searchInput" class="form-control form-control-sm border-0 shadow-none"
                           placeholder="Search by employee, code, type..." style="max-width:320px;">
                </div>
                <div class="d-flex gap-2 flex-wrap">
                    <a href="?status=all" class="filter-btn ${statusFilter == 'all' ? 'active' : ''}">All</a>
                    <a href="?status=Active" class="filter-btn ${statusFilter == 'Active' ? 'active' : ''}">Active</a>
                    <a href="?status=Expired" class="filter-btn ${statusFilter == 'Expired' ? 'active' : ''}">Expired</a>
                    <a href="?status=Terminated" class="filter-btn ${statusFilter == 'Terminated' ? 'active' : ''}">Terminated</a>
                </div>
            </div>
            <div class="table-responsive">
                <table class="table table-hover mb-0" id="contractTable">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">#</th>
                            <th>Employee</th>
                            <th>Code</th>
                            <th>Type</th>
                            <th>Period</th>
                            <th>Basic Salary</th>
                            <th>Status</th>
                            <th class="text-end pe-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="ct" items="${contracts}" varStatus="s">
                            <tr>
                                <td class="ps-4 text-muted">${s.index + 1}</td>
                                <td>
                                    <div class="d-flex align-items-center gap-2">
                                        <div class="avatar-sm">${fn:substring(ct.employeeFullName, 0, 1)}</div>
                                        <div>
                                            <div class="fw-medium">${ct.employeeFullName}</div>
                                            <div class="text-muted" style="font-size:0.78rem;">${ct.employeeCode} &middot; ${ct.departmentName}</div>
                                        </div>
                                    </div>
                                </td>
                                <td><span class="code-badge">${ct.contractCode}</span></td>
                                <td>${ct.contractType.dbValue}</td>
                                <td class="small">
                                    <div>${ct.startDate}</div>
                                    <div class="text-muted">${not empty ct.endDate ? ct.endDate : 'No end date'}</div>
                                </td>
                                <td><fmt:formatNumber value="${ct.basicSalary}" type="number" maxFractionDigits="0"/> &#8363;</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${ct.status == 'Active'}">
                                            <span class="status-pill status-active">Active</span>
                                        </c:when>
                                        <c:when test="${ct.status == 'Expired'}">
                                            <span class="status-pill status-expired">Expired</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="status-pill status-terminated">Terminated</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end pe-4">
                                    <div class="d-flex justify-content-end gap-1">
                                        <a href="${pageContext.request.contextPath}/contracts?action=view&id=${ct.contractId}"
                                           class="btn btn-sm btn-outline-primary"><i class="bi bi-eye me-1"></i>View</a>
                                        <c:if test="${fn:contains(permissions, 'UPDATE_CONTRACT') && ct.status == 'Active'}">
                                            <a href="${pageContext.request.contextPath}/contracts?action=edit&id=${ct.contractId}"
                                               class="btn btn-sm btn-outline-secondary"><i class="bi bi-pencil me-1"></i>Edit</a>
                                        </c:if>
                                        <c:if test="${fn:contains(permissions, 'TERMINATE_CONTRACT') && ct.status == 'Active'}">
                                            <form method="post" action="${pageContext.request.contextPath}/contracts?action=terminate"
                                                  class="d-inline"
                                                  onsubmit="return confirm('Terminate contract ${ct.contractCode} of ${ct.employeeFullName}? This cannot be undone.')">
                                                <input type="hidden" name="contractId" value="${ct.contractId}">
                                                <button type="submit" class="btn btn-sm btn-outline-danger"><i class="bi bi-x-octagon me-1"></i>Terminate</button>
                                            </form>
                                        </c:if>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty contracts}">
                            <tr>
                                <td colspan="8" class="text-center text-muted py-5">
                                    <i class="bi bi-file-earmark-text fs-2 d-block mb-2 opacity-25"></i>No contracts found.
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
        document.querySelectorAll('#contractTable tbody tr').forEach(row => {
            row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
        });
    });
</script>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="activePage" value="attendanceList" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Attendance Detail &mdash; ${detailEmployee.fullName} &mdash; HRM System</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body { background-color: #f4f6f9; }
        .sidebar { width: 240px; min-height: calc(100vh - 56px); background-color: #1a3c5e;
            position: fixed; top: 56px; left: 0; padding-top: 1rem; z-index: 100; }
        .sidebar .nav-link { color: rgba(255,255,255,0.75); padding: 0.6rem 1.25rem;
            border-radius: 6px; margin: 2px 10px; font-size: 0.9rem; transition: all 0.2s; }
        .sidebar .nav-link:hover, .sidebar .nav-link.active { color: #fff; background-color: rgba(255,255,255,0.12); }
        .sidebar .nav-link i { width: 20px; }
        .sidebar-label { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 1px;
            color: rgba(255,255,255,0.4); padding: 0.75rem 1.25rem 0.25rem; }
        .main-content { margin-left: 240px; padding: 2rem; }
        .badge-status-present  { background: #e6f9f0; color: #166534; }
        .badge-status-absent   { background: #fee2e2; color: #b91c1c; }
        .badge-status-late     { background: #fff8e1; color: #a16207; }
        .badge-status-leave    { background: #e3f0fb; color: #1a3c5e; }
        .badge-status-holiday  { background: #f3e8ff; color: #6b21a8; }
        .badge-verified        { background: #e6f9f0; color: #166534; }
        .badge-pending         { background: #fff8e1; color: #a16207; }
        .avatar-lg { width: 56px; height: 56px; border-radius: 50%;
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            display: inline-flex; align-items: center; justify-content: center;
            font-size: 1.3rem; font-weight: 700; color: white; flex-shrink: 0; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">

    <%-- Breadcrumb --%>
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb mb-0" style="font-size:0.85rem;">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/attendance" class="text-decoration-none">
                    <i class="bi bi-calendar-check me-1"></i>Attendance
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">
                ${detailEmployee.fullName}
            </li>
        </ol>
    </nav>

    <%-- Employee header card --%>
    <div class="card border-0 shadow-sm rounded-3 mb-4">
        <div class="card-body d-flex align-items-center gap-3 py-3">
            <div class="avatar-lg">
                ${fn:substring(detailEmployee.fullName, 0, 1)}
            </div>
            <div>
                <h5 class="fw-bold text-dark mb-0">${detailEmployee.fullName}</h5>
                <small class="text-muted">
                    ${detailEmployee.employeeCode}
                    <c:if test="${not empty detailEmployee.departmentName}">
                        &middot; ${detailEmployee.departmentName}
                    </c:if>
                </small>
            </div>
            <div class="ms-auto text-end">
                <span class="text-muted" style="font-size:0.78rem;">${totalRecords} record(s)</span>
            </div>
        </div>
    </div>

    <%-- Date filter --%>
    <div class="card border-0 shadow-sm rounded-3 mb-4">
        <div class="card-body py-3">
            <form method="get" action="${pageContext.request.contextPath}/attendance" class="row g-2 align-items-end">
                <input type="hidden" name="action" value="employeeDetail">
                <input type="hidden" name="employeeId" value="${employeeId}">
                <div class="col-auto">
                    <label class="form-label small text-muted mb-1">From</label>
                    <input type="date" name="fromDate" value="${fromDate}" class="form-control form-control-sm">
                </div>
                <div class="col-auto">
                    <label class="form-label small text-muted mb-1">To</label>
                    <input type="date" name="toDate" value="${toDate}" class="form-control form-control-sm">
                </div>
                <div class="col-auto">
                    <button type="submit" class="btn btn-sm btn-primary"
                            style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                        <i class="bi bi-funnel me-1"></i>Filter
                    </button>
                </div>
                <c:if test="${not empty fromDate or not empty toDate}">
                    <div class="col-auto">
                        <a href="${pageContext.request.contextPath}/attendance?action=employeeDetail&employeeId=${employeeId}"
                           class="btn btn-sm btn-outline-secondary">
                            <i class="bi bi-x-lg me-1"></i>Clear
                        </a>
                    </div>
                </c:if>
            </form>
        </div>
    </div>

    <%-- Records table --%>
    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <c:choose>
                <c:when test="${not empty records}">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0" style="font-size:0.88rem;">
                            <thead class="table-light">
                                <tr>
                                    <th class="ps-3" style="width:40px;">#</th>
                                    <th>Date</th>
                                    <th>Check In</th>
                                    <th>Check Out</th>
                                    <th>Status</th>
                                    <th>Late</th>
                                    <th>Late Penalty</th>
                                    <th>Overtime (hrs)</th>
                                    <th>Verification</th>
                                    <th>Verified By</th>
                                    <th>Note</th>
                                    <c:if test="${managerScope}">
                                        <th class="text-center" style="width:100px;">Actions</th>
                                    </c:if>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="r" items="${records}" varStatus="loop">
                                    <tr>
                                        <td class="ps-3 text-muted">${(currentPage - 1) * 10 + loop.index + 1}</td>
                                        <td class="fw-medium">${r.workDate}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty r.checkInTime}">${r.checkInTime}</c:when>
                                                <c:otherwise><span class="text-muted">&mdash;</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty r.checkOutTime}">${r.checkOutTime}</c:when>
                                                <c:otherwise><span class="text-muted">&mdash;</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${r.attendanceStatus == 'Present'}">
                                                    <span class="badge badge-status-present">
                                                        <i class="bi bi-check-circle-fill me-1"></i>Present
                                                    </span>
                                                </c:when>
                                                <c:when test="${r.attendanceStatus == 'Absent'}">
                                                    <span class="badge badge-status-absent">
                                                        <i class="bi bi-x-circle-fill me-1"></i>Absent
                                                    </span>
                                                </c:when>
                                                <c:when test="${r.attendanceStatus == 'Late'}">
                                                    <span class="badge badge-status-late">
                                                        <i class="bi bi-clock me-1"></i>Late
                                                    </span>
                                                </c:when>
                                                <c:when test="${r.attendanceStatus == 'Leave'}">
                                                    <span class="badge badge-status-leave">
                                                        <i class="bi bi-calendar-check me-1"></i>Leave
                                                    </span>
                                                </c:when>
                                                <c:when test="${r.attendanceStatus == 'Holiday'}">
                                                    <span class="badge badge-status-holiday">
                                                        <i class="bi bi-star-fill me-1"></i>Holiday
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-secondary">${r.attendanceStatus}</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${r.lateMinutes > 0}">
                                                    <span class="text-warning fw-medium">${r.lateMinutes} min</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted">0</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${r.latePenaltyAmount != null and r.latePenaltyAmount > 0}">
                                                    <span class="text-danger fw-medium">
                                                        <fmt:formatNumber value="${r.latePenaltyAmount}" type="number" maxFractionDigits="0"/>
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted">0</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${r.overtimeHours != null and r.overtimeHours > 0}">
                                                    <span class="text-primary fw-medium">${r.overtimeHours}</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted">0</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${r.verificationStatus == 'Verified'}">
                                                    <span class="badge badge-verified">
                                                        <i class="bi bi-patch-check-fill me-1"></i>Verified
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge badge-pending">
                                                        <i class="bi bi-clock me-1"></i>Pending
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty r.verifiedByFullName}">
                                                    ${r.verifiedByFullName}
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted">&mdash;</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty r.note}">
                                                    <span title="${r.note}">${fn:substring(r.note, 0, 40)}${fn:length(r.note) > 40 ? '...' : ''}</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted">&mdash;</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <c:if test="${managerScope}">
                                            <td class="text-center">
                                                <c:if test="${r.verificationStatus != 'Verified'}">
                                                    <div class="d-flex justify-content-center gap-1">
                                                        <a href="${pageContext.request.contextPath}/attendance?action=edit&id=${r.attendanceId}"
                                                           class="btn btn-sm btn-outline-primary" title="Edit">
                                                            <i class="bi bi-pencil-square"></i>
                                                        </a>
                                                        <form method="post"
                                                              action="${pageContext.request.contextPath}/attendance?action=verify&id=${r.attendanceId}"
                                                              class="d-inline"
                                                              onsubmit="return confirm('Verify this record?');">
                                                            <button type="submit" class="btn btn-sm btn-outline-success" title="Verify">
                                                                <i class="bi bi-check-lg"></i>
                                                            </button>
                                                        </form>
                                                    </div>
                                                </c:if>
                                            </td>
                                        </c:if>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>

                    <%-- Pagination --%>
                    <c:if test="${totalPages > 1}">
                        <div class="d-flex justify-content-between align-items-center px-3 py-2 border-top">
                            <small class="text-muted">
                                Page ${currentPage} of ${totalPages} &middot; ${totalRecords} record(s)
                            </small>
                            <nav aria-label="Attendance pages">
                                <ul class="pagination pagination-sm mb-0">
                                    <c:set var="baseUrl" value="${pageContext.request.contextPath}/attendance?action=employeeDetail&employeeId=${employeeId}"/>
                                    <c:if test="${not empty fromDate}"><c:set var="baseUrl" value="${baseUrl}&fromDate=${fromDate}"/></c:if>
                                    <c:if test="${not empty toDate}"><c:set var="baseUrl" value="${baseUrl}&toDate=${toDate}"/></c:if>

                                    <li class="page-item ${currentPage <= 1 ? 'disabled' : ''}">
                                        <a class="page-link" href="${baseUrl}&page=${currentPage - 1}">&laquo;</a>
                                    </li>

                                    <c:forEach var="p" begin="1" end="${totalPages}">
                                        <c:if test="${p == 1 || p == totalPages || (p >= currentPage - 2 && p <= currentPage + 2)}">
                                            <li class="page-item ${p == currentPage ? 'active' : ''}">
                                                <a class="page-link" href="${baseUrl}&page=${p}">${p}</a>
                                            </li>
                                        </c:if>
                                        <c:if test="${(p == currentPage - 3 && p > 1) || (p == currentPage + 3 && p < totalPages)}">
                                            <li class="page-item disabled"><span class="page-link">&hellip;</span></li>
                                        </c:if>
                                    </c:forEach>

                                    <li class="page-item ${currentPage >= totalPages ? 'disabled' : ''}">
                                        <a class="page-link" href="${baseUrl}&page=${currentPage + 1}">&raquo;</a>
                                    </li>
                                </ul>
                            </nav>
                        </div>
                    </c:if>
                </c:when>
                <c:otherwise>
                    <div class="text-center text-muted py-5">
                        <i class="bi bi-calendar2-x fs-2 d-block mb-2 opacity-25"></i>
                        No attendance records found
                        <c:if test="${not empty fromDate or not empty toDate}">for the selected date range</c:if>.
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- Back button --%>
    <div class="mt-3">
        <a href="${pageContext.request.contextPath}/attendance" class="btn btn-sm btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Back to Attendance
        </a>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

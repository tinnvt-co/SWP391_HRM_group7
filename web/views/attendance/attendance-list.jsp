<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="attendanceList" scope="request"/>
<c:set var="todayMax" value="<%= java.time.LocalDate.now().toString() %>"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Attendance Records &mdash; HRM System</title>
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
        .att-pill {
            padding: 4px 12px; border-radius: 20px; font-size: 0.75rem; font-weight: 600;
            display: inline-flex; align-items: center; gap: 0.35rem;
        }
        .att-present  { background:#e6f9f0; color:#166534; }
        .att-absent   { background:#fee2e2; color:#b91c1c; }
        .att-late     { background:#fff8e1; color:#a16207; }
        .att-leave    { background:#e3f0fb; color:#1a3c5e; }
        .att-holiday  { background:#fef3c7; color:#854d0e; }
        .att-unpaid   { background:#f3e8ff; color:#6b21a8; }
        .verify-pill {
            padding: 3px 10px; border-radius: 20px; font-size: 0.72rem; font-weight: 600;
        }
        .verify-pending  { background:#fff8e1; color:#a16207; }
        .verify-verified { background:#e6f9f0; color:#166534; }
        .verify-rejected { background:#fee2e2; color:#b91c1c; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
            <h5 class="fw-bold text-dark mb-0">Attendance Records</h5>
            <small class="text-muted">
                <c:choose>
                    <c:when test="${managerScope}">Daily attendance of employees you manage</c:when>
                    <c:when test="${orgWide}">Daily attendance across the organization</c:when>
                    <c:otherwise>Your daily attendance records</c:otherwise>
                </c:choose>
            </small>
        </div>
        <c:if test="${fn:contains(permissions, 'VERIFY_STAFF_ATTENDANCE')}">
            <a href="${pageContext.request.contextPath}/attendance?action=add"
               class="btn btn-primary btn-sm px-3 fw-medium"
               style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                <i class="bi bi-plus-circle me-2"></i>New Record
            </a>
        </c:if>
    </div>

    <c:if test="${param.created == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i>
            <span>Attendance record created successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.updated == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i>
            <span>Attendance record updated successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.deleted == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i>
            <span>Attendance record deleted successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.verified == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i>
            <span>Attendance record verified successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.error == 'already-verified'}">
        <div class="alert alert-warning d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-triangle-fill"></i>
            <span>This record is already verified and cannot be modified or deleted.</span>
        </div>
    </c:if>

    <div class="card border-0 shadow-sm rounded-3 mb-3">
        <div class="card-body">
            <form method="get" class="row g-2 align-items-end">
                <c:if test="${managerScope or orgWide}">
                    <div class="col-md-4">
                        <label class="form-label small text-muted mb-1">Employee</label>
                        <select name="employeeId" class="form-select form-select-sm">
                            <option value="">-- All employees --</option>
                            <c:forEach var="e" items="${scopeEmployees}">
                                <option value="${e.employeeId}"
                                        ${employeeIdFilter == e.employeeId ? 'selected' : ''}>
                                    ${e.fullName} (${e.employeeCode})
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                </c:if>
                <div class="col-md-3">
                    <label class="form-label small text-muted mb-1">From</label>
                    <input type="date" name="fromDate" class="form-control form-control-sm"
                           max="${todayMax}" value="${fromDate}">
                </div>
                <div class="col-md-3">
                    <label class="form-label small text-muted mb-1">To</label>
                    <input type="date" name="toDate" class="form-control form-control-sm"
                           max="${todayMax}" value="${toDate}">
                </div>
                <div class="col-md-2 d-flex gap-2">
                    <button type="submit" class="btn btn-sm btn-primary flex-grow-1"
                            style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                        <i class="bi bi-funnel me-1"></i>Filter
                    </button>
                    <a href="${pageContext.request.contextPath}/attendance"
                       class="btn btn-sm btn-outline-secondary">Clear</a>
                </div>
            </form>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="p-3 border-bottom d-flex align-items-center gap-2">
                <i class="bi bi-search text-muted"></i>
                <input type="text" id="searchInput" class="form-control form-control-sm border-0 shadow-none"
                       placeholder="Search by employee, status, note..." style="max-width:340px;">
            </div>

            <div class="table-responsive">
                <table class="table table-hover mb-0" id="attendanceTable">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">#</th>
                            <th>Employee</th>
                            <th>Date</th>
                            <th>Check-in</th>
                            <th>Check-out</th>
                            <th>Hours</th>
                            <th>OT</th>
                            <th>Status</th>
                            <th>Verified</th>
                            <th>Note</th>
                            <th class="text-end pe-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="r" items="${records}" varStatus="s">
                            <tr>
                                <td class="ps-4 text-muted">${s.index + 1}</td>
                                <td>
                                    <div class="d-flex align-items-center gap-2">
                                        <div class="avatar-sm">${fn:substring(r.employeeFullName, 0, 1)}</div>
                                        <div>
                                            <div class="fw-medium">${r.employeeFullName}</div>
                                            <div class="text-muted" style="font-size:0.78rem;">${r.employeeCode}</div>
                                        </div>
                                    </div>
                                </td>
                                <td class="fw-medium">${r.workDate}</td>
                                <td class="text-muted">
                                    <c:choose>
                                        <c:when test="${not empty r.checkInTime}">${r.checkInTime}</c:when>
                                        <c:otherwise>&mdash;</c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-muted">
                                    <c:choose>
                                        <c:when test="${not empty r.checkOutTime}">${r.checkOutTime}</c:when>
                                        <c:otherwise>&mdash;</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${r.workingHours}</td>
                                <td>${r.overtimeHours}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${r.attendanceStatus == 'Present'}">
                                            <span class="att-pill att-present"><i class="bi bi-check-circle-fill"></i>Present</span>
                                        </c:when>
                                        <c:when test="${r.attendanceStatus == 'Absent'}">
                                            <span class="att-pill att-absent"><i class="bi bi-x-circle-fill"></i>Absent</span>
                                        </c:when>
                                        <c:when test="${r.attendanceStatus == 'Late'}">
                                            <span class="att-pill att-late"><i class="bi bi-clock-fill"></i>Late</span>
                                        </c:when>
                                        <c:when test="${r.attendanceStatus == 'Leave'}">
                                            <span class="att-pill att-leave"><i class="bi bi-calendar-check"></i>Leave</span>
                                        </c:when>
                                        <c:when test="${r.attendanceStatus == 'Holiday'}">
                                            <span class="att-pill att-holiday"><i class="bi bi-flag-fill"></i>Holiday</span>
                                        </c:when>
                                        <c:when test="${r.attendanceStatus == 'UnpaidLeave'}">
                                            <span class="att-pill att-unpaid"><i class="bi bi-cash-stack"></i>Unpaid Leave</span>
                                        </c:when>
                                        <c:otherwise>${r.attendanceStatus}</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${r.verificationStatus == 'Verified'}">
                                            <span class="verify-pill verify-verified">Verified</span>
                                            <c:if test="${not empty r.verifiedByFullName}">
                                                <div class="text-muted" style="font-size:0.72rem;">by ${r.verifiedByFullName}</div>
                                            </c:if>
                                        </c:when>
                                        <c:when test="${r.verificationStatus == 'Pending'}">
                                            <span class="verify-pill verify-pending">Pending</span>
                                        </c:when>
                                        <c:when test="${r.verificationStatus == 'Rejected'}">
                                            <span class="verify-pill verify-rejected">Rejected</span>
                                        </c:when>
                                    </c:choose>
                                </td>
                                <td class="pe-4 small text-muted" style="max-width:200px;">
                                    <c:choose>
                                        <c:when test="${not empty r.note}">${r.note}</c:when>
                                        <c:otherwise>&mdash;</c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end pe-4">
                                    <c:if test="${fn:contains(permissions, 'VERIFY_STAFF_ATTENDANCE')
                                              and r.verificationStatus != 'Verified'}">
                                        <div class="d-flex justify-content-end gap-1">
                                            <form method="post"
                                                  action="${pageContext.request.contextPath}/attendance?action=verify"
                                                  class="d-inline"
                                                  onsubmit="return confirm('Verify attendance for ${r.employeeFullName} on ${r.workDate}?')">
                                                <input type="hidden" name="id" value="${r.attendanceId}">
                                                <button type="submit" class="btn btn-sm btn-outline-success" title="Verify">
                                                    <i class="bi bi-check2-circle me-1"></i>Verify
                                                </button>
                                            </form>
                                            <a href="${pageContext.request.contextPath}/attendance?action=edit&id=${r.attendanceId}"
                                               class="btn btn-sm btn-outline-primary" title="Edit">
                                                <i class="bi bi-pencil me-1"></i>Edit
                                            </a>
                                            <form method="post"
                                                  action="${pageContext.request.contextPath}/attendance?action=delete"
                                                  class="d-inline"
                                                  onsubmit="return confirm('Delete attendance for ${r.employeeFullName} on ${r.workDate}? This cannot be undone.')">
                                                <input type="hidden" name="id" value="${r.attendanceId}">
                                                <button type="submit" class="btn btn-sm btn-outline-danger" title="Delete">
                                                    <i class="bi bi-trash me-1"></i>Delete
                                                </button>
                                            </form>
                                        </div>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty records}">
                            <tr>
                                <td colspan="11" class="text-center text-muted py-5">
                                    <i class="bi bi-calendar2-x fs-2 d-block mb-2 opacity-25"></i>
                                    No attendance records found for this filter.
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
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            const q = this.value.toLowerCase();
            document.querySelectorAll('#attendanceTable tbody tr').forEach(row => {
                row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
            });
        });
    }
</script>
</body>
</html>

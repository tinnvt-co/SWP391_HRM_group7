<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="attendanceLeaveCalendar" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Leave Calendar &mdash; HRM System</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body { background-color:#f4f6f9; }
        .sidebar { width:240px; min-height:calc(100vh - 56px); background-color:#1a3c5e;
            position:fixed; top:56px; left:0; padding-top:1rem; z-index:100; }
        .sidebar .nav-link { color:rgba(255,255,255,0.75); padding:0.6rem 1.25rem;
            border-radius:6px; margin:2px 10px; font-size:0.9rem; }
        .sidebar .nav-link:hover, .sidebar .nav-link.active { color:#fff; background-color:rgba(255,255,255,0.12); }
        .sidebar .nav-link i { width:20px; }
        .sidebar-label { font-size:0.7rem; text-transform:uppercase; letter-spacing:1px;
            color:rgba(255,255,255,0.4); padding:0.75rem 1.25rem 0.25rem; }
        .main-content { margin-left:240px; padding:2rem; }
        .table th { font-size:0.78rem; text-transform:uppercase; letter-spacing:0.5px; color:#6b7280; font-weight:600; }
        .table td { vertical-align:middle; font-size:0.9rem; }
        .stat-card { border:0; border-radius:12px; box-shadow:0 2px 10px rgba(0,0,0,0.06); }
        .stat-icon { width:42px; height:42px; border-radius:10px; display:flex; align-items:center; justify-content:center; font-size:1.1rem; }
        .code-pill { min-width:42px; display:inline-flex; justify-content:center; padding:4px 10px;
            border-radius:999px; font-weight:700; font-size:0.78rem; }
        .code-paid { background:#e6f9f0; color:#166534; }
        .code-unpaid { background:#fff8e1; color:#a16207; }
        .reason-cell { max-width:280px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
        @media (max-width:768px) {
            .sidebar { position:static; width:100%; min-height:auto; }
            .main-content { margin-left:0; padding:1rem; }
        }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
            <h5 class="fw-bold text-dark mb-0">Leave Calendar</h5>
            <small class="text-muted">Approved leave days used to fill monthly attendance sheets</small>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-3 mb-3">
        <div class="card-body">
            <form method="get" action="${pageContext.request.contextPath}/leave-requests" class="row g-2 align-items-end">
                <input type="hidden" name="action" value="attendance">
                <div class="col-lg-3 col-md-6">
                    <label class="form-label small text-muted mb-1">Department</label>
                    <select name="deptId" class="form-select form-select-sm" onchange="this.form.employeeId.value=''; this.form.submit();">
                        <option value="" ${empty selectedDeptId ? 'selected' : ''}>All attendance departments</option>
                        <c:forEach var="dept" items="${departments}">
                            <option value="${dept.departmentId}" ${dept.departmentId == selectedDeptId ? 'selected' : ''}>
                                ${dept.departmentName}
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-lg-3 col-md-6">
                    <label class="form-label small text-muted mb-1">Employee</label>
                    <select name="employeeId" class="form-select form-select-sm">
                        <option value="" ${empty selectedEmployeeId ? 'selected' : ''}>All employees</option>
                        <c:forEach var="emp" items="${employees}">
                            <option value="${emp.employeeId}" ${emp.employeeId == selectedEmployeeId ? 'selected' : ''}>
                                ${emp.fullName} (${emp.employeeCode})
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-lg-2 col-md-4">
                    <label class="form-label small text-muted mb-1">Month</label>
                    <select name="month" class="form-select form-select-sm">
                        <c:forEach var="m" begin="1" end="12">
                            <option value="${m}" ${m == selectedMonth ? 'selected' : ''}>Month ${m}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-lg-2 col-md-4">
                    <label class="form-label small text-muted mb-1">Year</label>
                    <input type="number" name="year" class="form-control form-control-sm"
                           value="${selectedYear}" min="2020" max="2100">
                </div>
                <div class="col-lg-2 col-md-4">
                    <button type="submit" class="btn btn-sm btn-primary w-100"
                            style="background:#1a3c5e;border:none;">
                        <i class="bi bi-search me-1"></i>View
                    </button>
                </div>
            </form>
        </div>
    </div>

    <div class="row g-3 mb-3">
        <div class="col-sm-6 col-xl-3">
            <div class="stat-card card p-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="stat-icon" style="background:#e3f0fb;color:#1a3c5e;">
                        <i class="bi bi-calendar2-week"></i>
                    </div>
                    <div>
                        <div class="text-muted small">Leave Days</div>
                        <div class="fw-bold fs-5">${totalLeaveDays}</div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-sm-6 col-xl-3">
            <div class="stat-card card p-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="stat-icon" style="background:#e6f9f0;color:#166534;">
                        <i class="bi bi-people"></i>
                    </div>
                    <div>
                        <div class="text-muted small">Employees On Leave</div>
                        <div class="fw-bold fs-5">${totalEmployeesOnLeave}</div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-sm-6 col-xl-3">
            <div class="stat-card card p-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="stat-icon" style="background:#ecfdf3;color:#166534;">
                        <i class="bi bi-check2-circle"></i>
                    </div>
                    <div>
                        <div class="text-muted small">Paid Leave</div>
                        <div class="fw-bold fs-5">${paidLeaveDays}</div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-sm-6 col-xl-3">
            <div class="stat-card card p-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="stat-icon" style="background:#fff8e1;color:#a16207;">
                        <i class="bi bi-cash-stack"></i>
                    </div>
                    <div>
                        <div class="text-muted small">Unpaid Leave</div>
                        <div class="fw-bold fs-5">${unpaidLeaveDays}</div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="p-3 border-bottom d-flex align-items-center justify-content-between flex-wrap gap-2">
                <div>
                    <span class="fw-medium">${monthLabel}</span>
                    <span class="text-muted ms-2">${totalLeaveDays} approved leave day(s)</span>
                </div>
                <div class="d-flex align-items-center gap-2">
                    <i class="bi bi-search text-muted"></i>
                    <input type="text" id="searchInput" class="form-control form-control-sm"
                           placeholder="Search employee, department, reason..." style="max-width:320px;">
                </div>
            </div>

            <div class="table-responsive">
                <table class="table table-hover mb-0" id="leaveCalendarTable">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">#</th>
                            <th>Date</th>
                            <th>Employee</th>
                            <th>Department</th>
                            <th>Leave Type</th>
                            <th class="text-center">Attendance Code</th>
                            <th>Attendance Status</th>
                            <th>Reason</th>
                            <th class="text-end pe-4">Request</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="day" items="${leaveDays}" varStatus="s">
                            <tr>
                                <td class="ps-4 text-muted">${s.index + 1}</td>
                                <td class="fw-medium">${day.leaveDate}</td>
                                <td>
                                    <div class="fw-medium">${day.employeeFullName}</div>
                                    <div class="text-muted" style="font-size:0.78rem;">${day.employeeCode}</div>
                                </td>
                                <td>${day.departmentName}</td>
                                <td>${day.leaveTypeLabel}</td>
                                <td class="text-center">
                                    <span class="code-pill ${day.attendanceCode == 'UL' ? 'code-unpaid' : 'code-paid'}">
                                        ${day.attendanceCode}
                                    </span>
                                </td>
                                <td>${day.attendanceStatus}</td>
                                <td class="reason-cell text-muted" title="${day.reason}">${day.reason}</td>
                                <td class="text-end pe-4">
                                    <c:choose>
                                        <c:when test="${permissions.contains('VIEW_LEAVE_REQUEST_DETAIL')}">
                                            <a class="btn btn-sm btn-outline-primary"
                                               href="${pageContext.request.contextPath}/leave-requests?action=detail&id=${day.leaveRequestId}">
                                                <i class="bi bi-eye me-1"></i>View
                                            </a>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted">#${day.leaveRequestId}</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty leaveDays}">
                            <tr>
                                <td colspan="9" class="text-center text-muted py-5">
                                    <i class="bi bi-calendar-x fs-2 d-block mb-2 opacity-25"></i>
                                    No approved leave days found for ${monthLabel}.
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
            document.querySelectorAll('#leaveCalendarTable tbody tr').forEach(row => {
                row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
            });
        });
    }
</script>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="activePage" value="attendanceReport" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Attendance Reports &mdash; HRM System</title>
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
        .status-pill { padding: 3px 10px; border-radius: 20px; font-size: 0.72rem; font-weight: 600; }
        .st-submitted { background:#e3f0fb; color:#1a3c5e; }
        .st-reviewed  { background:#e6f9f0; color:#166534; }
        .st-rejected  { background:#fee2e2; color:#b91c1c; }
        .st-final     { background:#fef3c7; color:#854d0e; }
        .st-pending-hr { background:#fff7ed; color:#9a3412; }
        .st-approved-hr { background:#dcfce7; color:#166534; }
        .task-card { border: none; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.07);
            transition: box-shadow 0.2s, transform 0.2s; }
        .task-card-link { color: inherit; text-decoration: none; display: block; }
        .task-card-link:hover .task-card { box-shadow: 0 4px 16px rgba(0,0,0,0.11); transform: translateY(-1px); }
        .task-icon { width: 48px; height: 48px; border-radius: 12px; background:#fff8e1;
            display:flex; align-items:center; justify-content:center; font-size:1.25rem; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
            <h5 class="fw-bold text-dark mb-0">Attendance Reports</h5>
            <small class="text-muted">
                <c:choose>
                    <c:when test="${managerScope}">Monthly attendance reports for your employees</c:when>
                    <c:otherwise>Monthly attendance reports submitted by managers</c:otherwise>
                </c:choose>
            </small>
        </div>
        <c:if test="${canSubmitToHrManager}">
            <form method="post"
                  action="${pageContext.request.contextPath}/attendance-report?action=submitToHrManager"
                  onsubmit="return confirm('Submit all ready attendance reports in the selected department to HR Manager?');">
                <input type="hidden" name="month" value="${selectedMonth}">
                <input type="hidden" name="year" value="${selectedYear}">
                <input type="hidden" name="deptId" value="${selectedDeptId}">
                <button type="submit" class="btn btn-primary btn-sm px-3 fw-medium"
                        style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;"
                        ${canSubmitSelectedDepartment ? '' : 'disabled'}
                        title="${canSubmitSelectedDepartment ? 'Submit all ready reports in the selected department' : 'All attendance records must be confirmed before submission'}">
                    <i class="bi bi-send me-2"></i>Submit Department
                    <c:if test="${readyToSubmitCount > 0}">
                        <span class="badge bg-light text-primary ms-1">${readyToSubmitCount}</span>
                    </c:if>
                </button>
            </form>
        </c:if>
        <c:if test="${canApproveAttendanceReport}">
            <form method="post"
                  action="${pageContext.request.contextPath}/attendance-report?action=approveAll"
                  onsubmit="return confirm('Approve all pending attendance reports in the selected department?');">
                <input type="hidden" name="month" value="${selectedMonth}">
                <input type="hidden" name="year" value="${selectedYear}">
                <input type="hidden" name="deptId" value="${selectedDeptId}">
                <button type="submit" class="btn btn-success btn-sm px-3 fw-medium"
                        ${pendingHrManagerApprovalCount > 0 ? '' : 'disabled'}
                        title="${pendingHrManagerApprovalCount > 0 ? 'Approve all pending reports in the selected department' : 'No pending reports in this department'}">
                    <i class="bi bi-check2-all me-2"></i>Approve Department
                    <c:if test="${pendingHrManagerApprovalCount > 0}">
                        <span class="badge bg-light text-success ms-1">${pendingHrManagerApprovalCount}</span>
                    </c:if>
                </button>
            </form>
        </c:if>
    </div>

    <c:if test="${not empty attendanceTask}">
        <c:url var="attendanceWorkflowTaskUrl" value="/attendance-report">
            <c:param name="deptId" value="${attendanceTask.departmentId}"/>
            <c:param name="month" value="${attendanceTask.month}"/>
            <c:param name="year" value="${attendanceTask.year}"/>
        </c:url>
    </c:if>
    <c:if test="${not empty hrDepartmentConfirmationTask}">
        <c:url var="hrDepartmentConfirmationTaskUrl" value="/attendance">
            <c:param name="deptId" value="${hrDepartmentConfirmationTask.departmentId}"/>
            <c:param name="month" value="${hrDepartmentConfirmationTask.month}"/>
            <c:param name="year" value="${hrDepartmentConfirmationTask.year}"/>
        </c:url>
    </c:if>
    <c:if test="${not empty payrollTaskSummary and payrollTaskSummary.actionable}">
        <c:url var="attendancePayrollTaskUrl" value="/payroll">
            <c:if test="${payrollTaskApproval}">
                <c:param name="action" value="approval"/>
            </c:if>
            <c:param name="month" value="${payrollTaskSummary.month}"/>
            <c:param name="year" value="${payrollTaskSummary.year}"/>
            <c:if test="${not empty payrollTaskSummary.departmentId}">
                <c:param name="deptId" value="${payrollTaskSummary.departmentId}"/>
            </c:if>
        </c:url>
    </c:if>
    <c:if test="${hrStaffScope or hrManagerScope}">
        <div class="row g-3 mb-3">
            <div class="col-lg-4 col-md-6">
                <a class="task-card-link ${empty attendanceTask ? 'pe-none' : ''}"
                   href="${not empty attendanceTask ? attendanceWorkflowTaskUrl : '#'}"
                   aria-disabled="${empty attendanceTask ? 'true' : 'false'}">
                    <div class="task-card card p-3">
                        <div class="d-flex align-items-center gap-3">
                            <div class="task-icon" style="background:#e3f0fb;">
                                <i class="bi bi-calendar2-check text-primary"></i>
                            </div>
                            <div>
                                <div class="text-muted small">Attendance Tasks to Process</div>
                                <div class="fw-bold fs-5">${empty attendanceTask ? 0 : attendanceTask.count}</div>
                                <div class="text-muted" style="font-size:0.78rem;">
                                    <c:choose>
                                        <c:when test="${not empty attendanceTask}">
                                            Next: ${attendanceTask.actionLabel} &middot;
                                            <c:out value="${attendanceTask.departmentName}"/> &middot;
                                            Month ${attendanceTask.month}/${attendanceTask.year}
                                        </c:when>
                                        <c:otherwise>No pending attendance report tasks</c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </div>
                </a>
            </div>
            <c:if test="${hrManagerScope}">
                <div class="col-lg-4 col-md-6">
                    <a class="task-card-link ${empty hrDepartmentConfirmationTask ? 'pe-none' : ''}"
                       href="${not empty hrDepartmentConfirmationTask ? hrDepartmentConfirmationTaskUrl : '#'}"
                       aria-disabled="${empty hrDepartmentConfirmationTask ? 'true' : 'false'}">
                        <div class="task-card card p-3">
                            <div class="d-flex align-items-center gap-3">
                                <div class="task-icon" style="background:#e6f9f0;">
                                    <i class="bi bi-person-check text-success"></i>
                                </div>
                                <div>
                                    <div class="text-muted small">HR Attendance Tasks</div>
                                    <div class="fw-bold fs-5">${empty hrDepartmentConfirmationTask ? 0 : hrDepartmentConfirmationTask.count}</div>
                                    <div class="text-muted" style="font-size:0.78rem;">
                                        <c:choose>
                                            <c:when test="${not empty hrDepartmentConfirmationTask}">
                                                Next: Review Attendance &middot;
                                                <c:out value="${hrDepartmentConfirmationTask.departmentName}"/> &middot;
                                                Month ${hrDepartmentConfirmationTask.month}/${hrDepartmentConfirmationTask.year}
                                            </c:when>
                                            <c:otherwise>No HR attendance awaiting review</c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </a>
                </div>
            </c:if>
            <div class="col-lg-4 col-md-6">
                <a class="task-card-link ${payrollTaskSummary.actionable ? '' : 'pe-none'}"
                   href="${payrollTaskSummary.actionable ? attendancePayrollTaskUrl : '#'}"
                   aria-disabled="${payrollTaskSummary.actionable ? 'false' : 'true'}">
                    <div class="task-card card p-3">
                        <div class="d-flex align-items-center gap-3">
                            <div class="task-icon">
                                <i class="bi bi-list-task text-warning"></i>
                            </div>
                            <div>
                                <div class="text-muted small">Payroll Tasks to Process</div>
                                <div class="fw-bold fs-5">${payrollTaskSummary.count}</div>
                                <div class="text-muted" style="font-size:0.78rem;">
                                    <c:choose>
                                        <c:when test="${payrollTaskSummary.actionable}">
                                            ${payrollTaskSummary.taskLabel}
                                            <c:if test="${not empty payrollTaskSummary.departmentName}">
                                                &middot; ${payrollTaskSummary.departmentName}
                                            </c:if>
                                            &middot; Month ${payrollTaskSummary.month}/${payrollTaskSummary.year}
                                        </c:when>
                                        <c:otherwise>No pending payroll tasks</c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </div>
                </a>
            </div>
        </div>
    </c:if>

    <div class="card border-0 shadow-sm rounded-3 mb-3">
        <div class="card-body">
            <form method="get" class="row g-2 align-items-end">
                <c:if test="${departmentScope}">
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Department</label>
                        <select name="deptId" class="form-select form-select-sm">
                            <c:forEach var="dept" items="${departments}">
                                <option value="${dept.departmentId}"
                                        ${dept.departmentId == selectedDeptId ? 'selected' : ''}>
                                    <c:out value="${dept.departmentName}"/>
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                </c:if>
                <div class="col-md-3">
                    <label class="form-label small text-muted mb-1">Month</label>
                    <select name="month" class="form-select form-select-sm">
                        <c:forEach var="m" begin="1" end="12">
                            <c:choose>
                                <c:when test="${m == selectedMonth}">
                                    <option value="${m}" selected="selected">Month ${m}</option>
                                </c:when>
                                <c:otherwise>
                                    <option value="${m}">Month ${m}</option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-3">
                    <label class="form-label small text-muted mb-1">Year</label>
                    <input type="number" name="year" class="form-control form-control-sm"
                           value="${selectedYear}" min="2020" max="2100">
                </div>
                <div class="col-md-2 d-flex gap-2">
                    <button type="submit" class="btn btn-sm btn-primary flex-grow-1"
                            style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                        <i class="bi bi-funnel me-1"></i>View
                    </button>
                </div>
            </form>
        </div>
    </div>

    <c:if test="${not empty attendanceReportMessage}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span><c:out value="${attendanceReportMessage}"/></span>
        </div>
    </c:if>
    <c:if test="${not empty attendanceReportError}">
        <div class="alert alert-danger d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-circle-fill"></i><span><c:out value="${attendanceReportError}"/></span>
        </div>
    </c:if>

    <c:if test="${hrStaffScope and pendingDepartmentConfirmationCount > 0}">
        <div class="alert alert-warning d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-hourglass-split"></i>
            <span>${pendingDepartmentConfirmationCount} attendance record(s) in
                <strong><c:out value="${selectedDeptName}"/></strong> are still waiting for department confirmation.
                This department cannot be submitted yet.</span>
        </div>
    </c:if>

    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="p-3 border-bottom d-flex align-items-center gap-2">
                <i class="bi bi-calendar-check text-muted"></i>
                <span class="fw-medium">${monthLabel}</span>
                <c:if test="${departmentScope and not empty selectedDeptName}">
                    <span class="text-muted">&middot; <c:out value="${selectedDeptName}"/></span>
                </c:if>
                <span class="text-muted">&middot; ${totalReports} report(s)</span>
            </div>

            <div class="table-responsive">
                <table class="table table-hover mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">#</th>
                            <th>Employee</th>
                            <th>Department</th>
                            <th>Manager</th>
                            <th class="text-center">Work Days</th>
                            <th class="text-center">Paid Leave</th>
                            <th class="text-center">Unpaid Leave</th>
                            <th class="text-center">OT Hours</th>
                            <th class="text-end">Late Penalty</th>
                            <th>Status</th>
                            <th>Submitted</th>
                            <c:if test="${canApproveAttendanceReport}">
                                <th class="text-center">Actions</th>
                            </c:if>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="r" items="${reports}" varStatus="s">
                            <tr>
                                <td class="ps-4 text-muted">${(currentPage - 1) * 10 + s.index + 1}</td>
                                <td>
                                    <div class="d-flex align-items-center gap-2">
                                        <div class="avatar-sm">${fn:substring(r.employeeFullName, 0, 1)}</div>
                                        <div>
                                            <div class="fw-medium">${r.employeeFullName}</div>
                                            <div class="text-muted" style="font-size:0.78rem;">${r.employeeCode}</div>
                                        </div>
                                    </div>
                                </td>
                                <td>${r.departmentName}</td>
                                <td class="text-muted">${r.managerFullName}</td>
                                <td class="text-center fw-medium">${r.actualWorkingDays}</td>
                                <td class="text-center">${r.paidLeaveDays}</td>
                                <td class="text-center">${r.unpaidLeaveDays}</td>
                                <td class="text-center">${r.overtimeHours}</td>
                                <td class="text-end text-danger">
                                    <fmt:formatNumber value="${r.latePenaltyAmount}" type="number" maxFractionDigits="0"/>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${r.status == 'SubmittedToHrStaff'}">
                                            <span class="status-pill st-submitted">With HR Staff</span>
                                        </c:when>
                                        <c:when test="${r.status == 'ReviewedByHrStaff'}">
                                            <span class="status-pill st-reviewed">Reviewed</span>
                                        </c:when>
                                        <c:when test="${r.status == 'RejectedByHrStaff'}">
                                            <span class="status-pill st-rejected">Rejected</span>
                                        </c:when>
                                        <c:when test="${r.status == 'FinalSubmitted'}">
                                            <span class="status-pill st-final">Final</span>
                                        </c:when>
                                        <c:when test="${r.status == 'PendingHrManagerApproval'}">
                                            <span class="status-pill st-pending-hr">Pending HR Manager</span>
                                        </c:when>
                                        <c:when test="${r.status == 'ApprovedByHrManager'}">
                                            <span class="status-pill st-approved-hr">Approved</span>
                                        </c:when>
                                        <c:when test="${r.status == 'RejectedByHrManager'}">
                                            <span class="status-pill st-rejected">Rejected by HR Manager</span>
                                            <c:if test="${not empty r.hrNote}">
                                                <div class="text-danger mt-1" style="font-size:0.76rem;max-width:240px;white-space:normal;">
                                                    <i class="bi bi-chat-left-text me-1"></i>
                                                    <strong>Reason:</strong> <c:out value="${r.hrNote}"/>
                                                </div>
                                            </c:if>
                                        </c:when>
                                        <c:otherwise>${r.status}</c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-muted" style="font-size:0.82rem;">${r.submittedAt}</td>
                                <c:if test="${canApproveAttendanceReport}">
                                    <td class="text-center">
                                        <c:choose>
                                            <c:when test="${r.status == 'PendingHrManagerApproval'}">
                                                <div class="d-inline-flex gap-1">
                                                    <form method="post" action="${pageContext.request.contextPath}/attendance-report?action=approve"
                                                          onsubmit="return confirm('Approve this attendance report?');">
                                                        <input type="hidden" name="reportId" value="${r.attendanceReportId}">
                                                        <input type="hidden" name="month" value="${selectedMonth}">
                                                        <input type="hidden" name="year" value="${selectedYear}">
                                                        <input type="hidden" name="deptId" value="${selectedDeptId}">
                                                        <button type="submit" class="btn btn-sm btn-outline-success" title="Approve">
                                                            <i class="bi bi-check2"></i>
                                                        </button>
                                                    </form>
                                                    <form method="post" action="${pageContext.request.contextPath}/attendance-report?action=reject"
                                                          onsubmit="const reason = prompt('Reason for rejection:'); if (!reason) return false; this.note.value = reason; return true;">
                                                        <input type="hidden" name="reportId" value="${r.attendanceReportId}">
                                                        <input type="hidden" name="month" value="${selectedMonth}">
                                                        <input type="hidden" name="year" value="${selectedYear}">
                                                        <input type="hidden" name="deptId" value="${selectedDeptId}">
                                                        <input type="hidden" name="note" value="">
                                                        <button type="submit" class="btn btn-sm btn-outline-danger" title="Reject">
                                                            <i class="bi bi-x-lg"></i>
                                                        </button>
                                                    </form>
                                                </div>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted">&mdash;</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </c:if>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty reports}">
                            <tr>
                                <td colspan="${canApproveAttendanceReport ? 12 : 11}" class="text-center text-muted py-5">
                                    <i class="bi bi-inbox fs-2 d-block mb-2 opacity-25"></i>
                                    No attendance reports submitted for ${monthLabel}.
                                    <c:if test="${departmentScope and not empty selectedDeptName}">
                                        <span><c:out value="${selectedDeptName}"/>.</span>
                                    </c:if>
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
                    Page ${currentPage} of ${totalPages} &middot; ${totalReports} report(s)
                </small>
                <nav>
                    <ul class="pagination pagination-sm mb-0">
                        <li class="page-item ${currentPage == 1 ? 'disabled' : ''}">
                            <a class="page-link"
                               href="?month=${selectedMonth}&year=${selectedYear}&deptId=${selectedDeptId}&page=${currentPage - 1}">Previous</a>
                        </li>
                        <c:forEach var="p" begin="1" end="${totalPages}">
                            <li class="page-item ${p == currentPage ? 'active' : ''}">
                                <a class="page-link"
                                   href="?month=${selectedMonth}&year=${selectedYear}&deptId=${selectedDeptId}&page=${p}">${p}</a>
                            </li>
                        </c:forEach>
                        <li class="page-item ${currentPage == totalPages ? 'disabled' : ''}">
                            <a class="page-link"
                               href="?month=${selectedMonth}&year=${selectedYear}&deptId=${selectedDeptId}&page=${currentPage + 1}">Next</a>
                        </li>
                    </ul>
                </nav>
            </div>
        </c:if>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

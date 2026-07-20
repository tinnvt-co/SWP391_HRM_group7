<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="attendanceList" scope="request"/>
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
        .sidebar { width: 240px; min-height: calc(100vh - 56px); background-color: #1a3c5e;
            position: fixed; top: 56px; left: 0; padding-top: 1rem; z-index: 100; }
        .sidebar .nav-link { color: rgba(255,255,255,0.75); padding: 0.6rem 1.25rem;
            border-radius: 6px; margin: 2px 10px; font-size: 0.9rem; transition: all 0.2s; }
        .sidebar .nav-link:hover, .sidebar .nav-link.active { color: #fff; background-color: rgba(255,255,255,0.12); }
        .sidebar .nav-link i { width: 20px; }
        .sidebar-label { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 1px;
            color: rgba(255,255,255,0.4); padding: 0.75rem 1.25rem 0.25rem; }
        .main-content { margin-left: 240px; padding: 2rem; }
        .emp-card { border: none; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
            transition: transform 0.15s, box-shadow 0.15s; cursor: pointer; text-decoration: none; color: inherit; }
        .emp-card:hover { transform: translateY(-3px); box-shadow: 0 6px 20px rgba(0,0,0,0.1); color: inherit; }
        .avatar-md { width: 48px; height: 48px; border-radius: 50%;
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            display: inline-flex; align-items: center; justify-content: center;
            font-size: 1.1rem; font-weight: 700; color: white; flex-shrink: 0; }
        .stat-badge { padding: 3px 10px; border-radius: 20px; font-size: 0.72rem; font-weight: 600; }
        .stat-pending  { background:#fff8e1; color:#a16207; }
        .stat-verified { background:#e6f9f0; color:#166534; }
        .stat-present  { background:#e6f9f0; color:#166534; }
        .stat-absent   { background:#fee2e2; color:#b91c1c; }
        .stat-leave    { background:#e3f0fb; color:#1a3c5e; }
        .dept-card { border: none; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
            transition: transform 0.15s, box-shadow 0.15s; }
        .dept-card:hover { transform: translateY(-3px); box-shadow: 0 6px 20px rgba(0,0,0,0.1); }
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
            <h5 class="fw-bold text-dark mb-0">Attendance Records</h5>
            <small class="text-muted">
                <c:choose>
                    <c:when test="${managerScope}">Review and confirm your department attendance, including your own &mdash; ${importMonthLabel}</c:when>
                    <c:when test="${hrScope}">Attendance across the organization &mdash; ${importMonthLabel}</c:when>
                    <c:otherwise>Your attendance records</c:otherwise>
                </c:choose>
            </small>
        </div>
        <div class="d-flex align-items-center justify-content-end gap-2 flex-wrap">
            <form method="get" action="${pageContext.request.contextPath}/attendance"
                  class="d-flex align-items-center gap-2">
                <c:if test="${not empty selectedDeptId}">
                    <input type="hidden" name="deptId" value="${selectedDeptId}">
                </c:if>
                <select name="month" class="form-select form-select-sm" style="width:130px;"
                        aria-label="Month" onchange="this.form.submit()">
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
                <select name="year" class="form-select form-select-sm" style="width:100px;"
                        aria-label="Year" onchange="this.form.submit()">
                    <c:forEach var="y" items="${yearOptions}">
                        <c:choose>
                            <c:when test="${y == selectedYear}">
                                <option value="${y}" selected="selected">${y}</option>
                            </c:when>
                            <c:otherwise>
                                <option value="${y}">${y}</option>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                </select>
            </form>
            <%-- Import button: HR Staff only --%>
            <c:if test="${canImportAttendance}">
                <button type="button" class="btn btn-primary btn-sm px-3 fw-medium"
                        style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;"
                        data-bs-toggle="modal" data-bs-target="#importModal">
                    <i class="bi bi-upload me-2"></i>Import Attendance
                </button>
            </c:if>
            <%-- Confirm button: Manager only --%>
            <c:if test="${managerScope}">
                <form method="post"
                      action="${pageContext.request.contextPath}/attendance?action=confirmToHr"
                      class="d-inline"
                      onsubmit="return confirm('Confirm ${pendingCount} pending attendance record(s) for your department, including your own attendance?');">
                    <input type="hidden" name="month" value="${selectedMonth}">
                    <input type="hidden" name="year" value="${selectedYear}">
                    <button type="submit" class="btn btn-success btn-sm px-3 fw-medium"
                            ${pendingCount > 0 ? '' : 'disabled'}
                            title="${pendingCount > 0 ? 'Confirm all attendance in your department' : 'No pending records to confirm'}">
                        <i class="bi bi-check2-all me-2"></i>Confirm Department Attendance
                        <c:if test="${pendingCount > 0}">
                            <span class="badge bg-light text-success ms-1">${pendingCount}</span>
                        </c:if>
                    </button>
                </form>
            </c:if>
        </div>
    </div>

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
    <c:if test="${not empty payrollTaskSummary}">
        <div class="row g-3 mb-3">
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
                                <div class="text-muted small">Tasks to Process</div>
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

    <%-- Flash messages --%>
    <c:if test="${not empty importMessage}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span><c:out value="${importMessage}"/></span>
        </div>
    </c:if>
    <c:if test="${not empty importError}">
        <div class="alert alert-danger d-flex align-items-start gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-octagon-fill mt-1"></i><span><c:out value="${importError}"/></span>
        </div>
    </c:if>

    <%-- ============ HR Staff: Department selector ============ --%>
    <c:if test="${hrScope}">
        <div class="card border-0 shadow-sm rounded-3 mb-4">
            <div class="card-body">
                <h6 class="fw-bold text-dark mb-3"><i class="bi bi-diagram-3 me-2"></i>Departments</h6>
                <div class="row g-3">
                    <c:forEach var="dept" items="${departments}">
                        <div class="col-md-3 col-sm-6">
                            <a href="${pageContext.request.contextPath}/attendance?deptId=${dept.departmentId}&year=${selectedYear}&month=${selectedMonth}"
                               class="card dept-card h-100 text-decoration-none
                                      ${selectedDeptId == dept.departmentId ? 'border border-primary border-2' : ''}">
                                <div class="card-body text-center py-3">
                                    <div class="rounded-circle mx-auto mb-2 d-flex align-items-center justify-content-center"
                                         style="width:44px;height:44px;background:linear-gradient(135deg,#1a3c5e,#2d6a9f);">
                                        <i class="bi bi-building text-white"></i>
                                    </div>
                                    <div class="fw-medium" style="font-size:0.88rem;">${dept.departmentName}</div>
                                </div>
                            </a>
                        </div>
                    </c:forEach>
                    <c:if test="${empty departments}">
                        <div class="col-12 text-center text-muted py-3">
                            <i class="bi bi-diagram-3 fs-2 d-block mb-2 opacity-25"></i>
                            No departments found.
                        </div>
                    </c:if>
                </div>
            </div>
        </div>
    </c:if>

    <%-- ============ Employee cards ============ --%>
    <c:if test="${not empty employeeCards}">
        <div class="card border-0 shadow-sm rounded-3 mb-3">
            <div class="card-body">
                <div class="d-flex align-items-center justify-content-between mb-3">
                    <h6 class="fw-bold text-dark mb-0">
                        <i class="bi bi-people me-2"></i>Employees
                        <span class="badge bg-secondary ms-1" style="font-size:0.72rem;">${fn:length(employeeCards)}</span>
                    </h6>
                    <input type="text" id="searchInput" class="form-control form-control-sm border-0 shadow-none"
                           placeholder="Search employee..." style="max-width:260px;">
                </div>
                <div class="row g-3" id="employeeGrid">
                    <c:forEach var="c" items="${employeeCards}">
                        <div class="col-lg-4 col-md-6 emp-item">
                            <a href="${pageContext.request.contextPath}/attendance?action=employeeDetail&employeeId=${c.employeeId}&fromDate=${monthStart}&toDate=${monthEnd}"
                               class="card emp-card h-100">
                                <div class="card-body d-flex align-items-start gap-3">
                                    <div class="avatar-md">${fn:substring(c.fullName, 0, 1)}</div>
                                    <div class="flex-grow-1">
                                        <div class="fw-bold" style="font-size:0.92rem;">${c.fullName}</div>
                                        <div class="text-muted" style="font-size:0.78rem;">${c.employeeCode} &middot; ${c.departmentName}</div>
                                        <div class="d-flex flex-wrap gap-1 mt-2">
                                            <span class="stat-badge stat-present">
                                                <i class="bi bi-check-circle-fill me-1"></i>${c.presentDays} days
                                            </span>
                                            <c:if test="${c.absentDays > 0}">
                                                <span class="stat-badge stat-absent">
                                                    <i class="bi bi-x-circle-fill me-1"></i>${c.absentDays} absent
                                                </span>
                                            </c:if>
                                            <c:if test="${c.leaveDays > 0}">
                                                <span class="stat-badge stat-leave">
                                                    <i class="bi bi-calendar-check me-1"></i>${c.leaveDays} leave
                                                </span>
                                            </c:if>
                                        </div>
                                        <div class="d-flex flex-wrap gap-1 mt-1">
                                            <c:if test="${c.pendingCount > 0}">
                                                <span class="stat-badge stat-pending">
                                                    <i class="bi bi-clock me-1"></i>${c.pendingCount} pending
                                                </span>
                                            </c:if>
                                            <c:if test="${c.verifiedCount > 0}">
                                                <span class="stat-badge stat-verified">
                                                    <i class="bi bi-patch-check me-1"></i>${c.verifiedCount} verified
                                                </span>
                                            </c:if>
                                        </div>
                                    </div>
                                    <i class="bi bi-chevron-right text-muted mt-1"></i>
                                </div>
                            </a>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </div>
    </c:if>

    <%-- Empty state when HR selected dept but no records --%>
    <c:if test="${hrScope and not empty selectedDeptId and empty employeeCards}">
        <div class="card border-0 shadow-sm rounded-3">
            <div class="card-body text-center text-muted py-5">
                <i class="bi bi-calendar2-x fs-2 d-block mb-2 opacity-25"></i>
                No attendance records found for this department in ${importMonthLabel}.
            </div>
        </div>
    </c:if>

    <%-- Empty state for Manager with no cards --%>
    <c:if test="${managerScope and empty employeeCards}">
        <div class="card border-0 shadow-sm rounded-3">
            <div class="card-body text-center text-muted py-5">
                <i class="bi bi-calendar2-x fs-2 d-block mb-2 opacity-25"></i>
                No attendance records for your team in ${importMonthLabel}.
                <div class="small mt-1">Attendance data will appear here after HR Staff imports the attendance sheet.</div>
            </div>
        </div>
    </c:if>
</div>

<%-- ===== Import attendance sheet modal (HR Staff only) ===== --%>
<c:if test="${canImportAttendance}">
<div class="modal fade" id="importModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content border-0 shadow">
      <form method="post" enctype="multipart/form-data"
            action="${pageContext.request.contextPath}/attendance?action=import">
        <input type="hidden" name="month" value="${selectedMonth}">
        <input type="hidden" name="year" value="${selectedYear}">
        <div class="modal-header" style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);">
          <h6 class="modal-title text-white mb-0">
            <i class="bi bi-upload me-2"></i>Import Attendance Sheet
          </h6>
          <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body">
          <p class="small text-muted mb-3">
            Import the attendance file for <strong>attendance departments</strong>,
            for <strong>${importMonthLabel}</strong>.
            Department managers must confirm their team's records before HR Staff can submit reports to HR Manager.
          </p>

          <div class="alert alert-light border d-flex align-items-center gap-2 py-2 small mb-3">
            <i class="bi bi-calendar-event text-primary"></i>
            <span>Records are imported into the selected month
              (<strong>${importMonthLabel}</strong>). All records start as <strong>Pending Manager Confirmation</strong>.</span>
          </div>

          <div class="mb-2">
            <label class="form-label small text-muted mb-1">Attendance file (.xlsx)</label>
            <input type="file" name="sheet" accept=".xlsx" required
                   class="form-control form-control-sm">
          </div>

          <div class="border rounded p-2 bg-light small text-muted">
            <strong>Accepted workbook:</strong>
            the file must include a sheet named <strong>Attendance Detail</strong>.
            Import reads only these columns:
            <strong>No. / Attendance Code / Employee Code / Timestamp</strong>.
            <br>The sheet title and every timestamp must match the selected month
            (<strong>${importMonthLabel}</strong>).
            <br>Missing punch days are resolved by approved leave requests, official holidays,
            Sundays, or absences. Existing records will not be overwritten.
            <br>Maximum file size: <strong>10 MB</strong>.
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-sm btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
          <button type="submit" class="btn btn-sm btn-primary"
                  style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
            <i class="bi bi-upload me-1"></i>Import
          </button>
        </div>
      </form>
    </div>
  </div>
</div>
</c:if>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            const q = this.value.toLowerCase();
            document.querySelectorAll('.emp-item').forEach(item => {
                item.style.display = item.textContent.toLowerCase().includes(q) ? '' : 'none';
            });
        });
    }
</script>
</body>
</html>

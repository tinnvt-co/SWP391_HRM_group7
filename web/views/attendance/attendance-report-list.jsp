<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
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
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
            <h5 class="fw-bold text-dark mb-0">Attendance Reports</h5>
            <small class="text-muted">Monthly attendance reports submitted by managers</small>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-3 mb-3">
        <div class="card-body">
            <form method="get" class="row g-2 align-items-end">
                <div class="col-md-3">
                    <label class="form-label small text-muted mb-1">Month</label>
                    <select name="month" class="form-select form-select-sm">
                        <c:forEach var="m" begin="1" end="12">
                            <option value="${m}" ${m == selectedMonth ? 'selected' : ''}>Month ${m}</option>
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

    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="p-3 border-bottom d-flex align-items-center gap-2">
                <i class="bi bi-calendar-check text-muted"></i>
                <span class="fw-medium">${monthLabel}</span>
                <span class="text-muted">&middot; ${fn:length(reports)} report(s)</span>
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
                            <th>Status</th>
                            <th>Submitted</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="r" items="${reports}" varStatus="s">
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
                                <td>${r.departmentName}</td>
                                <td class="text-muted">${r.managerFullName}</td>
                                <td class="text-center fw-medium">${r.actualWorkingDays}</td>
                                <td class="text-center">${r.paidLeaveDays}</td>
                                <td class="text-center">${r.unpaidLeaveDays}</td>
                                <td class="text-center">${r.overtimeHours}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${r.status == 'SubmittedToHrStaff'}">
                                            <span class="status-pill st-submitted">Submitted</span>
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
                                        <c:otherwise>${r.status}</c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-muted" style="font-size:0.82rem;">${r.submittedAt}</td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty reports}">
                            <tr>
                                <td colspan="10" class="text-center text-muted py-5">
                                    <i class="bi bi-inbox fs-2 d-block mb-2 opacity-25"></i>
                                    No attendance reports submitted for ${monthLabel}.
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
</body>
</html>

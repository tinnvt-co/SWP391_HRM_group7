<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="activePage" value="payrollApproval" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Payroll Approval &mdash; HRM System</title>
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
        .table th { font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.5px; color: #6b7280; font-weight: 600; }
        .table td { vertical-align: middle; font-size: 0.88rem; }
        .st-pill { padding: 4px 12px; border-radius: 20px; font-size: 0.75rem; font-weight: 600; }
        .st-draft   { background:#e5e7eb; color:#374151; }
        .st-pending { background:#fff8e1; color:#a16207; }
        .st-approved{ background:#e6f9f0; color:#166534; }
        .st-rejected{ background:#fee2e2; color:#b91c1c; }
        .st-paid    { background:#e3f0fb; color:#1a3c5e; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
            <h5 class="fw-bold text-dark mb-0">Payroll Approval</h5>
            <small class="text-muted">Review and approve monthly payroll</small>
        </div>
    </div>

    <c:if test="${not empty payrollMessage}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>${payrollMessage}</span>
        </div>
    </c:if>
    <c:if test="${not empty payrollError}">
        <div class="alert alert-danger d-flex align-items-start gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-octagon-fill mt-1"></i><span>${payrollError}</span>
        </div>
    </c:if>

    <div class="card border-0 shadow-sm rounded-3 mb-3">
        <div class="card-body">
            <form method="get" class="row g-2 align-items-end">
                <input type="hidden" name="action" value="approval">
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
                <div class="col-md-2">
                    <button type="submit" class="btn btn-sm btn-primary w-100"
                            style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                        <i class="bi bi-search me-1"></i>View
                    </button>
                </div>
            </form>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-3 mb-3">
        <div class="card-body d-flex align-items-center justify-content-between flex-wrap gap-2">
            <div>
                <span class="fw-medium">${monthLabel}</span>
                <c:choose>
                    <c:when test="${empty period}">
                        <span class="text-muted ms-2">No payroll for this month.</span>
                    </c:when>
                    <c:otherwise>
                        <span class="st-pill ms-2
                            ${period.status == 'Draft' ? 'st-draft' :
                              period.status == 'PendingApproval' ? 'st-pending' :
                              period.status == 'Approved' ? 'st-approved' :
                              period.status == 'Rejected' ? 'st-rejected' : 'st-paid'}">
                            ${period.status.dbValue}
                        </span>
                        <span class="text-muted ms-2">${period.payrollCount} employee(s)</span>
                    </c:otherwise>
                </c:choose>
            </div>
            <c:if test="${not empty period and period.status == 'PendingApproval'}">
                <div class="d-flex gap-2">
                    <form method="post" action="${pageContext.request.contextPath}/payroll?action=approve"
                          onsubmit="return confirm('Approve this payroll? HR Staff will then process payment.');">
                        <input type="hidden" name="periodId" value="${period.payrollPeriodId}">
                        <button type="submit" class="btn btn-sm btn-success">
                            <i class="bi bi-check2-circle me-1"></i>Approve
                        </button>
                    </form>
                    <button type="button" class="btn btn-sm btn-outline-danger"
                            data-bs-toggle="modal" data-bs-target="#rejectModal">
                        <i class="bi bi-x-circle me-1"></i>Reject
                    </button>
                </div>
            </c:if>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">#</th>
                            <th>Employee</th>
                            <th>Department</th>
                            <th class="text-end">Basic</th>
                            <th class="text-center">Work Days</th>
                            <th class="text-end">Allowance</th>
                            <th class="text-end">KPI</th>
                            <th class="text-end">OT Salary</th>
                            <th class="text-end">Gross</th>
                            <th class="text-end">Deduction</th>
                            <th class="text-end">Net</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="p" items="${payrolls}" varStatus="s">
                            <tr>
                                <td class="ps-4 text-muted">${s.index + 1}</td>
                                <td>
                                    <div class="fw-medium">${p.employeeFullName}</div>
                                    <div class="text-muted" style="font-size:0.78rem;">${p.employeeCode}</div>
                                </td>
                                <td>${p.departmentName}</td>
                                <td class="text-end"><fmt:formatNumber value="${p.basicSalary}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-center">${p.actualWorkingDays}</td>
                                <td class="text-end"><fmt:formatNumber value="${p.totalAllowance}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-end"><fmt:formatNumber value="${p.kpiBonus}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-end"><fmt:formatNumber value="${p.overtimeSalary}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-end"><fmt:formatNumber value="${p.grossSalary}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-end text-danger"><fmt:formatNumber value="${p.totalDeduction}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-end fw-bold"><fmt:formatNumber value="${p.netSalary}" type="number" maxFractionDigits="0"/></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty payrolls}">
                            <tr>
                                <td colspan="11" class="text-center text-muted py-5">
                                    <i class="bi bi-inbox fs-2 d-block mb-2 opacity-25"></i>
                                    No payroll submitted for ${monthLabel}.
                                </td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<%-- Reject modal --%>
<c:if test="${not empty period and period.status == 'PendingApproval'}">
<div class="modal fade" id="rejectModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content border-0 shadow">
      <form method="post" action="${pageContext.request.contextPath}/payroll?action=reject">
        <input type="hidden" name="periodId" value="${period.payrollPeriodId}">
        <div class="modal-header bg-danger">
          <h6 class="modal-title text-white mb-0"><i class="bi bi-x-octagon me-2"></i>Reject Payroll</h6>
          <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <label class="form-label small text-muted mb-1">Reason for rejection (required)</label>
          <textarea name="rejectReason" class="form-control" rows="3" maxlength="500" required
                    placeholder="Explain what HR Staff needs to fix..."></textarea>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-sm btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
          <button type="submit" class="btn btn-sm btn-danger">Reject &amp; Send Back</button>
        </div>
      </form>
    </div>
  </div>
</div>
</c:if>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

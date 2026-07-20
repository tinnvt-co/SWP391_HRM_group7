<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="activePage" value="payroll" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Payroll &mdash; HRM System</title>
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
        .salary-summary-card { border: none; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.07); transition: box-shadow 0.2s, transform 0.2s; }
        .summary-card-link { color: inherit; text-decoration: none; display: block; }
        .summary-card-link:hover .salary-summary-card { box-shadow: 0 4px 16px rgba(0,0,0,0.11); transform: translateY(-1px); }
        .salary-summary-icon {
            width: 48px; height: 48px; border-radius: 12px;
            display: flex; align-items: center; justify-content: center; font-size: 1.25rem;
        }
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
            <h5 class="fw-bold text-dark mb-0">Payroll</h5>
            <small class="text-muted">Calculate and process monthly salary</small>
        </div>
    </div>

    <c:if test="${payrollTaskSummary.actionable}">
        <c:url var="payrollTaskUrl" value="/payroll">
            <c:param name="month" value="${payrollTaskSummary.month}"/>
            <c:param name="year" value="${payrollTaskSummary.year}"/>
        </c:url>
    </c:if>
    <div class="row g-3 mb-3">
        <div class="col-lg-4 col-md-6">
            <div class="salary-summary-card card p-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="salary-summary-icon" style="background:#e3f0fb;">
                        <i class="bi bi-calendar2-month text-primary"></i>
                    </div>
                    <div>
                        <div class="text-muted small">Company-paid Monthly Salary</div>
                        <div class="fw-bold fs-5">
                            <fmt:formatNumber value="${monthlySalaryTotal}" type="number" maxFractionDigits="0"/>
                        </div>
                        <div class="text-muted" style="font-size:0.78rem;">
                            ${selectedDeptName} &middot; Month ${selectedMonth}/${selectedYear}
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-lg-4 col-md-6">
            <div class="salary-summary-card card p-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="salary-summary-icon" style="background:#e6f9f0;">
                        <i class="bi bi-cash-stack text-success"></i>
                    </div>
                    <div>
                        <div class="text-muted small">Company-paid Yearly Salary</div>
                        <div class="fw-bold fs-5">
                            <fmt:formatNumber value="${yearlySalaryTotal}" type="number" maxFractionDigits="0"/>
                        </div>
                        <div class="text-muted" style="font-size:0.78rem;">
                            ${selectedDeptName} &middot; ${selectedYear}
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-lg-4 col-md-6">
            <a class="summary-card-link ${payrollTaskSummary.actionable ? '' : 'pe-none'}"
               href="${payrollTaskSummary.actionable ? payrollTaskUrl : '#'}"
               aria-disabled="${payrollTaskSummary.actionable ? 'false' : 'true'}">
                <div class="salary-summary-card card p-3">
                    <div class="d-flex align-items-center gap-3">
                        <div class="salary-summary-icon" style="background:#fff8e1;">
                            <i class="bi bi-list-task text-warning"></i>
                        </div>
                        <div>
                            <div class="text-muted small">Tasks to Process</div>
                            <div class="fw-bold fs-5">${payrollTaskSummary.count}</div>
                            <div class="text-muted" style="font-size:0.78rem;">
                                <c:choose>
                                    <c:when test="${payrollTaskSummary.actionable}">
                                        ${payrollTaskSummary.taskLabel}
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

    <c:if test="${not empty payrollMessage}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>${payrollMessage}</span>
        </div>
    </c:if>
    <c:if test="${not empty payrollError}">
        <div class="alert alert-danger d-flex align-items-start gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-octagon-fill mt-1"></i><span><c:out value="${payrollError}"/></span>
        </div>
    </c:if>

    <c:if test="${rejectedPeriod}">
        <div class="alert alert-danger border-danger-subtle mb-3" role="alert">
            <div class="d-flex align-items-start gap-2">
                <i class="bi bi-arrow-counterclockwise fs-5"></i>
                <div>
                    <div class="fw-semibold">Payroll Returned for Revision</div>
                    <div class="small mt-1">
                        <span class="fw-medium">Reason:</span>
                        <c:choose>
                            <c:when test="${not empty period.rejectReason}">
                                <c:out value="${period.rejectReason}"/>
                            </c:when>
                            <c:otherwise>No rejection reason was provided.</c:otherwise>
                        </c:choose>
                    </div>
                    <div class="small text-muted mt-1">
                        <c:if test="${not empty period.approvedByName}">
                            Rejected by <c:out value="${period.approvedByName}"/>.
                        </c:if>
                        Review the payroll, make the required corrections, then submit it again.
                    </div>
                </div>
            </div>
        </div>
    </c:if>

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
                <div class="col-md-2">
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

    <%-- Status banner + workflow actions --%>
    <div class="card border-0 shadow-sm rounded-3 mb-3">
        <div class="card-body d-flex align-items-center justify-content-between flex-wrap gap-2">
            <div>
                <span class="fw-medium">${monthLabel}</span>
                <c:if test="${not empty selectedDeptName}">
                    <span class="text-muted ms-2">${selectedDeptName}</span>
                </c:if>
                <c:choose>
                    <c:when test="${allDepartmentsScope}">
                        <span class="text-muted ms-2">Showing generated payroll across departments.</span>
                        <span class="text-muted ms-2">${totalPayrolls} employee(s)</span>
                    </c:when>
                    <c:when test="${empty period}">
                        <span class="text-muted ms-2">No payroll generated yet.</span>
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
            <div class="d-flex gap-2">
                <%-- Generate or append missing payroll lines from approved attendance reports. --%>
                <c:if test="${hasReports}">
                    <form method="post" action="${pageContext.request.contextPath}/payroll?action=generate">
                        <input type="hidden" name="year" value="${selectedYear}">
                        <input type="hidden" name="month" value="${selectedMonth}">
                        <button type="submit" class="btn btn-sm btn-primary"
                                style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                            <i class="bi bi-calculator me-1"></i>
                            <c:choose>
                                <c:when test="${empty period}">Calculate Payroll</c:when>
                                <c:otherwise>Add Missing Payroll</c:otherwise>
                            </c:choose>
                        </button>
                    </form>
                </c:if>
                <c:if test="${submittablePayrollBatchCount > 0 and empty period}">
                    <form method="post" action="${pageContext.request.contextPath}/payroll?action=submit"
                          onsubmit="return confirm('Submit all ready payroll for this month to HR Manager?');">
                        <input type="hidden" name="year" value="${selectedYear}">
                        <input type="hidden" name="month" value="${selectedMonth}">
                        <button type="submit" class="btn btn-sm btn-primary"
                                style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                            <i class="bi bi-send me-1"></i>Submit for Approval
                            <span class="badge bg-light text-primary ms-1">${submittablePayrollBatchCount}</span>
                        </button>
                    </form>
                </c:if>
                <c:if test="${payablePayrollBatchCount > 0 and empty period}">
                    <form method="post" action="${pageContext.request.contextPath}/payroll?action=confirmPayment"
                          onsubmit="return confirm('Confirm payment for all approved payroll in this month?');">
                        <input type="hidden" name="year" value="${selectedYear}">
                        <input type="hidden" name="month" value="${selectedMonth}">
                        <button type="submit" class="btn btn-sm btn-success">
                            <i class="bi bi-cash-coin me-1"></i>Confirm Payment
                            <span class="badge bg-light text-success ms-1">${payablePayrollBatchCount}</span>
                        </button>
                    </form>
                </c:if>
                <%-- Submit for approval: Draft or Rejected --%>
                <c:if test="${not empty period and (period.status == 'Draft' or period.status == 'Rejected')}">
                    <form method="post" action="${pageContext.request.contextPath}/payroll?action=submit"
                          onsubmit="return confirm('Submit this payroll to HR Manager for approval?');">
                        <input type="hidden" name="periodId" value="${period.payrollPeriodId}">
                        <button type="submit" class="btn btn-sm btn-primary"
                                style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                            <i class="bi bi-send me-1"></i>
                            ${period.status == 'Rejected' ? 'Re-submit for Approval' : 'Submit for Approval'}
                        </button>
                    </form>
                </c:if>
                <%-- Confirm payment: Approved --%>
                <c:if test="${not empty period and period.status == 'Approved'}">
                    <form method="post" action="${pageContext.request.contextPath}/payroll?action=confirmPayment"
                          onsubmit="return confirm('Confirm salary payment? Payslips will be released to employees.');">
                        <input type="hidden" name="periodId" value="${period.payrollPeriodId}">
                        <button type="submit" class="btn btn-sm btn-success">
                            <i class="bi bi-cash-coin me-1"></i>Confirm Payment
                        </button>
                    </form>
                </c:if>
            </div>
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
                            <th class="text-end">Attendance Bonus</th>
                            <th class="text-end">KPI</th>
                            <th class="text-end">OT Salary</th>
                            <th class="text-end">Gross</th>
                            <th class="text-end">Deduction</th>
                            <th class="text-end">Net</th>
                            <th class="text-center">Details</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="p" items="${payrolls}" varStatus="s">
                            <tr>
                                <td class="ps-4 text-muted">${(currentPage - 1) * 10 + s.index + 1}</td>
                                <td>
                                    <div class="fw-medium">${p.employeeFullName}</div>
                                    <div class="text-muted" style="font-size:0.78rem;">${p.employeeCode}</div>
                                </td>
                                <td>${p.departmentName}</td>
                                <td class="text-end"><fmt:formatNumber value="${p.basicSalary}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-center">${p.actualWorkingDays}</td>
                                <td class="text-end"><fmt:formatNumber value="${p.totalAllowance}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-end"><fmt:formatNumber value="${p.attendanceBonusAmount}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-end"><fmt:formatNumber value="${p.kpiBonus}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-end"><fmt:formatNumber value="${p.overtimeSalary}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-end"><fmt:formatNumber value="${p.grossSalary}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-end text-danger"><fmt:formatNumber value="${p.totalDeduction}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-end fw-bold"><fmt:formatNumber value="${p.totalReceived}" type="number" maxFractionDigits="0"/></td>
                                <td class="text-center">
                                    <button type="button" class="btn btn-sm btn-outline-primary"
                                            data-bs-toggle="modal" data-bs-target="#payrollDetail${p.payrollId}">
                                        <i class="bi bi-receipt"></i>
                                    </button>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty payrolls}">
                            <tr>
                                <td colspan="13" class="text-center text-muted py-5">
                                    <i class="bi bi-cash-stack fs-2 d-block mb-2 opacity-25"></i>
                                    No payroll for ${monthLabel}.
                                    <c:if test="${not empty selectedDeptName}">
                                        <span>${selectedDeptName}.</span>
                                    </c:if>
                                    <c:if test="${not hasReports}">
                                        <div class="small mt-1">No HR Manager-approved attendance reports are waiting for payroll generation.</div>
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
                    Page ${currentPage} of ${totalPages} &middot; ${totalPayrolls} payroll record(s)
                </small>
                <nav>
                    <ul class="pagination pagination-sm mb-0">
                        <li class="page-item ${currentPage == 1 ? 'disabled' : ''}">
                            <a class="page-link"
                               href="?month=${selectedMonth}&year=${selectedYear}&page=${currentPage - 1}">Previous</a>
                        </li>
                        <c:forEach var="pg" begin="1" end="${totalPages}">
                            <li class="page-item ${pg == currentPage ? 'active' : ''}">
                                <a class="page-link"
                                   href="?month=${selectedMonth}&year=${selectedYear}&page=${pg}">${pg}</a>
                            </li>
                        </c:forEach>
                        <li class="page-item ${currentPage == totalPages ? 'disabled' : ''}">
                            <a class="page-link"
                               href="?month=${selectedMonth}&year=${selectedYear}&page=${currentPage + 1}">Next</a>
                        </li>
                    </ul>
                </nav>
            </div>
        </c:if>
    </div>
</div>

<c:forEach var="p" items="${payrolls}">
<div class="modal fade" id="payrollDetail${p.payrollId}" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
    <div class="modal-content border-0 shadow">
      <div class="modal-header" style="background:#1a3c5e;">
        <h6 class="modal-title text-white mb-0"><i class="bi bi-receipt me-2"></i>Payroll Detail</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 border rounded p-3 mb-3">
            <div>
                <div class="text-muted small text-uppercase fw-semibold">Employee</div>
                <div class="fw-bold">${p.employeeFullName}</div>
                <div class="text-muted small">${p.employeeCode} &middot; ${p.departmentName}</div>
            </div>
            <div class="text-end">
                <div class="text-muted small text-uppercase fw-semibold">Period</div>
                <div class="fw-bold">${monthLabel}</div>
                <span class="badge bg-secondary">${p.status.dbValue}</span>
            </div>
        </div>
        <div class="row g-3">
            <div class="col-md-6">
                <h6 class="text-success fw-bold border-bottom pb-2">Earnings</h6>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Basic salary</span><span><fmt:formatNumber value="${p.basicSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Paid working days</span><span>${p.actualWorkingDays}</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Work salary</span><span><fmt:formatNumber value="${p.workSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Normal OT (${p.normalOvertimeHours}h x 150%)</span><span>+ <fmt:formatNumber value="${p.normalOvertimeSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Weekend OT (${p.weekendOvertimeHours}h x 200%)</span><span>+ <fmt:formatNumber value="${p.weekendOvertimeSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Holiday OT (${p.holidayOvertimeHours}h x 300%)</span><span>+ <fmt:formatNumber value="${p.holidayOvertimeSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Allowances</span><span>+ <fmt:formatNumber value="${p.totalAllowance}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Attendance bonus</span><span>+ <fmt:formatNumber value="${p.attendanceBonusAmount}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">KPI bonus</span><span>+ <fmt:formatNumber value="${p.kpiBonus}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between fw-bold border-top mt-2 pt-2"><span>Gross salary</span><span><fmt:formatNumber value="${p.grossSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
            </div>
            <div class="col-md-6">
                <h6 class="text-danger fw-bold border-bottom pb-2">Deductions</h6>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Insurance base</span><span><fmt:formatNumber value="${p.insuranceBase}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Social insurance (8.00%)</span><span class="text-danger">- <fmt:formatNumber value="${p.socialInsurance}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Health insurance (1.50%)</span><span class="text-danger">- <fmt:formatNumber value="${p.healthInsurance}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Unemployment insurance (1.00%)</span><span class="text-danger">- <fmt:formatNumber value="${p.unemploymentInsurance}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Personal income tax</span><span class="text-danger">- <fmt:formatNumber value="${p.personalIncomeTax}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Advance payment / other deduction</span><span class="text-danger">- <fmt:formatNumber value="${p.advancePayment}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between py-1"><span class="text-muted">Late penalty</span><span class="text-danger">- <fmt:formatNumber value="${p.latePenaltyAmount}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="d-flex justify-content-between fw-bold border-top mt-2 pt-2"><span>Total deduction</span><span class="text-danger">- <fmt:formatNumber value="${p.totalDeduction}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                <div class="border-top mt-3 pt-2">
                    <div class="d-flex justify-content-between py-1"><span class="text-muted">Maternity leave days</span><span>${p.maternityLeaveDays}</span></div>
                    <div class="d-flex justify-content-between py-1"><span class="text-muted">Maternity benefit</span><span><fmt:formatNumber value="${p.socialInsuranceBenefit}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                    <div class="text-muted small">Paid by social insurance; included in Net Salary shown below, but excluded from company-paid salary totals.</div>
                </div>
            </div>
        </div>
        <div class="text-center border rounded mt-3 p-3" style="background:#eef7ff;">
            <div class="text-muted small text-uppercase fw-semibold">Net Salary</div>
            <div class="fs-3 fw-bold text-success"><fmt:formatNumber value="${p.totalReceived}" type="number" maxFractionDigits="0"/> &#8363;</div>
            <c:if test="${p.socialInsuranceBenefit > 0}">
                <div class="small text-muted">
                    <fmt:formatNumber value="${p.netSalary}" type="number" maxFractionDigits="0"/> &#8363; company-paid
                    + <fmt:formatNumber value="${p.socialInsuranceBenefit}" type="number" maxFractionDigits="0"/> &#8363; social insurance benefit
                </div>
            </c:if>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal">Close</button>
      </div>
    </div>
  </div>
</div>
</c:forEach>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

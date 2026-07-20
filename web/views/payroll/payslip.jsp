<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="activePage" value="payslip" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Payslip &mdash; HRM System</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body { background-color: #f4f6f9; }
        .sidebar { width: 240px; min-height: calc(100vh - 56px); background-color: #1a3c5e;
            position: fixed; top: 56px; left: 0; padding-top: 1rem; z-index: 100; }
        .sidebar .nav-link { color: rgba(255,255,255,0.75); padding: 0.6rem 1.25rem;
            border-radius: 6px; margin: 2px 10px; font-size: 0.9rem; }
        .sidebar .nav-link:hover, .sidebar .nav-link.active { color: #fff; background-color: rgba(255,255,255,0.12); }
        .sidebar .nav-link i { width: 20px; }
        .sidebar-label { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 1px;
            color: rgba(255,255,255,0.4); padding: 0.75rem 1.25rem 0.25rem; }
        .main-content { margin-left: 240px; padding: 2rem; }
        .payslip-wrap { max-width: 980px; }
        .summary-box { border: 1px solid #dfe5ee; border-radius: 8px; background:#fff; }
        .section-panel { border: 1px solid #e3e8f0; border-radius: 8px; height: 100%; }
        .section-panel.earnings { background:#fbfffd; }
        .section-panel.deductions { background:#fffafa; }
        .section-title { font-size:0.86rem; font-weight:700; padding-bottom:0.7rem; border-bottom:1px solid #e5e7eb; }
        .section-title.earnings { color:#166534; }
        .section-title.deductions { color:#b42318; }
        .row-line { display:flex; align-items:flex-start; justify-content:space-between; gap:1rem;
            padding:0.48rem 0; font-size:0.92rem; }
        .row-line .lbl { color:#6b7280; }
        .row-line.sub { padding:0.22rem 0 0.22rem 1.25rem; font-size:0.84rem; }
        .net-box { border:1px solid #b9d6f2; background:#eef7ff; border-radius:8px; padding:1rem 1.25rem; }
        .net-value { color:#157347; font-size:2rem; font-weight:800; letter-spacing:0; }
        @media (max-width: 768px) {
            .sidebar { position: static; width: 100%; min-height: auto; }
            .main-content { margin-left: 0; padding: 1rem; }
            .net-value { font-size:1.55rem; }
        }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
            <h5 class="fw-bold text-dark mb-0">My Payslip</h5>
            <small class="text-muted">Your monthly salary statement</small>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-3 mb-3 payslip-wrap">
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
                <div class="col-md-2">
                    <button type="submit" class="btn btn-sm btn-primary w-100"
                            style="background:#1a3c5e;border:none;">
                        <i class="bi bi-search me-1"></i>View
                    </button>
                </div>
            </form>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty payslip}">
            <div class="card border-0 shadow-sm rounded-3 payslip-wrap">
                <div class="card-body text-center text-muted py-5">
                    <i class="bi bi-receipt fs-2 d-block mb-2 opacity-25"></i>
                    No payslip available for ${monthLabel}.
                    <div class="small mt-1">Your payslip appears here once payroll payment for the month is confirmed.</div>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card border-0 shadow-sm rounded-3 payslip-wrap">
                <div class="card-body p-4">
                    <div class="summary-box p-3 mb-4 d-flex justify-content-between align-items-center flex-wrap gap-3">
                        <div>
                            <div class="text-muted small text-uppercase fw-semibold">Employee</div>
                            <div class="fs-5 fw-bold">${employee.fullName}</div>
                            <div class="text-muted small">${payslip.employeeCode} &middot; ${payslip.departmentName}</div>
                        </div>
                        <div class="text-md-end">
                            <div class="text-muted small text-uppercase fw-semibold">Employee ID / Period</div>
                            <div class="fw-bold">#${payslip.employeeId} | ${monthLabel}</div>
                            <span class="badge bg-secondary mt-1">${payslip.status.dbValue}</span>
                        </div>
                    </div>

                    <div class="row g-4">
                        <div class="col-lg-6">
                            <div class="section-panel earnings p-3">
                                <div class="section-title earnings">
                                    <i class="bi bi-plus-circle-fill me-1"></i>Earnings
                                </div>
                                <div class="row-line"><span class="lbl">Basic salary</span>
                                    <span><fmt:formatNumber value="${payslip.basicSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line"><span class="lbl">Paid working days</span>
                                    <span>${payslip.actualWorkingDays}</span></div>
                                <div class="row-line"><span class="lbl">Work salary</span>
                                    <span><fmt:formatNumber value="${payslip.workSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line"><span class="lbl">Overtime salary</span>
                                    <span>+ <fmt:formatNumber value="${payslip.overtimeSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line sub"><span class="lbl">Normal OT (${payslip.normalOvertimeHours}h x 150%)</span>
                                    <span>+ <fmt:formatNumber value="${payslip.normalOvertimeSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line sub"><span class="lbl">Weekend OT (${payslip.weekendOvertimeHours}h x 200%)</span>
                                    <span>+ <fmt:formatNumber value="${payslip.weekendOvertimeSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line sub"><span class="lbl">Holiday OT (${payslip.holidayOvertimeHours}h x 300%)</span>
                                    <span>+ <fmt:formatNumber value="${payslip.holidayOvertimeSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line"><span class="lbl">Allowances</span>
                                    <span>+ <fmt:formatNumber value="${payslip.totalAllowance}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line"><span class="lbl">Attendance bonus</span>
                                    <span>+ <fmt:formatNumber value="${payslip.attendanceBonusAmount}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line"><span class="lbl">KPI bonus</span>
                                    <span>+ <fmt:formatNumber value="${payslip.kpiBonus}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line fw-bold border-top mt-2 pt-3"><span>Gross salary</span>
                                    <span><fmt:formatNumber value="${payslip.grossSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                            </div>
                        </div>

                        <div class="col-lg-6">
                            <div class="section-panel deductions p-3">
                                <div class="section-title deductions">
                                    <i class="bi bi-dash-circle-fill me-1"></i>Deductions
                                </div>
                                <div class="row-line"><span class="lbl">Insurance base</span>
                                    <span><fmt:formatNumber value="${payslip.insuranceBase}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line"><span class="lbl">Social insurance (8.00%)</span>
                                    <span class="text-danger">- <fmt:formatNumber value="${payslip.socialInsurance}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line"><span class="lbl">Health insurance (1.50%)</span>
                                    <span class="text-danger">- <fmt:formatNumber value="${payslip.healthInsurance}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line"><span class="lbl">Unemployment insurance (1.00%)</span>
                                    <span class="text-danger">- <fmt:formatNumber value="${payslip.unemploymentInsurance}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line"><span class="lbl">Personal income tax</span>
                                    <span class="text-danger">- <fmt:formatNumber value="${payslip.personalIncomeTax}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line"><span class="lbl">Advance payment / other deduction</span>
                                    <span class="text-danger">- <fmt:formatNumber value="${payslip.advancePayment}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line"><span class="lbl">Late penalty</span>
                                    <span class="text-danger">- <fmt:formatNumber value="${payslip.latePenaltyAmount}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                <div class="row-line fw-bold border-top mt-2 pt-3"><span>Total deduction</span>
                                    <span class="text-danger">- <fmt:formatNumber value="${payslip.totalDeduction}" type="number" maxFractionDigits="0"/> &#8363;</span></div>

                                <div class="mt-4 pt-2 border-top">
                                    <div class="text-muted small text-uppercase fw-semibold mb-1">Social insurance benefit</div>
                                    <div class="row-line py-1"><span class="lbl">Maternity leave days</span>
                                        <span>${payslip.maternityLeaveDays}</span></div>
                                    <div class="row-line py-1"><span class="lbl">Maternity benefit</span>
                                        <span><fmt:formatNumber value="${payslip.socialInsuranceBenefit}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                                    <div class="text-muted small">This benefit is paid by social insurance and is not included in company net salary.</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="net-box mt-4 text-center">
                        <div class="text-muted small text-uppercase fw-semibold">Net Salary</div>
                        <div class="net-value"><fmt:formatNumber value="${payslip.netSalary}" type="number" maxFractionDigits="0"/> &#8363;</div>
                        <div class="small text-muted">Company-paid salary for ${monthLabel}</div>
                    </div>

                    <c:if test="${not empty payslip.note}">
                        <div class="text-muted small mt-3"><i class="bi bi-info-circle me-1"></i>${payslip.note}</div>
                    </c:if>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

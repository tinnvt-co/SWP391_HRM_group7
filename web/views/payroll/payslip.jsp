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
        .slip { max-width: 640px; }
        .slip .row-line { display:flex; justify-content:space-between; padding:0.55rem 0;
            border-bottom:1px solid #f0f1f3; font-size:0.92rem; }
        .slip .row-line .lbl { color:#6b7280; }
        .slip .grp-title { font-size:0.72rem; text-transform:uppercase; letter-spacing:0.5px;
            color:#9ca3af; font-weight:600; margin:1rem 0 0.25rem; }
        .net-box { background:linear-gradient(135deg,#1a3c5e,#2d6a9f); color:#fff;
            border-radius:10px; padding:1rem 1.25rem; }
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
                <div class="col-md-2">
                    <button type="submit" class="btn btn-sm btn-primary w-100"
                            style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                        <i class="bi bi-search me-1"></i>View
                    </button>
                </div>
            </form>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty payslip}">
            <div class="card border-0 shadow-sm rounded-3">
                <div class="card-body text-center text-muted py-5">
                    <i class="bi bi-receipt fs-2 d-block mb-2 opacity-25"></i>
                    No payslip available for ${monthLabel}.
                    <div class="small mt-1">Your payslip appears here once payroll for the month has been paid.</div>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card border-0 shadow-sm rounded-3 slip">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <div>
                            <div class="fw-bold">${employee.fullName}</div>
                            <div class="text-muted small">${payslip.employeeCode} &middot; ${payslip.departmentName}</div>
                        </div>
                        <div class="text-end">
                            <div class="fw-medium">${monthLabel}</div>
                            <span class="badge bg-success">Paid</span>
                        </div>
                    </div>

                    <div class="grp-title">Earnings</div>
                    <div class="row-line"><span class="lbl">Basic salary</span>
                        <span><fmt:formatNumber value="${payslip.basicSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                    <div class="row-line"><span class="lbl">Actual working days</span>
                        <span>${payslip.actualWorkingDays}</span></div>
                    <div class="row-line"><span class="lbl">Allowance</span>
                        <span><fmt:formatNumber value="${payslip.totalAllowance}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                    <div class="row-line"><span class="lbl">KPI bonus</span>
                        <span><fmt:formatNumber value="${payslip.kpiBonus}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                    <div class="row-line"><span class="lbl">Overtime salary</span>
                        <span><fmt:formatNumber value="${payslip.overtimeSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>
                    <div class="row-line fw-medium"><span>Gross salary</span>
                        <span><fmt:formatNumber value="${payslip.grossSalary}" type="number" maxFractionDigits="0"/> &#8363;</span></div>

                    <div class="grp-title">Deductions</div>
                    <div class="row-line"><span class="lbl">Total deduction (insurance + advance)</span>
                        <span class="text-danger">&minus; <fmt:formatNumber value="${payslip.totalDeduction}" type="number" maxFractionDigits="0"/> &#8363;</span></div>

                    <div class="net-box d-flex justify-content-between align-items-center mt-3">
                        <span class="fw-medium">Net salary</span>
                        <span class="fs-5 fw-bold"><fmt:formatNumber value="${payslip.netSalary}" type="number" maxFractionDigits="0"/> &#8363;</span>
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

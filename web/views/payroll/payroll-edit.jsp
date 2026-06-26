<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="activePage" value="payroll" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit Payroll &mdash; HRM System</title>
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
        .ro { background:#f1f3f5; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="mb-4">
        <h5 class="fw-bold text-dark mb-0">Edit Payroll</h5>
        <small class="text-muted">${payroll.employeeFullName} (${payroll.employeeCode}) &middot; ${payroll.departmentName}</small>
    </div>

    <c:if test="${not empty payrollError}">
        <div class="alert alert-danger d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-octagon-fill"></i><span>${payrollError}</span>
        </div>
    </c:if>

    <div class="card border-0 shadow-sm rounded-3" style="max-width:720px;">
        <div class="card-body">
            <form method="post" action="${pageContext.request.contextPath}/payroll?action=editPayroll">
                <input type="hidden" name="id" value="${payroll.payrollId}">

                <div class="row g-3 mb-3">
                    <div class="col-md-6">
                        <label class="form-label small text-muted mb-1">Basic Salary</label>
                        <input type="text" class="form-control ro" disabled
                               value="<fmt:formatNumber value='${payroll.basicSalary}' type='number' maxFractionDigits='0'/>">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Work Days</label>
                        <input type="text" class="form-control ro" disabled value="${payroll.actualWorkingDays}">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Allowance</label>
                        <input type="text" class="form-control ro" disabled
                               value="<fmt:formatNumber value='${payroll.totalAllowance}' type='number' maxFractionDigits='0'/>">
                    </div>
                    <div class="col-md-6">
                        <label class="form-label small text-muted mb-1">OT Salary</label>
                        <input type="text" class="form-control ro" disabled
                               value="<fmt:formatNumber value='${payroll.overtimeSalary}' type='number' maxFractionDigits='0'/>">
                    </div>
                </div>

                <hr>
                <p class="small text-muted">Editable fields. Gross / deduction / net are recalculated automatically when you save.</p>

                <div class="row g-3 mb-3">
                    <div class="col-md-6">
                        <label for="kpiBonus" class="form-label">KPI Bonus</label>
                        <input type="number" step="1000" min="0" id="kpiBonus" name="kpiBonus"
                               class="form-control" value="${payroll.kpiBonus.toBigInteger()}">
                    </div>
                    <div class="col-md-6">
                        <label for="advancePayment" class="form-label">Advance Payment</label>
                        <input type="number" step="1000" min="0" id="advancePayment" name="advancePayment"
                               class="form-control" value="${currentAdvance}">
                    </div>
                </div>

                <div class="d-flex justify-content-end gap-2 mt-4">
                    <a href="${pageContext.request.contextPath}/payroll${period.status == 'PendingApproval' ? '?action=approval' : ''}${period.status == 'PendingApproval' ? '&' : '?'}year=${period.payrollYear}&month=${period.payrollMonth}"
                       class="btn btn-outline-secondary px-4">Cancel</a>
                    <button type="submit" class="btn btn-primary px-4 fw-medium"
                            style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                        <i class="bi bi-check-lg me-2"></i>Save
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

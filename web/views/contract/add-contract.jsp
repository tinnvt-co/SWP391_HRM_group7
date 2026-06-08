<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="contracts" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>New Contract &mdash; HRM System</title>
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
        .form-label { font-weight: 500; font-size: 0.9rem; }
        .required::after { content: ' *'; color: #dc3545; }
        .form-control:focus, .form-select:focus {
            border-color: #2d6a9f; box-shadow: 0 0 0 0.2rem rgba(45,106,159,0.2);
        }
        .section-title {
            font-size: 0.75rem; font-weight: 600; text-transform: uppercase;
            letter-spacing: 0.8px; color: #6b7280;
            padding-bottom: 0.5rem; border-bottom: 1px solid #e5e7eb; margin-bottom: 1.25rem;
        }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center gap-2 mb-4">
        <a href="${pageContext.request.contextPath}/contracts" class="btn btn-sm btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Back
        </a>
        <div>
            <h5 class="fw-bold text-dark mb-0">New Contract</h5>
            <small class="text-muted">Create a new employment contract</small>
        </div>
    </div>

    <div class="row justify-content-center">
        <div class="col-xl-9">
            <div class="card border-0 shadow-sm rounded-3 p-4">

                <c:if test="${not empty error}">
                    <div class="alert alert-danger d-flex align-items-center gap-2 py-2 mb-4">
                        <i class="bi bi-exclamation-circle-fill"></i><span>${error}</span>
                    </div>
                </c:if>

                <form action="${pageContext.request.contextPath}/contracts?action=add" method="post" novalidate>

                    <div class="section-title">Contract Details</div>
                    <div class="row g-3 mb-4">
                        <div class="col-md-6">
                            <label for="employeeId" class="form-label required">Employee</label>
                            <select id="employeeId" name="employeeId" class="form-select" required>
                                <option value="">-- Select employee --</option>
                                <c:forEach var="e" items="${employees}">
                                    <option value="${e.employeeId}" ${param.employeeId == e.employeeId ? 'selected' : ''}>
                                        ${e.fullName} &middot; ${e.employeeCode} &middot; ${e.departmentName}
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label for="contractCode" class="form-label required">Contract Code</label>
                            <input type="text" id="contractCode" name="contractCode" class="form-control"
                                   maxlength="50" placeholder="e.g. HD-MP-00007"
                                   value="${not empty param.contractCode ? param.contractCode : ''}" required>
                        </div>
                        <div class="col-md-4">
                            <label for="contractType" class="form-label required">Contract Type</label>
                            <select id="contractType" name="contractType" class="form-select" required>
                                <option value="">-- Select type --</option>
                                <c:forEach var="t" items="${contractTypes}">
                                    <option value="${t}" ${param.contractType == t.name() ? 'selected' : ''}>${t.dbValue}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label for="startDate" class="form-label required">Start Date</label>
                            <input type="date" id="startDate" name="startDate" class="form-control"
                                   value="${not empty param.startDate ? param.startDate : ''}" required>
                        </div>
                        <div class="col-md-4">
                            <label for="endDate" class="form-label">End Date</label>
                            <input type="date" id="endDate" name="endDate" class="form-control"
                                   value="${not empty param.endDate ? param.endDate : ''}">
                            <div class="form-text">Leave empty for indefinite contracts.</div>
                        </div>
                    </div>

                    <div class="section-title">Salary &amp; Allowances (VND)</div>
                    <div class="row g-3 mb-4">
                        <div class="col-md-4">
                            <label for="basicSalary" class="form-label required">Basic Salary</label>
                            <input type="number" step="1000" min="0" id="basicSalary" name="basicSalary" class="form-control"
                                   value="${not empty param.basicSalary ? param.basicSalary : '0'}" required>
                        </div>
                        <div class="col-md-4">
                            <label for="standardWorkingDays" class="form-label required">Standard Working Days</label>
                            <input type="number" step="0.5" min="1" max="31" id="standardWorkingDays" name="standardWorkingDays" class="form-control"
                                   value="${not empty param.standardWorkingDays ? param.standardWorkingDays : '26'}" required>
                        </div>
                        <div class="col-md-4">
                            <label for="lunchAllowance" class="form-label">Lunch Allowance</label>
                            <input type="number" step="1000" min="0" id="lunchAllowance" name="lunchAllowance" class="form-control"
                                   value="${not empty param.lunchAllowance ? param.lunchAllowance : '0'}">
                        </div>
                        <div class="col-md-4">
                            <label for="transportationAllowance" class="form-label">Transportation Allowance</label>
                            <input type="number" step="1000" min="0" id="transportationAllowance" name="transportationAllowance" class="form-control"
                                   value="${not empty param.transportationAllowance ? param.transportationAllowance : '0'}">
                        </div>
                        <div class="col-md-4">
                            <label for="phoneAllowance" class="form-label">Phone Allowance</label>
                            <input type="number" step="1000" min="0" id="phoneAllowance" name="phoneAllowance" class="form-control"
                                   value="${not empty param.phoneAllowance ? param.phoneAllowance : '0'}">
                        </div>
                        <div class="col-md-4">
                            <label for="responsibilityAllowance" class="form-label">Responsibility Allowance</label>
                            <input type="number" step="1000" min="0" id="responsibilityAllowance" name="responsibilityAllowance" class="form-control"
                                   value="${not empty param.responsibilityAllowance ? param.responsibilityAllowance : '0'}">
                        </div>
                    </div>

                    <div class="section-title">Note</div>
                    <div class="mb-4">
                        <textarea id="note" name="note" class="form-control" rows="2" maxlength="255"
                                  placeholder="Optional note">${not empty param.note ? param.note : ''}</textarea>
                    </div>

                    <div class="d-flex justify-content-end gap-2">
                        <a href="${pageContext.request.contextPath}/contracts" class="btn btn-outline-secondary px-4">Cancel</a>
                        <button type="submit" class="btn btn-primary px-4 fw-medium"
                                style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                            <i class="bi bi-check-lg me-2"></i>Create Contract
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="contracts" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${readonly ? 'Contract Detail' : 'Edit Contract'} &mdash; HRM System</title>
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
        .emp-banner {
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            color: white; border-radius: 12px; padding: 1rem 1.25rem;
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
            <h5 class="fw-bold text-dark mb-0">${readonly ? 'Contract Detail' : 'Edit Contract'}</h5>
            <small class="text-muted">${contract.contractCode} &middot; ${contract.employeeFullName}</small>
        </div>
    </div>

    <div class="row justify-content-center">
        <div class="col-xl-9">

            <div class="emp-banner d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
                <div>
                    <div class="opacity-75 small">Employee</div>
                    <div class="fw-bold">${contract.employeeFullName} (${contract.employeeCode})</div>
                    <div class="opacity-75 small">${contract.departmentName}</div>
                </div>
                <span class="badge bg-white bg-opacity-25 text-white px-3">${contract.status}</span>
            </div>

            <c:if test="${not empty error}">
                <div class="alert alert-danger d-flex align-items-center gap-2 py-2 mb-3">
                    <i class="bi bi-exclamation-circle-fill"></i><span>${error}</span>
                </div>
            </c:if>

            <c:if test="${readonly && contract.status != 'Active'}">
                <div class="alert alert-secondary d-flex align-items-center gap-2 py-2 mb-3">
                    <i class="bi bi-info-circle-fill"></i>
                    <span>This contract is <strong>${contract.status}</strong> and is read-only.</span>
                </div>
            </c:if>

            <div class="card border-0 shadow-sm rounded-3 p-4">
                <form action="${pageContext.request.contextPath}/contracts?action=edit" method="post" novalidate>
                    <input type="hidden" name="contractId" value="${contract.contractId}">

                    <div class="section-title">Contract Details</div>
                    <div class="row g-3 mb-4">
                        <div class="col-md-6">
                            <label class="form-label text-muted">Contract Code</label>
                            <input type="text" class="form-control bg-light" value="${contract.contractCode}" disabled>
                        </div>
                        <div class="col-md-6">
                            <label for="contractType" class="form-label required">Contract Type</label>
                            <select id="contractType" name="contractType" class="form-select" ${readonly ? 'disabled' : ''} required>
                                <c:forEach var="t" items="${contractTypes}">
                                    <option value="${t}" ${contract.contractType == t ? 'selected' : ''}>${t.dbValue}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label for="startDate" class="form-label required">Start Date</label>
                            <input type="date" id="startDate" name="startDate" class="form-control"
                                   value="${contract.startDate}" ${readonly ? 'disabled' : ''} required>
                        </div>
                        <div class="col-md-6">
                            <label for="endDate" class="form-label">End Date</label>
                            <input type="date" id="endDate" name="endDate" class="form-control"
                                   value="${contract.endDate}" ${readonly ? 'disabled' : ''}>
                        </div>
                    </div>

                    <div class="section-title">Salary (VND)</div>
                    <div class="row g-3 mb-4">
                        <div class="col-md-6">
                            <label for="basicSalary" class="form-label required">Basic Salary</label>
                            <input type="number" step="1000" min="0" id="basicSalary" name="basicSalary" class="form-control"
                                   value="${contract.basicSalary}" ${readonly ? 'disabled' : ''} required>
                        </div>
                        <div class="col-md-6">
                            <label for="standardWorkingDays" class="form-label required">Standard Working Days</label>
                            <input type="number" step="0.5" min="1" max="31" id="standardWorkingDays" name="standardWorkingDays" class="form-control"
                                   value="${contract.standardWorkingDays}" ${readonly ? 'disabled' : ''} required>
                        </div>
                        <div class="col-12">
                            <div class="alert alert-info d-flex align-items-center gap-2 py-2 mb-0">
                                <i class="bi bi-wallet2"></i>
                                <span>
                                    Allowances are managed globally in
                                    <c:choose>
                                        <c:when test="${permissions.contains('MANAGE_ALLOWANCE')}">
                                            <a href="${pageContext.request.contextPath}/allowances" class="alert-link">Manage Allowance</a>.
                                        </c:when>
                                        <c:otherwise>Manage Allowance.</c:otherwise>
                                    </c:choose>
                                </span>
                            </div>
                        </div>
                    </div>

                    <div class="section-title">Note</div>
                    <div class="mb-4">
                        <textarea id="note" name="note" class="form-control" rows="2" maxlength="255"
                                  ${readonly ? 'disabled' : ''}>${contract.note}</textarea>
                    </div>

                    <div class="d-flex justify-content-end gap-2">
                        <a href="${pageContext.request.contextPath}/contracts" class="btn btn-outline-secondary px-4">
                            ${readonly ? 'Close' : 'Cancel'}
                        </a>
                        <c:if test="${not readonly}">
                            <button type="submit" class="btn btn-primary px-4 fw-medium"
                                    style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                                <i class="bi bi-check-lg me-2"></i>Save Changes
                            </button>
                        </c:if>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="activePage" value="accountRequests" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Employee Account Requests &mdash; HRM System</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body { background-color:#f4f6f9; }
        .sidebar {
            width:240px; min-height:calc(100vh - 56px); background-color:#1a3c5e;
            position:fixed; top:56px; left:0; padding-top:1rem; z-index:100;
        }
        .sidebar .nav-link {
            color:rgba(255,255,255,0.75); padding:0.6rem 1.25rem;
            border-radius:6px; margin:2px 10px; font-size:0.9rem; transition:all 0.2s;
        }
        .sidebar .nav-link:hover, .sidebar .nav-link.active { color:#fff; background-color:rgba(255,255,255,0.12); }
        .sidebar .nav-link i { width:20px; }
        .sidebar-label {
            font-size:0.7rem; text-transform:uppercase; letter-spacing:1px;
            color:rgba(255,255,255,0.4); padding:0.75rem 1.25rem 0.25rem;
        }
        .main-content { margin-left:240px; padding:2rem; }
        .table th { font-size:0.78rem; text-transform:uppercase; letter-spacing:0.5px; color:#6b7280; font-weight:600; }
        .table td { vertical-align:middle; font-size:0.9rem; }
        .status-pill { padding:3px 10px; border-radius:20px; font-size:0.72rem; font-weight:600; }
        .st-pending { background:#fff7ed; color:#9a3412; }
        .st-created { background:#dcfce7; color:#166534; }
        .st-rejected { background:#fee2e2; color:#b91c1c; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
            <h5 class="fw-bold text-dark mb-0">Employee Account Requests</h5>
            <small class="text-muted">
                <c:choose>
                    <c:when test="${adminScope}">Review onboarding requests and create accounts with contracts</c:when>
                    <c:otherwise>Submit employee contract information for Admin account creation</c:otherwise>
                </c:choose>
            </small>
        </div>
    </div>

    <c:if test="${not empty accountRequestMessage}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2">
            <i class="bi bi-check-circle-fill"></i><span>${accountRequestMessage}</span>
        </div>
    </c:if>
    <c:if test="${not empty accountRequestError}">
        <div class="alert alert-danger d-flex align-items-center gap-2 py-2">
            <i class="bi bi-exclamation-circle-fill"></i><span>${accountRequestError}</span>
        </div>
    </c:if>

    <c:if test="${canRequestAccount}">
        <div class="card border-0 shadow-sm rounded-3 mb-4">
            <div class="card-body">
                <h6 class="fw-bold mb-3"><i class="bi bi-person-plus me-2"></i>New Contract & Account Request</h6>
                <form method="post" action="${pageContext.request.contextPath}/employee-account-requests?action=createRequest"
                      enctype="multipart/form-data" class="row g-3">
                    <div class="col-md-4">
                        <label class="form-label small text-muted mb-1">Full Name</label>
                        <input type="text" name="fullName" class="form-control" maxlength="100" required>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label small text-muted mb-1">Email</label>
                        <input type="email" name="email" class="form-control" maxlength="100" required>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label small text-muted mb-1">Phone</label>
                        <input type="text" name="phone" class="form-control" maxlength="15"
                               pattern="[0-9]{10,15}" inputmode="numeric" required>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Gender</label>
                        <select name="gender" class="form-select" required>
                            <c:forEach var="g" items="${genders}">
                                <option value="${g}">${g}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Date of Birth</label>
                        <input id="dateOfBirth" type="date" name="dateOfBirth" class="form-control" required>
                        <div class="invalid-feedback">Chua du 18 tuoi.</div>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Hire Date</label>
                        <input type="date" name="hireDate" class="form-control" value="${today}" required>
                    </div>
                    <c:if test="${hrManagerRequestScope}">
                        <div class="col-md-3">
                            <label class="form-label small text-muted mb-1">Requested Role</label>
                            <select id="requestedRoleId" name="requestedRoleId" class="form-select" required>
                                <option value="">Select role</option>
                                <c:forEach var="role" items="${requestRoles}">
                                    <option value="${role.roleId}" data-role="${fn:trim(role.roleName)}">
                                        ${role.roleName}
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                    </c:if>
                    <div class="col-md-4">
                        <label class="form-label small text-muted mb-1">Department</label>
                        <select id="departmentId" name="departmentId" class="form-select" required>
                            <option value="">Select department</option>
                            <c:forEach var="d" items="${departments}">
                                <option value="${d.departmentId}" data-code="${fn:trim(d.departmentCode)}">
                                    ${d.departmentName}
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-md-5">
                        <label class="form-label small text-muted mb-1">Address</label>
                        <input type="text" name="address" class="form-control" maxlength="255" required>
                    </div>
                    <div class="col-12"><hr class="my-1"></div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Contract Type</label>
                        <select name="contractType" class="form-select" required>
                            <c:forEach var="ct" items="${contractTypes}">
                                <option value="${ct}">${ct.dbValue}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Contract Start</label>
                        <input id="contractStartDate" type="date" name="contractStartDate" class="form-control" value="${today}" required>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Contract End</label>
                        <input id="contractEndDate" type="date" name="contractEndDate" class="form-control">
                        <div class="invalid-feedback">Contract end must be at least 1 month after contract start.</div>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Basic Salary</label>
                        <input type="number" name="basicSalary" class="form-control" min="0" step="1000" required>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Standard Working Days</label>
                        <input type="number" name="standardWorkingDays" class="form-control" min="1" max="31" step="0.5" value="26" required>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label small text-muted mb-1">Contract Note</label>
                        <input type="text" name="contractNote" class="form-control" maxlength="255">
                    </div>
                    <div class="col-md-6">
                        <label class="form-label small text-muted mb-1">Signed Contract Document</label>
                        <input type="file" name="contractDocument" class="form-control"
                               accept="application/pdf,image/png,image/jpeg" required>
                        <div class="form-text">PDF, JPG, or PNG. Maximum 10 MB.</div>
                    </div>
                    <div class="col-12 text-end">
                        <button type="submit" class="btn btn-primary btn-sm px-3"
                                style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                            <i class="bi bi-send me-1"></i>Submit Request
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </c:if>

    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="p-3 border-bottom d-flex align-items-center gap-2">
                <i class="bi bi-person-lines-fill text-muted"></i>
                <span class="fw-medium">${totalRequests} request(s)</span>
            </div>
            <div class="table-responsive">
                <table class="table table-hover mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">#</th>
                            <th>Employee</th>
                            <th>Role / Department</th>
                            <th>Contract</th>
                            <th>Requested By</th>
                            <th>Status</th>
                            <th>Note</th>
                            <c:if test="${adminScope}">
                                <th class="text-center">Actions</th>
                            </c:if>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="req" items="${requests}" varStatus="s">
                            <tr>
                                <td class="ps-4 text-muted">${(currentPage - 1) * 10 + s.index + 1}</td>
                                <td>
                                    <div class="fw-medium">${req.fullName}</div>
                                    <div class="text-muted" style="font-size:0.78rem;">${req.email}</div>
                                    <c:if test="${not empty req.employeeCode}">
                                        <div class="text-muted" style="font-size:0.78rem;">${req.employeeCode}</div>
                                    </c:if>
                                </td>
                                <td>
                                    <div class="fw-medium">${not empty req.requestedRoleName ? req.requestedRoleName : 'EMPLOYEE'}</div>
                                    <div class="text-muted" style="font-size:0.78rem;">${req.departmentName}</div>
                                    <c:if test="${not empty req.positionTitle}">
                                        <div class="text-muted" style="font-size:0.78rem;">${req.positionTitle}</div>
                                    </c:if>
                                </td>
                                <td>
                                    <div class="fw-medium">${req.contractCode}</div>
                                    <div class="text-muted" style="font-size:0.78rem;">
                                        <c:if test="${not empty req.contractType}">${req.contractType.dbValue}</c:if>
                                        <c:if test="${not empty req.contractStartDate}"> &middot; ${req.contractStartDate}</c:if>
                                    </div>
                                    <c:if test="${not empty req.basicSalary}">
                                        <div class="text-muted" style="font-size:0.78rem;">
                                            Salary: <fmt:formatNumber value="${req.basicSalary}" type="number" maxFractionDigits="0"/>
                                        </div>
                                    </c:if>
                                    <c:if test="${not empty req.contractDocumentOriginalName}">
                                        <div class="text-muted" style="font-size:0.78rem;">
                                            Document: ${req.contractDocumentOriginalName}
                                        </div>
                                    </c:if>
                                </td>
                                <td class="text-muted">${req.requestedByName}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${req.status == 'Pending'}">
                                            <span class="status-pill st-pending">Pending</span>
                                        </c:when>
                                        <c:when test="${req.status == 'Created'}">
                                            <span class="status-pill st-created">Created</span>
                                        </c:when>
                                        <c:when test="${req.status == 'Rejected'}">
                                            <span class="status-pill st-rejected">Rejected</span>
                                        </c:when>
                                        <c:otherwise>${req.status}</c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-muted" style="max-width:240px;">${not empty req.adminNote ? req.adminNote : '-'}</td>
                                <c:if test="${adminScope}">
                                    <td class="text-center">
                                        <c:choose>
                                            <c:when test="${req.status == 'Pending'}">
                                                <div class="d-inline-flex gap-1">
                                                    <form method="post" action="${pageContext.request.contextPath}/employee-account-requests?action=approve"
                                                          onsubmit="return confirm('Create account, employee profile, and active contract for ${req.fullName}?');">
                                                        <input type="hidden" name="requestId" value="${req.requestId}">
                                                        <button type="submit" class="btn btn-sm btn-outline-success" title="Create Account">
                                                            <i class="bi bi-person-check"></i>
                                                        </button>
                                                    </form>
                                                    <form method="post" action="${pageContext.request.contextPath}/employee-account-requests?action=reject"
                                                          onsubmit="const reason = prompt('Reason for rejection:'); if (!reason) return false; this.note.value = reason; return true;">
                                                        <input type="hidden" name="requestId" value="${req.requestId}">
                                                        <input type="hidden" name="note" value="">
                                                        <button type="submit" class="btn btn-sm btn-outline-danger" title="Reject">
                                                            <i class="bi bi-x-lg"></i>
                                                        </button>
                                                    </form>
                                                </div>
                                            </c:when>
                                            <c:otherwise><span class="text-muted">&mdash;</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                </c:if>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty requests}">
                            <tr>
                                <td colspan="${adminScope ? 8 : 7}" class="text-center text-muted py-5">
                                    <i class="bi bi-inbox fs-2 d-block mb-2 opacity-25"></i>
                                    No account requests found.
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
                    Page ${currentPage} of ${totalPages} &middot; ${totalRequests} request(s)
                </small>
                <nav>
                    <ul class="pagination pagination-sm mb-0">
                        <li class="page-item ${currentPage == 1 ? 'disabled' : ''}">
                            <a class="page-link" href="?page=${currentPage - 1}">Previous</a>
                        </li>
                        <c:forEach var="p" begin="1" end="${totalPages}">
                            <li class="page-item ${p == currentPage ? 'active' : ''}">
                                <a class="page-link" href="?page=${p}">${p}</a>
                            </li>
                        </c:forEach>
                        <li class="page-item ${currentPage == totalPages ? 'disabled' : ''}">
                            <a class="page-link" href="?page=${currentPage + 1}">Next</a>
                        </li>
                    </ul>
                </nav>
            </div>
        </c:if>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
document.addEventListener('DOMContentLoaded', function () {
    const dobInput = document.getElementById('dateOfBirth');
    const contractStartInput = document.getElementById('contractStartDate');
    const contractEndInput = document.getElementById('contractEndDate');
    const roleSelect = document.getElementById('requestedRoleId');
    const deptSelect = document.getElementById('departmentId');

    function formatDate(date) {
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return date.getFullYear() + '-' + month + '-' + day;
    }

    function addMonths(date, months) {
        const result = new Date(date.getTime());
        const day = result.getDate();
        result.setMonth(result.getMonth() + months);
        if (result.getDate() < day) result.setDate(0);
        return result;
    }

    function validateDob() {
        if (!dobInput) return;
        if (!dobInput.value) {
            dobInput.setCustomValidity('');
            dobInput.classList.remove('is-invalid');
            return;
        }
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const minimumDob = new Date(today.getFullYear() - 18, today.getMonth(), today.getDate());
        const dob = new Date(dobInput.value + 'T00:00:00');
        const invalid = dob > minimumDob;
        dobInput.setCustomValidity(invalid ? 'Chua du 18 tuoi.' : '');
        dobInput.classList.toggle('is-invalid', invalid);
    }

    function validateContractEnd() {
        if (!contractStartInput || !contractEndInput) return;
        if (contractStartInput.value) {
            const startDate = new Date(contractStartInput.value + 'T00:00:00');
            contractEndInput.min = formatDate(addMonths(startDate, 1));
        }
        if (!contractEndInput.value || !contractStartInput.value) {
            if (contractEndInput) {
                contractEndInput.setCustomValidity('');
                contractEndInput.classList.remove('is-invalid');
            }
            return;
        }
        const startDate = new Date(contractStartInput.value + 'T00:00:00');
        const endDate = new Date(contractEndInput.value + 'T00:00:00');
        const minimumEndDate = addMonths(startDate, 1);
        const invalid = endDate < minimumEndDate;
        contractEndInput.setCustomValidity(invalid ? 'Contract end must be at least 1 month after contract start.' : '');
        contractEndInput.classList.toggle('is-invalid', invalid);
    }

    if (dobInput) {
        const today = new Date();
        dobInput.max = formatDate(new Date(today.getFullYear() - 18, today.getMonth(), today.getDate()));
        dobInput.addEventListener('input', validateDob);
        dobInput.addEventListener('blur', validateDob);
        validateDob();
    }

    if (contractStartInput && contractEndInput) {
        contractStartInput.addEventListener('input', validateContractEnd);
        contractEndInput.addEventListener('input', validateContractEnd);
        validateContractEnd();
    }

    if (!deptSelect) return;

    function selectedRoleName() {
        if (!roleSelect) return 'EMPLOYEE';
        const option = roleSelect.options[roleSelect.selectedIndex];
        return option ? (option.getAttribute('data-role') || '').trim().toUpperCase() : '';
    }

    function applyDepartmentFilter() {
        const roleName = selectedRoleName();
        let firstAllowed = '';

        Array.from(deptSelect.options).forEach(function (option) {
            if (!option.value) {
                option.hidden = false;
                option.disabled = false;
                return;
            }

            const code = (option.getAttribute('data-code') || '').trim().toUpperCase();
            const allowed = roleName === 'HR_STAFF'
                    ? code === 'HR'
                    : code !== 'HR' && code !== 'ADMIN_DEPT' && code !== 'IT';

            option.hidden = !allowed;
            option.disabled = !allowed;
            if (allowed && !firstAllowed) firstAllowed = option.value;
        });

        const selected = deptSelect.options[deptSelect.selectedIndex];
        if (selected && selected.disabled) {
            deptSelect.value = roleName === 'HR_STAFF' ? firstAllowed : '';
        } else if (roleName === 'HR_STAFF' && !deptSelect.value) {
            deptSelect.value = firstAllowed;
        }
    }

    if (roleSelect) {
        roleSelect.addEventListener('change', applyDepartmentFilter);
    }
    applyDepartmentFilter();
});
</script>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
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
        .document-preview {
            width: 180px; height: 120px; border: 1px solid #dbe3ea;
            border-radius: 8px; background: #f8fafc; object-fit: cover;
        }
        .document-frame { width: 100%; height: 100%; border: 0; border-radius: 8px; }
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
            <c:if test="${systemContract && canEditSystemContract}">
                <div class="alert alert-info d-flex align-items-center gap-2 py-2 mb-3">
                    <i class="bi bi-lock-fill"></i>
                    <span>This is a system-seeded contract. HR Manager can edit it.</span>
                </div>
            </c:if>

            <div class="card border-0 shadow-sm rounded-3 p-4">
                <form action="${pageContext.request.contextPath}/contracts?action=edit&id=${contract.contractId}" method="post"
                      enctype="multipart/form-data" novalidate>
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
                            <label for="workScheduleId" class="form-label required">Work Schedule</label>
                            <select id="workScheduleId" name="workScheduleId" class="form-select"
                                    ${readonly ? 'disabled' : ''} required>
                                <c:forEach var="schedule" items="${workSchedules}">
                                    <option value="${schedule.workScheduleId}"
                                            ${contract.workScheduleId == schedule.workScheduleId ? 'selected' : ''}>
                                        ${schedule.scheduleName} (${schedule.dailyWorkingHours} hours/day)
                                    </option>
                                </c:forEach>
                            </select>
                            <div class="form-text">Monthly working days are calculated from this schedule.</div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label text-muted">Salary Policy</label>
                            <input type="text" class="form-control bg-light" value="${contract.salaryPolicy.dbValue}" disabled>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label text-muted">Fixed Allowance</label>
                            <fmt:formatNumber var="fixedAllowanceFormatted" value="${contract.fixedAllowanceAmount}"
                                              type="number" maxFractionDigits="0"/>
                            <input type="text" class="form-control bg-light"
                                   value="${fixedAllowanceFormatted} &#8363;" disabled>
                        </div>
                        <div class="col-12">
                            <div class="table-responsive">
                                <table class="table table-sm align-middle mb-0">
                                    <thead class="table-light">
                                    <tr>
                                        <th>Active Allowance</th>
                                        <th class="text-end">Amount</th>
                                        <th>Applies To</th>
                                        <th>Description</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:choose>
                                        <c:when test="${empty activeAllowanceTypes}">
                                            <tr>
                                                <td colspan="4" class="text-muted text-center py-3">
                                                    No active allowance policy is configured.
                                                </td>
                                            </tr>
                                        </c:when>
                                        <c:otherwise>
                                            <c:forEach var="allowance" items="${activeAllowanceTypes}">
                                                <tr>
                                                    <td class="fw-medium">${allowance.allowanceName}</td>
                                                    <td class="text-end">
                                                        <fmt:formatNumber value="${allowance.amount}" type="number" maxFractionDigits="0"/>
                                                        &#8363;
                                                    </td>
                                                    <td><span class="badge text-bg-light">${allowance.appliesToLabel}</span></td>
                                                    <td class="text-muted small">${not empty allowance.description ? allowance.description : '-'}</td>
                                                </tr>
                                            </c:forEach>
                                            <tr class="table-light">
                                                <td class="fw-semibold">Common Allowance Total</td>
                                                <td class="text-end fw-semibold">
                                                    <fmt:formatNumber value="${commonActiveAllowance}" type="number" maxFractionDigits="0"/>
                                                    &#8363;
                                                </td>
                                                <td colspan="2" class="text-muted small">Responsibility allowance is applied by role during payroll.</td>
                                            </tr>
                                        </c:otherwise>
                                    </c:choose>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>

                    <div class="section-title">Document & Note</div>
                    <div class="row g-3 mb-4">
                        <div class="col-md-6">
                            <label class="form-label">Current Contract Document</label>
                            <c:choose>
                                <c:when test="${not empty contract.document}">
                                    <c:url var="docUrl" value="/contract-document">
                                        <c:param name="id" value="${contract.document.documentId}"/>
                                    </c:url>
                                    <div class="d-flex align-items-center gap-3 flex-wrap">
                                        <button type="button" class="btn p-0 border-0 bg-transparent"
                                                data-bs-toggle="modal" data-bs-target="#contractDocumentModal">
                                            <c:choose>
                                                <c:when test="${contract.document.image}">
                                                    <img src="${docUrl}" alt="Contract document preview" class="document-preview">
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="document-preview d-inline-block">
                                                        <iframe src="${docUrl}" class="document-frame" title="Contract PDF preview"></iframe>
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </button>
                                        <div>
                                            <div class="fw-medium">${contract.document.originalFileName}</div>
                                            <button type="button" class="btn btn-sm btn-outline-primary mt-2"
                                                    data-bs-toggle="modal" data-bs-target="#contractDocumentModal">
                                                <i class="bi bi-arrows-fullscreen me-1"></i>View Large
                                            </button>
                                        </div>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="text-muted small">No contract document has been uploaded.</div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div class="col-md-6">
                            <c:if test="${not readonly}">
                                <label for="contractDocument" class="form-label">Replace Contract Document</label>
                                <input type="file" id="contractDocument" name="contractDocument" class="form-control"
                                       accept="application/pdf,image/png,image/jpeg">
                                <div class="form-text">PDF, JPG, or PNG. Maximum 10 MB.</div>
                            </c:if>
                            <label for="note" class="form-label mt-3">Note</label>
                            <textarea id="note" name="note" class="form-control" rows="2" maxlength="255"
                                      ${readonly ? 'disabled' : ''}>${contract.note}</textarea>
                        </div>
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

<c:if test="${not empty contract.document}">
    <div class="modal fade" id="contractDocumentModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-xl modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header">
                    <h6 class="modal-title fw-bold">${contract.document.originalFileName}</h6>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-2">
                    <c:choose>
                        <c:when test="${contract.document.image}">
                            <img src="${docUrl}" alt="Contract document" class="w-100 rounded">
                        </c:when>
                        <c:otherwise>
                            <iframe src="${docUrl}" title="Contract document"
                                    style="width:100%;height:75vh;border:0;border-radius:8px;"></iframe>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>
</c:if>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="activePage" value="myContract" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Contract &mdash; HRM System</title>
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
        .info-row { padding: 0.65rem 0; border-bottom: 1px solid #f1f3f5; }
        .info-row:last-child { border-bottom: none; }
        .status-pill { padding: 4px 12px; border-radius: 20px; font-size: 0.78rem; font-weight: 600; }
        .status-active     { background:#e6f9f0; color:#166534; }
        .status-expired    { background:#fff8e1; color:#a16207; }
        .status-terminated { background:#fee2e2; color:#b91c1c; }
        .contract-banner {
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            color: white; border-radius: 14px; padding: 1.5rem;
        }
        .money { font-weight: 600; }
        .salary-total { background:#f0f9ff; border:1px solid #bae6fd; border-radius:10px; padding:1rem; }
        .document-preview {
            width: 180px; height: 120px; border: 1px solid #dbe3ea;
            border-radius: 8px; background: #f8fafc; object-fit: cover;
        }
        .document-frame { width:100%; height:100%; border:0; border-radius:8px; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="mb-4">
        <h5 class="fw-bold text-dark mb-0">My Contract</h5>
        <small class="text-muted">Your employment contract information</small>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-warning d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-triangle-fill"></i><span>${error}</span>
        </div>
    </c:if>

    <c:choose>
        <c:when test="${hasFixedMonthlyContract}">
            <div class="alert alert-info d-flex align-items-center gap-2 py-2 mb-4">
                <i class="bi bi-lock-fill"></i>
                <span>Your active contract uses a fixed monthly salary policy.</span>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card border-0 shadow-sm rounded-3 mb-4">
                <div class="card-body p-4">
                    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
                        <h6 class="fw-semibold mb-0 text-secondary">
                            <i class="bi bi-wallet2 me-2"></i>Current Monthly Allowances
                        </h6>
                        <span class="badge text-bg-primary">
                            Total:
                            <fmt:formatNumber value="${totalActiveAllowance}" type="number" maxFractionDigits="0"/>
                            &#8363;
                        </span>
                    </div>
                    <c:choose>
                        <c:when test="${empty activeAllowanceTypes}">
                            <div class="text-muted small">No active allowance policy is configured.</div>
                        </c:when>
                        <c:otherwise>
                            <div class="table-responsive">
                                <table class="table table-sm align-middle mb-0">
                                    <thead class="table-light">
                                    <tr>
                                        <th>Allowance</th>
                                        <th class="text-end">Amount</th>
                                        <th>Applies To</th>
                                        <th>Description</th>
                                    </tr>
                                    </thead>
                                    <tbody>
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
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </c:otherwise>
    </c:choose>

    <c:choose>
        <c:when test="${empty contracts}">
            <c:if test="${empty error}">
                <div class="card border-0 shadow-sm rounded-3 p-5 text-center text-muted">
                    <i class="bi bi-file-earmark-x fs-1 d-block mb-3 opacity-25"></i>
                    You have no contract on record yet. Please contact HR.
                </div>
            </c:if>
        </c:when>
        <c:otherwise>
            <c:forEach var="ct" items="${contracts}">
                <div class="card border-0 shadow-sm rounded-3 mb-4 overflow-hidden">
                    <div class="contract-banner d-flex align-items-center justify-content-between flex-wrap gap-3">
                        <div>
                            <div class="opacity-75 small">Contract Code</div>
                            <div class="fw-bold fs-5">${ct.contractCode}</div>
                            <div class="opacity-75 small">${ct.contractType.dbValue}</div>
                        </div>
                        <div class="text-end">
                            <c:choose>
                                <c:when test="${ct.status == 'Active'}">
                                    <span class="status-pill status-active">Active</span>
                                </c:when>
                                <c:when test="${ct.status == 'Expired'}">
                                    <span class="status-pill status-expired">Expired</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="status-pill status-terminated">Terminated</span>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <div class="p-4">
                        <div class="row g-4">
                            <div class="col-lg-6">
                                <h6 class="fw-semibold mb-3 text-secondary"><i class="bi bi-calendar-range me-2"></i>Period</h6>
                                <div class="info-row row align-items-center">
                                    <div class="col-5 text-muted small">Start Date</div>
                                    <div class="col-7 fw-medium">${ct.startDate}</div>
                                </div>
                                <div class="info-row row align-items-center">
                                    <div class="col-5 text-muted small">End Date</div>
                                    <div class="col-7">${not empty ct.endDate ? ct.endDate : 'Indefinite'}</div>
                                </div>
                                <div class="info-row row align-items-center">
                                    <div class="col-5 text-muted small">Standard Working Days</div>
                                    <div class="col-7">${ct.standardWorkingDays}</div>
                                </div>
                                <div class="info-row row align-items-center">
                                    <div class="col-5 text-muted small">Salary Policy</div>
                                    <div class="col-7">${ct.salaryPolicy.dbValue}</div>
                                </div>
                                <c:if test="${not empty ct.note}">
                                    <div class="info-row row align-items-center">
                                        <div class="col-5 text-muted small">Note</div>
                                        <div class="col-7">${ct.note}</div>
                                    </div>
                                </c:if>
                            </div>

                            <div class="col-lg-6">
                                <h6 class="fw-semibold mb-3 text-secondary"><i class="bi bi-cash-stack me-2"></i>Salary</h6>
                                <div class="info-row row align-items-center">
                                    <div class="col-7 text-muted small">Basic Salary</div>
                                    <div class="col-5 money"><fmt:formatNumber value="${ct.basicSalary}" type="number" maxFractionDigits="0"/> &#8363;</div>
                                </div>
                                <c:if test="${ct.fixedAllowanceAmount > 0}">
                                    <div class="info-row row align-items-center">
                                        <div class="col-7 text-muted small">Fixed Responsibility Allowance</div>
                                        <div class="col-5 money">
                                            <fmt:formatNumber value="${ct.fixedAllowanceAmount}" type="number" maxFractionDigits="0"/> &#8363;
                                        </div>
                                    </div>
                                </c:if>
                                <div class="salary-total d-flex justify-content-between align-items-center mt-3">
                                    <span class="fw-semibold text-primary">Monthly Contract Package</span>
                                    <span class="fw-bold text-primary">
                                        <fmt:formatNumber value="${ct.basicSalary + ct.fixedAllowanceAmount}" type="number" maxFractionDigits="0"/> &#8363;
                                    </span>
                                </div>
                            </div>
                        </div>

                        <div class="border-top mt-4 pt-4">
                            <h6 class="fw-semibold mb-3 text-secondary"><i class="bi bi-file-earmark-pdf me-2"></i>Contract Document</h6>
                            <c:choose>
                                <c:when test="${not empty ct.document}">
                                    <c:url var="docUrl" value="/contract-document">
                                        <c:param name="id" value="${ct.document.documentId}"/>
                                    </c:url>
                                    <div class="d-flex align-items-center gap-3 flex-wrap">
                                        <button type="button" class="btn p-0 border-0 bg-transparent"
                                                data-bs-toggle="modal" data-bs-target="#contractDocumentModal${ct.contractId}">
                                            <c:choose>
                                                <c:when test="${ct.document.image}">
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
                                            <div class="fw-medium">${ct.document.originalFileName}</div>
                                            <button type="button" class="btn btn-sm btn-outline-primary mt-2"
                                                    data-bs-toggle="modal" data-bs-target="#contractDocumentModal${ct.contractId}">
                                                <i class="bi bi-arrows-fullscreen me-1"></i>View Large
                                            </button>
                                        </div>
                                    </div>

                                    <div class="modal fade" id="contractDocumentModal${ct.contractId}" tabindex="-1" aria-hidden="true">
                                        <div class="modal-dialog modal-xl modal-dialog-centered">
                                            <div class="modal-content">
                                                <div class="modal-header">
                                                    <h6 class="modal-title fw-bold">${ct.document.originalFileName}</h6>
                                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                </div>
                                                <div class="modal-body p-2">
                                                    <c:choose>
                                                        <c:when test="${ct.document.image}">
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
                                </c:when>
                                <c:otherwise>
                                    <div class="text-muted small">No contract document has been uploaded.</div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

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
                                <div class="alert alert-info d-flex align-items-center gap-2 py-2 mt-3 mb-0">
                                    <i class="bi bi-wallet2"></i>
                                    <span>Monthly allowances are managed globally and applied during payroll calculation.</span>
                                </div>
                                <div class="salary-total d-flex justify-content-between align-items-center mt-3">
                                    <span class="fw-semibold text-primary">Contract Salary</span>
                                    <span class="fw-bold text-primary">
                                        <fmt:formatNumber value="${ct.basicSalary}" type="number" maxFractionDigits="0"/> &#8363;
                                    </span>
                                </div>
                            </div>
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

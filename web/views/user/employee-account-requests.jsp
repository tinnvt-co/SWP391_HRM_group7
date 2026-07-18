<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
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
                    <c:when test="${adminScope}">Review HR Staff requests and create employee accounts</c:when>
                    <c:otherwise>Submit employee information for Admin account creation</c:otherwise>
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

    <c:if test="${hrStaffScope}">
        <div class="card border-0 shadow-sm rounded-3 mb-4">
            <div class="card-body">
                <h6 class="fw-bold mb-3"><i class="bi bi-person-plus me-2"></i>New Request</h6>
                <form method="post" action="${pageContext.request.contextPath}/employee-account-requests?action=createRequest" class="row g-3">
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
                        <input type="text" name="phone" class="form-control" maxlength="20">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Gender</label>
                        <select name="gender" class="form-select">
                            <c:forEach var="g" items="${genders}">
                                <option value="${g}">${g}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Date of Birth</label>
                        <input type="date" name="dateOfBirth" class="form-control">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Hire Date</label>
                        <input type="date" name="hireDate" class="form-control" value="${today}" required>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small text-muted mb-1">Employee Code</label>
                        <input type="text" name="employeeCode" class="form-control" maxlength="20">
                    </div>
                    <div class="col-md-4">
                        <label class="form-label small text-muted mb-1">Department</label>
                        <select name="departmentId" class="form-select" required>
                            <option value="">Select department</option>
                            <c:forEach var="d" items="${departments}">
                                <option value="${d.departmentId}">${d.departmentName}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-md-8">
                        <label class="form-label small text-muted mb-1">Address</label>
                        <input type="text" name="address" class="form-control" maxlength="255">
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
                            <th>Department</th>
                            <th>Requested By</th>
                            <th>Status</th>
                            <th>Account</th>
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
                                <td>${req.departmentName}</td>
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
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty req.createdUsername}">
                                            <span class="text-muted">@${req.createdUsername}</span>
                                        </c:when>
                                        <c:otherwise><span class="text-muted">&mdash;</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-muted" style="max-width:240px;">${not empty req.adminNote ? req.adminNote : '-'}</td>
                                <c:if test="${adminScope}">
                                    <td class="text-center">
                                        <c:choose>
                                            <c:when test="${req.status == 'Pending'}">
                                                <div class="d-inline-flex gap-1">
                                                    <form method="post" action="${pageContext.request.contextPath}/employee-account-requests?action=approve"
                                                          onsubmit="return confirm('Create an employee account for ${req.fullName}?');">
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
</body>
</html>

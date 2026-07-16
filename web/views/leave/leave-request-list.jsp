<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="leaveList" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Leave Requests &mdash; HRM System</title>
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
        .table th { font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.5px; color: #6b7280; font-weight: 600; }
        .table td { vertical-align: middle; font-size: 0.9rem; }
        .avatar-sm {
            width: 34px; height: 34px; border-radius: 50%;
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            display: inline-flex; align-items: center; justify-content: center;
            font-size: 0.8rem; font-weight: 700; color: white; flex-shrink: 0;
        }
        .stat-card { border: none; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); cursor: pointer; transition: transform 0.15s; }
        .stat-card:hover { transform: translateY(-2px); }
        .stat-card.active { outline: 2px solid #2d6a9f; }
        .stat-icon {
            width: 42px; height: 42px; border-radius: 10px;
            display: flex; align-items: center; justify-content: center; font-size: 1.1rem;
        }
        .status-pill {
            padding: 4px 12px; border-radius: 20px;
            font-size: 0.75rem; font-weight: 600;
            display: inline-flex; align-items: center; gap: 0.35rem;
        }
        .status-pending   { background:#fff8e1; color:#a16207; }
        .status-approved  { background:#e6f9f0; color:#166534; }
        .status-rejected  { background:#fee2e2; color:#b91c1c; }
        .status-cancelled { background:#e5e7eb; color:#4b5563; }
        .reason-cell {
            max-width: 260px; overflow: hidden;
            text-overflow: ellipsis; white-space: nowrap;
        }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
            <h5 class="fw-bold text-dark mb-0">Leave Requests</h5>
            <small class="text-muted">
                <c:choose>
                    <c:when test="${managerScope}">Requests submitted by employees you manage</c:when>
                    <c:otherwise>All leave requests across the organization</c:otherwise>
                </c:choose>
            </small>
        </div>
    </div>

    <div class="row g-3 mb-4">
        <div class="col-sm-6 col-xl-3">
            <a href="?action=list&status=Pending" class="text-decoration-none text-dark">
                <div class="stat-card card p-3 ${statusFilter == 'Pending' ? 'active' : ''}">
                    <div class="d-flex align-items-center gap-3">
                        <div class="stat-icon" style="background:#fff8e1;color:#a16207;">
                            <i class="bi bi-hourglass-split"></i>
                        </div>
                        <div>
                            <div class="text-muted small">Pending</div>
                            <div class="fw-bold fs-5">${countPending}</div>
                        </div>
                    </div>
                </div>
            </a>
        </div>
        <div class="col-sm-6 col-xl-3">
            <a href="?action=list&status=Approved" class="text-decoration-none text-dark">
                <div class="stat-card card p-3 ${statusFilter == 'Approved' ? 'active' : ''}">
                    <div class="d-flex align-items-center gap-3">
                        <div class="stat-icon" style="background:#e6f9f0;color:#166534;">
                            <i class="bi bi-check-circle"></i>
                        </div>
                        <div>
                            <div class="text-muted small">Approved</div>
                            <div class="fw-bold fs-5">${countApproved}</div>
                        </div>
                    </div>
                </div>
            </a>
        </div>
        <div class="col-sm-6 col-xl-3">
            <a href="?action=list&status=Rejected" class="text-decoration-none text-dark">
                <div class="stat-card card p-3 ${statusFilter == 'Rejected' ? 'active' : ''}">
                    <div class="d-flex align-items-center gap-3">
                        <div class="stat-icon" style="background:#fee2e2;color:#b91c1c;">
                            <i class="bi bi-x-circle"></i>
                        </div>
                        <div>
                            <div class="text-muted small">Rejected</div>
                            <div class="fw-bold fs-5">${countRejected}</div>
                        </div>
                    </div>
                </div>
            </a>
        </div>
        <div class="col-sm-6 col-xl-3">
            <a href="?action=list&status=all" class="text-decoration-none text-dark">
                <div class="stat-card card p-3 ${statusFilter == 'all' ? 'active' : ''}">
                    <div class="d-flex align-items-center gap-3">
                        <div class="stat-icon" style="background:#e3f0fb;color:#1a3c5e;">
                            <i class="bi bi-collection"></i>
                        </div>
                        <div>
                            <div class="text-muted small">All</div>
                            <div class="fw-bold fs-5">${countTotal}</div>
                        </div>
                    </div>
                </div>
            </a>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="p-3 border-bottom d-flex align-items-center gap-2 flex-wrap">
                <div class="d-flex align-items-center gap-2 flex-grow-1">
                    <i class="bi bi-search text-muted"></i>
                    <input type="text" id="searchInput" class="form-control form-control-sm border-0 shadow-none"
                           placeholder="Search by employee, type, reason..." style="max-width:340px;">
                </div>
                <form method="get" class="d-flex align-items-center gap-2">
                    <input type="hidden" name="action" value="list">
                    <label class="small text-muted mb-0">Status:</label>
                    <select name="status" class="form-select form-select-sm" style="max-width:160px;"
                            onchange="this.form.submit()">
                        <option value="Pending"   ${statusFilter == 'Pending'   ? 'selected' : ''}>Pending</option>
                        <option value="Approved"  ${statusFilter == 'Approved'  ? 'selected' : ''}>Approved</option>
                        <option value="Rejected"  ${statusFilter == 'Rejected'  ? 'selected' : ''}>Rejected</option>
                        <option value="Cancelled" ${statusFilter == 'Cancelled' ? 'selected' : ''}>Cancelled</option>
                        <option value="all"       ${statusFilter == 'all'       ? 'selected' : ''}>All</option>
                    </select>
                </form>
            </div>

            <div class="table-responsive">
                <table class="table table-hover mb-0" id="leaveTable">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">#</th>
                            <th>Employee</th>
                            <th>Type</th>
                            <th>Period</th>
                            <th>Days</th>
                            <th>Reason</th>
                            <th>Submitted</th>
                            <th>Status</th>
                            <th class="text-end pe-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="lr" items="${requests}" varStatus="s">
                            <tr>
                                <td class="ps-4 text-muted">${s.index + 1}</td>
                                <td>
                                    <div class="d-flex align-items-center gap-2">
                                        <div class="avatar-sm">${fn:substring(lr.employeeFullName, 0, 1)}</div>
                                        <div>
                                            <div class="fw-medium">${lr.employeeFullName}</div>
                                            <div class="text-muted" style="font-size:0.78rem;">${lr.employeeCode}</div>
                                        </div>
                                    </div>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${lr.leaveType == 'AnnualLeave'}">
                                            <i class="bi bi-sun text-warning me-1"></i>Annual Leave
                                        </c:when>
                                        <c:when test="${lr.leaveType == 'SickLeave'}">
                                            <i class="bi bi-bandaid text-danger me-1"></i>Sick Leave
                                        </c:when>
                                        <c:when test="${lr.leaveType == 'PersonalLeave'}">
                                            <i class="bi bi-person text-primary me-1"></i>Personal Leave
                                        </c:when>
                                        <c:when test="${lr.leaveType == 'MaternityLeave'}">
                                            <i class="bi bi-heart-pulse text-danger me-1"></i>Maternity Leave
                                        </c:when>
                                        <c:when test="${lr.leaveType == 'UnpaidLeave'}">
                                            <i class="bi bi-cash-stack text-secondary me-1"></i>Unpaid Leave
                                        </c:when>
                                        <c:otherwise>${lr.leaveType}</c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="small">
                                    <div>${lr.startDate}</div>
                                    <div class="text-muted">to ${lr.endDate}</div>
                                </td>
                                <td><span class="fw-medium">${lr.totalDays}</span></td>
                                <td class="reason-cell text-muted" title="${lr.reason}">${lr.reason}</td>
                                <td class="small text-muted">${lr.createdAt}</td>
                                <td class="pe-4">
                                    <c:choose>
                                        <c:when test="${lr.status == 'Pending'}">
                                            <span class="status-pill status-pending">
                                                <i class="bi bi-hourglass-split"></i>Pending
                                            </span>
                                        </c:when>
                                        <c:when test="${lr.status == 'Approved'}">
                                            <span class="status-pill status-approved">
                                                <i class="bi bi-check-circle-fill"></i>Approved
                                            </span>
                                        </c:when>
                                        <c:when test="${lr.status == 'Rejected'}">
                                            <span class="status-pill status-rejected">
                                                <i class="bi bi-x-circle-fill"></i>Rejected
                                            </span>
                                        </c:when>
                                        <c:when test="${lr.status == 'Cancelled'}">
                                            <span class="status-pill status-cancelled">
                                                <i class="bi bi-slash-circle-fill"></i>Cancelled
                                            </span>
                                        </c:when>
                                        <c:otherwise>${lr.status}</c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end pe-4">
                                    <c:if test="${permissions.contains('VIEW_LEAVE_REQUEST_DETAIL')}">
                                        <a href="${pageContext.request.contextPath}/leave-requests?action=detail&id=${lr.leaveRequestId}"
                                           class="btn btn-sm btn-outline-primary">
                                            <i class="bi bi-eye me-1"></i>View
                                        </a>
                                    </c:if>
                                    <c:if test="${permissions.contains('APPROVE_REJECT_LEAVE_REQUEST') && lr.status == 'Pending'}">
                                        <form method="post" action="${pageContext.request.contextPath}/leave-requests?action=approve"
                                              class="d-inline"
                                              onsubmit="return confirm('Approve leave request from ${lr.employeeFullName}?')">
                                            <input type="hidden" name="id" value="${lr.leaveRequestId}">
                                            <button type="submit" class="btn btn-sm btn-outline-success">
                                                <i class="bi bi-check-lg me-1"></i>Approve
                                            </button>
                                        </form>
                                        <button type="button" class="btn btn-sm btn-outline-danger"
                                                data-bs-toggle="modal"
                                                data-bs-target="#rejectModal${lr.leaveRequestId}">
                                            <i class="bi bi-x-lg me-1"></i>Reject
                                        </button>

                                        <div class="modal fade" id="rejectModal${lr.leaveRequestId}" tabindex="-1" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content text-start">
                                                    <form method="post" action="${pageContext.request.contextPath}/leave-requests?action=reject">
                                                        <div class="modal-header">
                                                            <h6 class="modal-title fw-bold">Reject Leave Request</h6>
                                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                        </div>
                                                        <div class="modal-body">
                                                            <input type="hidden" name="id" value="${lr.leaveRequestId}">
                                                            <p class="small text-muted mb-2">
                                                                Reject leave request from <strong>${lr.employeeFullName}</strong>
                                                                (${lr.startDate} to ${lr.endDate}).
                                                            </p>
                                                            <label class="form-label small fw-medium">Reject Reason</label>
                                                            <textarea name="managerNote" class="form-control" rows="3"
                                                                      maxlength="500" required
                                                                      placeholder="Enter reason for rejection..."></textarea>
                                                            <div class="form-text">Required, max 500 characters.</div>
                                                        </div>
                                                        <div class="modal-footer">
                                                            <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                                                            <button type="submit" class="btn btn-danger btn-sm">
                                                                <i class="bi bi-x-circle me-1"></i>Reject
                                                            </button>
                                                        </div>
                                                    </form>
                                                </div>
                                            </div>
                                        </div>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty requests}">
                            <tr>
                                <td colspan="9" class="text-center text-muted py-5">
                                    <i class="bi bi-inbox fs-2 d-block mb-2 opacity-25"></i>
                                    No leave requests found for this filter.
                                </td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            const q = this.value.toLowerCase();
            document.querySelectorAll('#leaveTable tbody tr').forEach(row => {
                row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
            });
        });
    }
</script>
</body>
</html>

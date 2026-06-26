<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="leaveStatus" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Leave Requests &mdash; HRM System</title>
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
        .stat-card {
            border: none; border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
        }
        .stat-icon {
            width: 42px; height: 42px; border-radius: 10px;
            display: flex; align-items: center; justify-content: center;
            font-size: 1.1rem;
        }
        .status-pill {
            padding: 4px 12px; border-radius: 20px;
            font-size: 0.75rem; font-weight: 600; display: inline-flex;
            align-items: center; gap: 0.35rem;
        }
        .status-pending   { background:#fff8e1; color:#a16207; }
        .status-approved  { background:#e6f9f0; color:#166534; }
        .status-rejected  { background:#fee2e2; color:#b91c1c; }
        .status-cancelled { background:#e5e7eb; color:#4b5563; }
        .filter-btn {
            border: 1px solid #e5e7eb; background: white;
            border-radius: 20px; padding: 4px 14px; font-size: 0.82rem;
            color: #4b5563; cursor: pointer; transition: all 0.15s;
        }
        .filter-btn:hover { border-color: #2d6a9f; color: #1a3c5e; }
        .filter-btn.active {
            background: #1a3c5e; color: white; border-color: #1a3c5e;
        }
        .reason-cell {
            max-width: 280px; overflow: hidden;
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
            <h5 class="fw-bold text-dark mb-0">My Leave Requests</h5>
            <small class="text-muted">View approval status of your leave requests</small>
        </div>
        <c:if test="${permissions.contains('SUBMIT_LEAVE_REQUEST')}">
            <a href="${pageContext.request.contextPath}/leave-requests"
               class="btn btn-primary btn-sm px-3 fw-medium"
               style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                <i class="bi bi-calendar-plus me-2"></i>New Request
            </a>
        </c:if>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-warning d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-triangle-fill"></i><span>${error}</span>
        </div>
    </c:if>

    <div class="row g-3 mb-4">
        <div class="col-sm-6 col-xl-3">
            <div class="stat-card card p-3">
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
        </div>
        <div class="col-sm-6 col-xl-3">
            <div class="stat-card card p-3">
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
        </div>
        <div class="col-sm-6 col-xl-3">
            <div class="stat-card card p-3">
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
        </div>
        <div class="col-sm-6 col-xl-3">
            <div class="stat-card card p-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="stat-icon" style="background:#e5e7eb;color:#4b5563;">
                        <i class="bi bi-slash-circle"></i>
                    </div>
                    <div>
                        <div class="text-muted small">Cancelled</div>
                        <div class="fw-bold fs-5">${countCancelled}</div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="p-3 border-bottom d-flex align-items-center gap-2 flex-wrap">
                <div class="d-flex align-items-center gap-2 flex-grow-1">
                    <i class="bi bi-search text-muted"></i>
                    <input type="text" id="searchInput" class="form-control form-control-sm border-0 shadow-none"
                           placeholder="Search by type, date, reason..." style="max-width:320px;">
                </div>
                <div class="d-flex gap-2 flex-wrap">
                    <button type="button" class="filter-btn active" data-filter="all">All</button>
                    <button type="button" class="filter-btn" data-filter="Pending">Pending</button>
                    <button type="button" class="filter-btn" data-filter="Approved">Approved</button>
                    <button type="button" class="filter-btn" data-filter="Rejected">Rejected</button>
                    <button type="button" class="filter-btn" data-filter="Cancelled">Cancelled</button>
                </div>
            </div>

            <div class="table-responsive">
                <table class="table table-hover mb-0" id="leaveTable">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">#</th>
                            <th>Type</th>
                            <th>Period</th>
                            <th>Days</th>
                            <th>Reason</th>
                            <th>Submitted</th>
                            <th>Status</th>
                            <th>Reviewed</th>
                            <th class="text-end pe-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="lr" items="${requests}" varStatus="s">
                            <tr data-status="${lr.status}">
                                <td class="ps-4 text-muted">${s.index + 1}</td>
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
                                <td>
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
                                <td class="pe-4 small">
                                    <c:choose>
                                        <c:when test="${not empty lr.approvedAt}">
                                            <div class="text-muted">${lr.approvedAt}</div>
                                            <c:if test="${not empty lr.managerNote}">
                                                <div class="text-secondary fst-italic" style="font-size:0.78rem;">
                                                    <i class="bi bi-chat-left-text me-1"></i>${lr.managerNote}
                                                </div>
                                            </c:if>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted">&mdash;</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end pe-4">
                                    <c:if test="${permissions.contains('VIEW_LEAVE_REQUEST_DETAIL')}">
                                        <a href="${pageContext.request.contextPath}/leave-requests?action=detail&id=${lr.leaveRequestId}"
                                           class="btn btn-sm btn-outline-primary">
                                            <i class="bi bi-eye me-1"></i>View
                                        </a>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty requests}">
                            <tr id="emptyRow">
                                <td colspan="9" class="text-center text-muted py-5">
                                    <i class="bi bi-calendar-x fs-2 d-block mb-2 opacity-25"></i>
                                    You have not submitted any leave request yet.
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
    const filterBtns  = document.querySelectorAll('.filter-btn');
    const tableBody   = document.querySelector('#leaveTable tbody');
    let currentFilter = 'all';

    function applyFilter() {
        if (!tableBody) return;
        const q = (searchInput?.value || '').toLowerCase();
        tableBody.querySelectorAll('tr[data-status]').forEach(row => {
            const matchesText   = row.textContent.toLowerCase().includes(q);
            const matchesStatus = currentFilter === 'all' || row.dataset.status === currentFilter;
            row.style.display = (matchesText && matchesStatus) ? '' : 'none';
        });
    }

    if (searchInput) searchInput.addEventListener('input', applyFilter);
    filterBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            filterBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            currentFilter = btn.dataset.filter;
            applyFilter();
        });
    });
</script>
</body>
</html>

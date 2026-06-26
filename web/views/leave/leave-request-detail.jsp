<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="${backTo == 'status' ? 'leaveStatus' : 'leaveList'}" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Leave Request Detail &mdash; HRM System</title>
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
        .avatar {
            width: 72px; height: 72px; border-radius: 50%;
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            display: flex; align-items: center; justify-content: center;
            font-size: 1.6rem; font-weight: 700; color: white;
        }
        .status-pill {
            padding: 5px 14px; border-radius: 20px;
            font-size: 0.82rem; font-weight: 600;
            display: inline-flex; align-items: center; gap: 0.4rem;
        }
        .status-pending   { background:#fff8e1; color:#a16207; }
        .status-approved  { background:#e6f9f0; color:#166534; }
        .status-rejected  { background:#fee2e2; color:#b91c1c; }
        .status-cancelled { background:#e5e7eb; color:#4b5563; }
        .leave-banner {
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            color: white; border-radius: 14px; padding: 1.5rem;
        }
        .reason-box {
            background: #f8fafc; border: 1px solid #e9ecef;
            border-radius: 10px; padding: 1rem; white-space: pre-wrap;
            font-size: 0.92rem; color: #334155;
        }
        .note-box {
            background: #f0f9ff; border: 1px solid #bae6fd;
            border-radius: 10px; padding: 1rem; white-space: pre-wrap;
            font-size: 0.9rem; color: #0369a1;
        }
        .type-badge {
            background: #e3f0fb; color: #1a3c5e;
            padding: 5px 14px; border-radius: 20px;
            font-size: 0.8rem; font-weight: 600;
            display: inline-flex; align-items: center; gap: 0.4rem;
        }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center gap-2 mb-4">
        <a href="${pageContext.request.contextPath}/leave-requests?action=${backTo}"
           class="btn btn-sm btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Back
        </a>
        <div>
            <h5 class="fw-bold text-dark mb-0">Leave Request Detail</h5>
            <small class="text-muted">Request #${lr.leaveRequestId} &mdash; submitted by ${lr.employeeFullName}</small>
        </div>
    </div>

    <c:if test="${param.result == 'approved'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>Leave request approved successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.result == 'rejected'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>Leave request rejected successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.error == 'not-pending'}">
        <div class="alert alert-warning d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-triangle-fill"></i><span>Only pending leave requests can be approved or rejected.</span>
        </div>
    </c:if>
    <c:if test="${param.error == 'reject-note-required'}">
        <div class="alert alert-warning d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-triangle-fill"></i><span>Please enter a reason before rejecting this request.</span>
        </div>
    </c:if>

    <div class="leave-banner d-flex align-items-center justify-content-between flex-wrap gap-3 mb-4">
        <div class="d-flex align-items-center gap-3">
            <div class="bg-white bg-opacity-25 rounded-3 d-flex align-items-center justify-content-center"
                 style="width:48px;height:48px;">
                <i class="bi bi-calendar-event fs-4"></i>
            </div>
            <div>
                <div class="opacity-75 small">Leave Period</div>
                <div class="fw-bold fs-5">${lr.startDate} &rarr; ${lr.endDate}</div>
                <div class="opacity-75 small">Total: <strong>${lr.totalDays}</strong> day(s)</div>
            </div>
        </div>
        <div>
            <c:choose>
                <c:when test="${lr.status == 'Pending'}">
                    <span class="status-pill status-pending">
                        <i class="bi bi-hourglass-split"></i>Pending Approval
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
            </c:choose>
        </div>
    </div>

    <div class="row g-4">
        <div class="col-lg-4">
            <div class="card border-0 shadow-sm rounded-3 p-4 text-center">
                <div class="d-flex justify-content-center mb-3">
                    <div class="avatar">${fn:substring(lr.employeeFullName, 0, 1)}</div>
                </div>
                <h6 class="fw-bold mb-1">${lr.employeeFullName}</h6>
                <p class="text-muted small mb-2">${lr.employeeCode}</p>
                <div class="text-start small text-muted mt-3">
                    <c:if test="${not empty lr.employeeDepartment}">
                        <div class="mb-2">
                            <i class="bi bi-building me-2"></i>${lr.employeeDepartment}
                        </div>
                    </c:if>
                    <c:if test="${not empty lr.employeeEmail}">
                        <div class="mb-2">
                            <i class="bi bi-envelope me-2"></i>${lr.employeeEmail}
                        </div>
                    </c:if>
                    <c:if test="${not empty lr.employeePhone}">
                        <div>
                            <i class="bi bi-telephone me-2"></i>${lr.employeePhone}
                        </div>
                    </c:if>
                </div>
            </div>
        </div>

        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-3 p-4">
                <h6 class="fw-semibold mb-3 text-secondary">
                    <i class="bi bi-file-earmark-text me-2"></i>Request Information
                </h6>

                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Leave Type</div>
                    <div class="col-8">
                        <c:choose>
                            <c:when test="${lr.leaveType == 'AnnualLeave'}">
                                <span class="type-badge"><i class="bi bi-sun"></i>Annual Leave</span>
                            </c:when>
                            <c:when test="${lr.leaveType == 'SickLeave'}">
                                <span class="type-badge"><i class="bi bi-bandaid"></i>Sick Leave</span>
                            </c:when>
                            <c:when test="${lr.leaveType == 'PersonalLeave'}">
                                <span class="type-badge"><i class="bi bi-person"></i>Personal Leave</span>
                            </c:when>
                            <c:when test="${lr.leaveType == 'UnpaidLeave'}">
                                <span class="type-badge"><i class="bi bi-cash-stack"></i>Unpaid Leave</span>
                            </c:when>
                            <c:otherwise>${lr.leaveType}</c:otherwise>
                        </c:choose>
                    </div>
                </div>
                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Start Date</div>
                    <div class="col-8 fw-medium">${lr.startDate}</div>
                </div>
                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">End Date</div>
                    <div class="col-8 fw-medium">${lr.endDate}</div>
                </div>
                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Total Days</div>
                    <div class="col-8 fw-medium">${lr.totalDays}</div>
                </div>
                <div class="info-row row">
                    <div class="col-4 text-muted small pt-1">Reason</div>
                    <div class="col-8">
                        <div class="reason-box">${lr.reason}</div>
                    </div>
                </div>
                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Submitted At</div>
                    <div class="col-8">${lr.createdAt}</div>
                </div>
                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Last Updated</div>
                    <div class="col-8">${lr.updatedAt}</div>
                </div>
            </div>

            <div class="card border-0 shadow-sm rounded-3 p-4 mt-3">
                <h6 class="fw-semibold mb-3 text-secondary">
                    <i class="bi bi-clipboard-check me-2"></i>Review Information
                </h6>

                <c:choose>
                    <c:when test="${lr.status == 'Pending'}">
                        <div class="text-center text-muted py-3">
                            <i class="bi bi-hourglass-split fs-3 d-block mb-2 opacity-50"></i>
                            This request is awaiting manager review.
                        </div>

                        <c:if test="${permissions.contains('APPROVE_REJECT_LEAVE_REQUEST') && backTo == 'list'}">
                            <hr>
                            <form method="post" action="${pageContext.request.contextPath}/leave-requests?action=approve"
                                  class="mb-3"
                                  onsubmit="return confirm('Approve this leave request?')">
                                <input type="hidden" name="id" value="${lr.leaveRequestId}">
                                <label class="form-label small text-muted">Manager Note (optional)</label>
                                <textarea name="managerNote" class="form-control mb-2" rows="2"
                                          maxlength="500" placeholder="Optional note..."></textarea>
                                <button type="submit" class="btn btn-success btn-sm px-4">
                                    <i class="bi bi-check-circle me-1"></i>Approve
                                </button>
                            </form>

                            <form method="post" action="${pageContext.request.contextPath}/leave-requests?action=reject"
                                  onsubmit="return confirm('Reject this leave request?')">
                                <input type="hidden" name="id" value="${lr.leaveRequestId}">
                                <label class="form-label small text-muted">Reject Reason</label>
                                <textarea name="managerNote" class="form-control mb-2" rows="2"
                                          maxlength="500" placeholder="Enter reason for rejection..." required></textarea>
                                <button type="submit" class="btn btn-outline-danger btn-sm px-4">
                                    <i class="bi bi-x-circle me-1"></i>Reject
                                </button>
                            </form>
                        </c:if>
                    </c:when>
                    <c:otherwise>
                        <div class="info-row row align-items-center">
                            <div class="col-4 text-muted small">Reviewed By</div>
                            <div class="col-8">
                                <c:choose>
                                    <c:when test="${not empty lr.approverFullName}">${lr.approverFullName}</c:when>
                                    <c:otherwise><span class="text-muted">&mdash;</span></c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                        <div class="info-row row align-items-center">
                            <div class="col-4 text-muted small">Reviewed At</div>
                            <div class="col-8">
                                <c:choose>
                                    <c:when test="${not empty lr.approvedAt}">${lr.approvedAt}</c:when>
                                    <c:otherwise><span class="text-muted">&mdash;</span></c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                        <div class="info-row row">
                            <div class="col-4 text-muted small pt-1">Manager Note</div>
                            <div class="col-8">
                                <c:choose>
                                    <c:when test="${not empty lr.managerNote}">
                                        <div class="note-box">
                                            <i class="bi bi-chat-left-text me-1"></i>${lr.managerNote}
                                        </div>
                                    </c:when>
                                    <c:otherwise><span class="text-muted">&mdash;</span></c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

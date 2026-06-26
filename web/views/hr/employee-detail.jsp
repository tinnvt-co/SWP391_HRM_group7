<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="hrEmployees" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Employee Detail &mdash; HRM System</title>
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
        .avatar {
            width: 88px; height: 88px; border-radius: 50%;
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            display: flex; align-items: center; justify-content: center;
            font-size: 2rem; font-weight: 700; color: white;
        }
        .info-row { padding: 0.65rem 0; border-bottom: 1px solid #f1f3f5; }
        .info-row:last-child { border-bottom: none; }
        .status-pill { padding: 5px 14px; border-radius: 20px; font-size: 0.8rem; font-weight: 600; }
        .status-working   { background:#e6f9f0; color:#166534; }
        .status-probation { background:#fff8e1; color:#a16207; }
        .status-resigned  { background:#fee2e2; color:#b91c1c; }
        .status-suspended { background:#e5e7eb; color:#4b5563; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center gap-2 mb-4">
        <a href="${pageContext.request.contextPath}/hr/employees" class="btn btn-sm btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Back
        </a>
        <div>
            <h5 class="fw-bold text-dark mb-0">Employee Detail</h5>
            <small class="text-muted">Viewing information for ${employee.fullName}</small>
        </div>
    </div>

    <c:if test="${param.statusUpdated == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>Employment status updated successfully.</span>
        </div>
    </c:if>
    <c:if test="${param.error == 'invalid-status'}">
        <div class="alert alert-danger d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-circle-fill"></i><span>Invalid employment status.</span>
        </div>
    </c:if>

    <div class="row g-4">
        <div class="col-lg-4">
            <div class="card border-0 shadow-sm rounded-3 p-4 text-center">
                <div class="d-flex justify-content-center mb-3">
                    <div class="avatar">${fn:substring(employee.fullName, 0, 1)}</div>
                </div>
                <h5 class="fw-bold mb-1">${employee.fullName}</h5>
                <p class="text-muted small mb-3">@${employee.username}</p>
                <div class="mb-2">
                    <c:choose>
                        <c:when test="${employee.employmentStatus == 'Working'}">
                            <span class="status-pill status-working">Working</span>
                        </c:when>
                        <c:when test="${employee.employmentStatus == 'Probation'}">
                            <span class="status-pill status-probation">Probation</span>
                        </c:when>
                        <c:when test="${employee.employmentStatus == 'Resigned'}">
                            <span class="status-pill status-resigned">Resigned</span>
                        </c:when>
                        <c:otherwise>
                            <span class="status-pill status-suspended">Suspended</span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <hr>
                <div class="text-start small text-muted">
                    <div class="mb-1"><i class="bi bi-hash me-2"></i>${employee.employeeCode}</div>
                    <div class="mb-1"><i class="bi bi-building me-2"></i>${employee.departmentName}</div>
                    <div><i class="bi bi-calendar-event me-2"></i>Hired: ${employee.hireDate}</div>
                </div>
            </div>

            <c:if test="${permissions.contains('UPDATE_EMPLOYMENT_STATUS')}">
                <div class="card border-0 shadow-sm rounded-3 p-4 mt-3">
                    <h6 class="fw-semibold mb-3 text-secondary">
                        <i class="bi bi-pencil-square me-2"></i>Update Employment Status
                    </h6>
                    <form action="${pageContext.request.contextPath}/hr/employees?action=updateStatus"
                          method="post"
                          onsubmit="return confirm('Update employment status for ${employee.fullName}?')">
                        <input type="hidden" name="employeeId" value="${employee.employeeId}">
                        <select name="employmentStatus" class="form-select mb-3">
                            <c:forEach var="st" items="${statuses}">
                                <option value="${st}" ${employee.employmentStatus == st ? 'selected' : ''}>${st}</option>
                            </c:forEach>
                        </select>
                        <button type="submit" class="btn btn-primary w-100 fw-medium"
                                style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                            <i class="bi bi-check-lg me-2"></i>Save Status
                        </button>
                    </form>
                </div>
            </c:if>
        </div>

        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-3 p-4">
                <h6 class="fw-semibold mb-3 text-secondary">
                    <i class="bi bi-person-lines-fill me-2"></i>Personal Information
                </h6>
                <div class="info-row row">
                    <div class="col-4 text-muted small">Full Name</div>
                    <div class="col-8 fw-medium">${employee.fullName}</div>
                </div>
                <div class="info-row row">
                    <div class="col-4 text-muted small">Username</div>
                    <div class="col-8">${employee.username}</div>
                </div>
                <div class="info-row row">
                    <div class="col-4 text-muted small">Email</div>
                    <div class="col-8">${employee.email}</div>
                </div>
                <div class="info-row row">
                    <div class="col-4 text-muted small">Phone</div>
                    <div class="col-8">${not empty employee.phone ? employee.phone : '—'}</div>
                </div>
                <div class="info-row row">
                    <div class="col-4 text-muted small">Gender</div>
                    <div class="col-8">${not empty employee.gender ? employee.gender : '—'}</div>
                </div>
                <div class="info-row row">
                    <div class="col-4 text-muted small">Date of Birth</div>
                    <div class="col-8">${not empty employee.dateOfBirth ? employee.dateOfBirth : '—'}</div>
                </div>
                <div class="info-row row">
                    <div class="col-4 text-muted small">Address</div>
                    <div class="col-8">${not empty employee.address ? employee.address : '—'}</div>
                </div>
            </div>

            <div class="card border-0 shadow-sm rounded-3 p-4 mt-3">
                <h6 class="fw-semibold mb-3 text-secondary">
                    <i class="bi bi-briefcase me-2"></i>Employment Information
                </h6>
                <div class="info-row row">
                    <div class="col-4 text-muted small">Employee Code</div>
                    <div class="col-8 fw-medium">${employee.employeeCode}</div>
                </div>
                <div class="info-row row">
                    <div class="col-4 text-muted small">Department</div>
                    <div class="col-8">${employee.departmentName}</div>
                </div>
                <div class="info-row row">
                    <div class="col-4 text-muted small">Hire Date</div>
                    <div class="col-8">${employee.hireDate}</div>
                </div>
                <div class="info-row row">
                    <div class="col-4 text-muted small">Employment Status</div>
                    <div class="col-8">${employee.employmentStatus}</div>
                </div>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

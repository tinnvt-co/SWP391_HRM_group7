<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="profile" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Profile &mdash; HRM System</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body { background-color: #f4f6f9; }
        .sidebar {
            width: 240px; min-height: calc(100vh - 56px);
            background-color: #1a3c5e; position: fixed;
            top: 56px; left: 0; padding-top: 1rem; z-index: 100;
        }
        .sidebar .nav-link {
            color: rgba(255,255,255,0.75); padding: 0.6rem 1.25rem;
            border-radius: 6px; margin: 2px 10px; font-size: 0.9rem; transition: all 0.2s;
        }
        .sidebar .nav-link:hover, .sidebar .nav-link.active {
            color: #fff; background-color: rgba(255,255,255,0.12);
        }
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
        .badge-role {
            background-color: #e3f0fb; color: #1a3c5e;
            padding: 5px 14px; border-radius: 20px; font-size: 0.8rem; font-weight: 600;
        }
        .badge-active {
            background-color: #e6f9f0; color: #166534;
            padding: 5px 14px; border-radius: 20px; font-size: 0.8rem; font-weight: 600;
        }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="mb-4">
        <h5 class="fw-bold text-dark mb-0">My Profile</h5>
        <small class="text-muted">View your personal information</small>
    </div>

    <div class="row g-4">
        <div class="col-lg-4">
            <div class="card border-0 shadow-sm rounded-3 p-4 text-center">
                <div class="d-flex justify-content-center mb-3">
                    <div class="avatar">${fn:substring(user.fullName, 0, 1)}</div>
                </div>
                <h5 class="fw-bold mb-1">${user.fullName}</h5>
                <p class="text-muted small mb-3">${user.email}</p>
                <div class="d-flex justify-content-center gap-2 flex-wrap">
                    <span class="badge-role">${user.role.roleName}</span>
                    <c:choose>
                        <c:when test="${user.active}">
                            <span class="badge-active"><i class="bi bi-circle-fill me-1" style="font-size:0.5rem;"></i>Active</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-secondary">Inactive</span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <hr>
                <a href="${pageContext.request.contextPath}/change-password"
                   class="btn btn-outline-primary btn-sm w-100 mb-2">
                    <i class="bi bi-key me-2"></i>Change Password
                </a>
                <c:if test="${canManageBankAccount}">
                    <a href="${pageContext.request.contextPath}/bank-account"
                       class="btn btn-outline-secondary btn-sm w-100">
                        <i class="bi bi-bank me-2"></i>Manage Bank Account
                    </a>
                </c:if>
            </div>
        </div>

        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-3 p-4">
                <h6 class="fw-semibold mb-3 text-secondary">
                    <i class="bi bi-person-lines-fill me-2"></i>Personal Information
                </h6>

                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Full Name</div>
                    <div class="col-8 fw-medium">${user.fullName}</div>
                </div>
                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Username</div>
                    <div class="col-8">${user.username}</div>
                </div>
                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Email</div>
                    <div class="col-8">${user.email}</div>
                </div>
                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Phone</div>
                    <div class="col-8">${not empty user.phone ? user.phone : '—'}</div>
                </div>
                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Gender</div>
                    <div class="col-8">${not empty user.gender ? user.gender : '—'}</div>
                </div>
                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Date of Birth</div>
                    <div class="col-8">${not empty user.dateOfBirth ? user.dateOfBirth : '—'}</div>
                </div>
                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Address</div>
                    <div class="col-8">${not empty user.address ? user.address : '—'}</div>
                </div>
            </div>

            <div class="card border-0 shadow-sm rounded-3 p-4 mt-3">
                <h6 class="fw-semibold mb-3 text-secondary">
                    <i class="bi bi-briefcase me-2"></i>Employment Information
                </h6>
                <c:choose>
                    <c:when test="${not empty employee}">
                        <div class="info-row row align-items-center">
                            <div class="col-4 text-muted small">Employee Code</div>
                            <div class="col-8 fw-medium">${employee.employeeCode}</div>
                        </div>
                        <div class="info-row row align-items-center">
                            <div class="col-4 text-muted small">Department</div>
                            <div class="col-8">${not empty employee.departmentName ? employee.departmentName : '—'}</div>
                        </div>
                        <div class="info-row row align-items-center">
                            <div class="col-4 text-muted small">Role</div>
                            <div class="col-8"><span class="badge-role">${user.role.roleName}</span></div>
                        </div>
                        <div class="info-row row align-items-center">
                            <div class="col-4 text-muted small">Employment Status</div>
                            <div class="col-8">${not empty employee.employmentStatus ? employee.employmentStatus : '—'}</div>
                        </div>
                        <div class="info-row row align-items-center">
                            <div class="col-4 text-muted small">Hire Date</div>
                            <div class="col-8">${not empty employee.hireDate ? employee.hireDate : '—'}</div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="text-muted small">
                            <i class="bi bi-info-circle me-1"></i>No employment record linked to this account.
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <c:if test="${canManageBankAccount}">
                <div class="card border-0 shadow-sm rounded-3 p-4 mt-3">
                    <div class="d-flex align-items-center justify-content-between mb-3">
                        <h6 class="fw-semibold mb-0 text-secondary">
                            <i class="bi bi-bank me-2"></i>Bank Account
                        </h6>
                        <a href="${pageContext.request.contextPath}/bank-account"
                           class="btn btn-sm btn-outline-primary">
                            <i class="bi bi-pencil me-1"></i>Manage
                        </a>
                    </div>
                    <c:choose>
                        <c:when test="${not empty employee.bankAccountNumber}">
                            <div class="info-row row align-items-center">
                                <div class="col-4 text-muted small">Bank Name</div>
                                <div class="col-8 fw-medium">${employee.bankName}</div>
                            </div>
                            <div class="info-row row align-items-center">
                                <div class="col-4 text-muted small">Account Number</div>
                                <div class="col-8">${employee.bankAccountNumber}</div>
                            </div>
                            <div class="info-row row align-items-center">
                                <div class="col-4 text-muted small">Branch</div>
                                <div class="col-8">${not empty employee.bankBranch ? employee.bankBranch : '—'}</div>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="alert alert-warning d-flex align-items-center gap-2 py-2 mb-0">
                                <i class="bi bi-exclamation-triangle-fill"></i>
                                <span>No bank account added yet. Please add one to receive your salary.</span>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:if>

            <div class="card border-0 shadow-sm rounded-3 p-4 mt-3">
                <h6 class="fw-semibold mb-3 text-secondary">
                    <i class="bi bi-clock-history me-2"></i>Account Activity
                </h6>
                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Last Login</div>
                    <div class="col-8">
                        <c:choose>
                            <c:when test="${not empty user.lastLogin}">${user.lastLogin}</c:when>
                            <c:otherwise>—</c:otherwise>
                        </c:choose>
                    </div>
                </div>
                <div class="info-row row align-items-center">
                    <div class="col-4 text-muted small">Member Since</div>
                    <div class="col-8">${not empty user.createdAt ? user.createdAt : '—'}</div>
                </div>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

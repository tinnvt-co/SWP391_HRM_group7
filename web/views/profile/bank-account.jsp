<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="profile" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Bank Account &mdash; HRM System</title>
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
        .form-label { font-weight: 500; font-size: 0.9rem; }
        .required::after { content: ' *'; color: #dc3545; }
        .form-control:focus {
            border-color: #2d6a9f; box-shadow: 0 0 0 0.2rem rgba(45,106,159,0.2);
        }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center gap-2 mb-4">
        <a href="${pageContext.request.contextPath}/profile" class="btn btn-sm btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Back
        </a>
        <div>
            <h5 class="fw-bold text-dark mb-0">Manage Bank Account</h5>
            <small class="text-muted">Bank details used to receive your salary</small>
        </div>
    </div>

    <div class="row justify-content-center">
        <div class="col-lg-7">

            <c:if test="${param.saved == 'success'}">
                <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
                    <i class="bi bi-check-circle-fill"></i><span>Bank account saved successfully.</span>
                </div>
            </c:if>

            <c:if test="${not empty error}">
                <div class="alert alert-danger d-flex align-items-center gap-2 py-2 mb-3">
                    <i class="bi bi-exclamation-circle-fill"></i><span>${error}</span>
                </div>
            </c:if>

            <c:choose>
                <c:when test="${empty employee}">
                    <div class="card border-0 shadow-sm rounded-3 p-4">
                        <div class="text-center text-muted py-4">
                            <i class="bi bi-exclamation-triangle fs-2 d-block mb-2 opacity-50"></i>
                            Your account is not linked to an employee record.<br>
                            Please contact HR before adding a bank account.
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <c:if test="${empty employee.bankAccountNumber}">
                        <div class="alert alert-warning d-flex align-items-center gap-2 py-2 mb-3">
                            <i class="bi bi-info-circle-fill"></i>
                            <span>You have not added a bank account yet. Add one below.</span>
                        </div>
                    </c:if>

                    <div class="card border-0 shadow-sm rounded-3 p-4">
                        <form action="${pageContext.request.contextPath}/bank-account" method="post" novalidate>
                            <div class="mb-3">
                                <label for="bankName" class="form-label required">Bank Name</label>
                                <input type="text" id="bankName" name="bankName" class="form-control"
                                       maxlength="150" placeholder="e.g. Vietcombank"
                                       value="${not empty employee.bankName ? employee.bankName : ''}" required>
                            </div>
                            <div class="mb-3">
                                <label for="bankAccountNumber" class="form-label required">Account Number</label>
                                <input type="text" id="bankAccountNumber" name="bankAccountNumber" class="form-control"
                                       inputmode="numeric" maxlength="30" pattern="[0-9]{6,30}"
                                       oninput="this.value=this.value.replace(/[^0-9]/g,'')"
                                       placeholder="Enter account number"
                                       value="${not empty employee.bankAccountNumber ? employee.bankAccountNumber : ''}" required>
                                <div class="form-text">Digits only, 6 to 30 characters.</div>
                            </div>
                            <div class="mb-4">
                                <label for="bankBranch" class="form-label">Branch</label>
                                <input type="text" id="bankBranch" name="bankBranch" class="form-control"
                                       maxlength="150" placeholder="e.g. Ho Chi Minh City"
                                       value="${not empty employee.bankBranch ? employee.bankBranch : ''}">
                            </div>

                            <div class="d-flex justify-content-end gap-2">
                                <a href="${pageContext.request.contextPath}/profile"
                                   class="btn btn-outline-secondary px-4">Cancel</a>
                                <button type="submit" class="btn btn-primary px-4 fw-medium"
                                        style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                                    <i class="bi bi-check-lg me-2"></i>Save Bank Account
                                </button>
                            </div>
                        </form>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="allowances" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Allowance &mdash; HRM System</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body { background-color: #f4f6f9; }
        .sidebar { width: 240px; min-height: calc(100vh - 56px); background-color: #1a3c5e;
            position: fixed; top: 56px; left: 0; padding-top: 1rem; z-index: 100; }
        .sidebar .nav-link { color: rgba(255,255,255,0.75); padding: 0.6rem 1.25rem;
            border-radius: 6px; margin: 2px 10px; font-size: 0.9rem; transition: all 0.2s; }
        .sidebar .nav-link:hover, .sidebar .nav-link.active { color: #fff; background-color: rgba(255,255,255,0.12); }
        .sidebar .nav-link i { width: 20px; }
        .sidebar-label { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 1px;
            color: rgba(255,255,255,0.4); padding: 0.75rem 1.25rem 0.25rem; }
        .main-content { margin-left: 240px; padding: 2rem; }
        .metric-card, .table-card { border: none; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.07); }
        .metric-icon {
            width: 44px; height: 44px; border-radius: 8px;
            display: flex; align-items: center; justify-content: center; font-size: 1.15rem;
        }
        .form-label { font-size: 0.82rem; color: #6b7280; font-weight: 600; }
        .btn-icon { width: 34px; height: 34px; padding: 0; display: inline-flex; align-items: center; justify-content: center; }
        .table td, .table th { vertical-align: middle; }
        .code-pill { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
            <h5 class="fw-bold text-dark mb-0">Manage Allowance</h5>
            <small class="text-muted">Global monthly allowance types used by payroll</small>
        </div>
        <button type="button" class="btn btn-primary"
                style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;"
                data-bs-toggle="modal" data-bs-target="#createAllowanceModal">
            <i class="bi bi-plus-lg me-2"></i>Add Allowance
        </button>
    </div>

    <c:if test="${not empty allowanceMessage}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i><span>${allowanceMessage}</span>
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-danger d-flex align-items-start gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-octagon-fill mt-1"></i><span>${error}</span>
        </div>
    </c:if>

    <div class="row g-3 mb-3">
        <div class="col-md-6 col-xl-4">
            <div class="metric-card card p-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="metric-icon" style="background:#e3f0fb;">
                        <i class="bi bi-wallet2 text-primary"></i>
                    </div>
                    <div>
                        <div class="text-muted small">Total Active Allowance</div>
                        <div class="fw-bold fs-5">
                            <fmt:formatNumber value="${totalActiveAllowance}" type="number" maxFractionDigits="0"/>
                            <span class="fs-6 fw-normal text-muted">&#8363;</span>
                        </div>
                        <div class="text-muted" style="font-size:0.78rem;">Added to each payroll line</div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-md-6 col-xl-4">
            <div class="metric-card card p-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="metric-icon" style="background:#e6f9f0;">
                        <i class="bi bi-toggle2-on text-success"></i>
                    </div>
                    <div>
                        <div class="text-muted small">Allowance Types</div>
                        <div class="fw-bold fs-5">${fn:length(allowanceTypes)}</div>
                        <div class="text-muted" style="font-size:0.78rem;">Active and inactive policies</div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="table-card card">
        <div class="table-responsive">
            <table class="table table-hover mb-0">
                <thead class="table-light">
                <tr>
                    <th style="width: 16%;">Code</th>
                    <th>Name</th>
                    <th style="width: 18%;" class="text-end">Amount</th>
                    <th style="width: 12%;">Status</th>
                    <th style="width: 22%;">Description</th>
                    <th style="width: 12%;" class="text-end">Actions</th>
                </tr>
                </thead>
                <tbody>
                <c:choose>
                    <c:when test="${empty allowanceTypes}">
                        <tr>
                            <td colspan="6" class="text-center text-muted py-5">
                                <i class="bi bi-wallet2 d-block fs-3 mb-2"></i>
                                No allowance types found.
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="a" items="${allowanceTypes}">
                            <tr>
                                <td><span class="badge text-bg-light code-pill">${a.allowanceCode}</span></td>
                                <td class="fw-semibold">${a.allowanceName}</td>
                                <td class="text-end">
                                    <fmt:formatNumber value="${a.amount}" type="number" maxFractionDigits="0"/>
                                    <span class="text-muted">&#8363;</span>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.active}">
                                            <span class="badge text-bg-success">Active</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge text-bg-secondary">Inactive</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-muted small">${not empty a.description ? a.description : '-'}</td>
                                <td class="text-end">
                                    <div class="d-inline-flex gap-1">
                                        <button type="button" class="btn btn-sm btn-outline-primary btn-icon"
                                                data-bs-toggle="modal"
                                                data-bs-target="#editAllowanceModal${a.allowanceTypeId}"
                                                title="Edit">
                                            <i class="bi bi-pencil"></i>
                                        </button>
                                        <form method="post" action="${pageContext.request.contextPath}/allowances"
                                              onsubmit="return confirm('${a.active ? 'Deactivate' : 'Activate'} this allowance type?');">
                                            <input type="hidden" name="action" value="toggle">
                                            <input type="hidden" name="allowanceTypeId" value="${a.allowanceTypeId}">
                                            <input type="hidden" name="active" value="${!a.active}">
                                            <button type="submit"
                                                    class="btn btn-sm ${a.active ? 'btn-outline-warning' : 'btn-outline-success'} btn-icon"
                                                    title="${a.active ? 'Deactivate' : 'Activate'}">
                                                <i class="bi ${a.active ? 'bi-toggle2-off' : 'bi-toggle2-on'}"></i>
                                            </button>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
                </tbody>
            </table>
        </div>
    </div>
</div>

<div class="modal fade" id="createAllowanceModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog">
        <form class="modal-content" method="post" action="${pageContext.request.contextPath}/allowances">
            <input type="hidden" name="action" value="create">
            <div class="modal-header">
                <h5 class="modal-title">Add Allowance</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="mb-3">
                    <label for="allowanceCode" class="form-label">Code</label>
                    <input type="text" id="allowanceCode" name="allowanceCode" class="form-control"
                           maxlength="50" placeholder="HOUSING" required>
                    <div class="form-text">Letters, numbers, and underscores only.</div>
                </div>
                <div class="mb-3">
                    <label for="allowanceName" class="form-label">Name</label>
                    <input type="text" id="allowanceName" name="allowanceName" class="form-control"
                           maxlength="100" required>
                </div>
                <div class="mb-3">
                    <label for="amount" class="form-label">Amount</label>
                    <input type="number" id="amount" name="amount" class="form-control"
                           step="1000" min="0" value="0" required>
                </div>
                <div class="mb-0">
                    <label for="description" class="form-label">Description</label>
                    <textarea id="description" name="description" class="form-control"
                              rows="2" maxlength="255"></textarea>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                <button type="submit" class="btn btn-primary">Create</button>
            </div>
        </form>
    </div>
</div>

<c:forEach var="a" items="${allowanceTypes}">
    <div class="modal fade" id="editAllowanceModal${a.allowanceTypeId}" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <form class="modal-content" method="post" action="${pageContext.request.contextPath}/allowances">
                <input type="hidden" name="action" value="edit">
                <input type="hidden" name="allowanceTypeId" value="${a.allowanceTypeId}">
                <div class="modal-header">
                    <h5 class="modal-title">Edit ${a.allowanceCode}</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label">Code</label>
                        <input type="text" class="form-control bg-light" value="${a.allowanceCode}" disabled>
                    </div>
                    <div class="mb-3">
                        <label for="allowanceName${a.allowanceTypeId}" class="form-label">Name</label>
                        <input type="text" id="allowanceName${a.allowanceTypeId}" name="allowanceName"
                               class="form-control" maxlength="100" value="${a.allowanceName}" required>
                    </div>
                    <div class="mb-3">
                        <label for="amount${a.allowanceTypeId}" class="form-label">Amount</label>
                        <input type="number" id="amount${a.allowanceTypeId}" name="amount"
                               class="form-control" step="1000" min="0" value="${a.amount}" required>
                    </div>
                    <div class="mb-0">
                        <label for="description${a.allowanceTypeId}" class="form-label">Description</label>
                        <textarea id="description${a.allowanceTypeId}" name="description"
                                  class="form-control" rows="2" maxlength="255">${a.description}</textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">Save Changes</button>
                </div>
            </form>
        </div>
    </div>
</c:forEach>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

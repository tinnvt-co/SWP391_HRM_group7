<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
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
        .metric-card { border: none; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.07); }
        .metric-icon {
            width: 46px; height: 46px; border-radius: 12px;
            display: flex; align-items: center; justify-content: center; font-size: 1.2rem;
        }
        .form-card { border: none; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.07); }
        .form-label { font-size: 0.82rem; color: #6b7280; font-weight: 600; }
        .input-group-text { min-width: 48px; justify-content: center; background: #f8fafc; }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
            <h5 class="fw-bold text-dark mb-0">Manage Allowance</h5>
            <small class="text-muted">Global monthly allowance policy</small>
        </div>
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
        <div class="col-md-6">
            <div class="metric-card card p-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="metric-icon" style="background:#e3f0fb;">
                        <i class="bi bi-wallet2 text-primary"></i>
                    </div>
                    <div>
                        <div class="text-muted small">Total Allowance / Employee</div>
                        <div class="fw-bold fs-5">
                            <span id="totalPreview">
                                <fmt:formatNumber value="${settings.totalMonthlyAllowance}" type="number" maxFractionDigits="0"/>
                            </span>
                            <span class="fs-6 fw-normal text-muted">&#8363;</span>
                        </div>
                        <div class="text-muted" style="font-size:0.78rem;">Monthly amount</div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="metric-card card p-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="metric-icon" style="background:#e6f9f0;">
                        <i class="bi bi-people text-success"></i>
                    </div>
                    <div>
                        <div class="text-muted small">Applied Contracts</div>
                        <div class="fw-bold fs-5">${settings.activeContractCount}</div>
                        <div class="text-muted" style="font-size:0.78rem;">Active employee contracts</div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="form-card card">
        <div class="card-body p-4">
            <form method="post" action="${pageContext.request.contextPath}/allowances"
                  onsubmit="return confirm('Apply this allowance policy to all active employee contracts?');">
                <div class="row g-3">
                    <div class="col-md-6">
                        <label for="lunchAllowance" class="form-label">Lunch Allowance</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-cup-hot"></i></span>
                            <input type="number" step="1000" min="0" id="lunchAllowance"
                                   name="lunchAllowance" class="form-control allowance-input"
                                   value="${settings.lunchAllowance}" required>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <label for="transportationAllowance" class="form-label">Transportation Allowance</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-bus-front"></i></span>
                            <input type="number" step="1000" min="0" id="transportationAllowance"
                                   name="transportationAllowance" class="form-control allowance-input"
                                   value="${settings.transportationAllowance}" required>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <label for="phoneAllowance" class="form-label">Phone Allowance</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-phone"></i></span>
                            <input type="number" step="1000" min="0" id="phoneAllowance"
                                   name="phoneAllowance" class="form-control allowance-input"
                                   value="${settings.phoneAllowance}" required>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <label for="responsibilityAllowance" class="form-label">Responsibility Allowance</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-award"></i></span>
                            <input type="number" step="1000" min="0" id="responsibilityAllowance"
                                   name="responsibilityAllowance" class="form-control allowance-input"
                                   value="${settings.responsibilityAllowance}" required>
                        </div>
                    </div>
                </div>
                <div class="d-flex justify-content-end gap-2 mt-4">
                    <a href="${pageContext.request.contextPath}/home" class="btn btn-outline-secondary px-4">Cancel</a>
                    <button type="submit" class="btn btn-primary px-4"
                            style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                        <i class="bi bi-save me-2"></i>Save Global Allowance
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const totalPreview = document.getElementById('totalPreview');
    const inputs = document.querySelectorAll('.allowance-input');

    function updateTotalPreview() {
        let total = 0;
        inputs.forEach(input => {
            const value = Number(input.value || 0);
            if (!Number.isNaN(value)) total += value;
        });
        totalPreview.textContent = new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 }).format(total);
    }

    inputs.forEach(input => input.addEventListener('input', updateTotalPreview));
</script>
</body>
</html>

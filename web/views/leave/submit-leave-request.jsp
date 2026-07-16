<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="leaveSubmit" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Submit Leave Request &mdash; HRM System</title>
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
        .form-label { font-weight: 500; font-size: 0.9rem; }
        .required::after { content: ' *'; color: #dc3545; }
        .form-control:focus, .form-select:focus {
            border-color: #2d6a9f; box-shadow: 0 0 0 0.2rem rgba(45,106,159,0.2);
        }
        .section-title {
            font-size: 0.75rem; font-weight: 600; text-transform: uppercase;
            letter-spacing: 0.8px; color: #6b7280;
            padding-bottom: 0.5rem; border-bottom: 1px solid #e5e7eb; margin-bottom: 1.25rem;
        }
        .employee-card {
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            border-radius: 12px; color: white; padding: 1rem 1.25rem;
        }
        .day-counter {
            background: #f0f9ff; border: 1px solid #bae6fd;
            border-radius: 10px; padding: 0.75rem 1rem; color: #0369a1; font-size: 0.9rem;
        }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="mb-4">
        <h5 class="fw-bold text-dark mb-0">Submit Leave Request</h5>
        <small class="text-muted">Request time off for annual, sick, personal, maternity or unpaid leave</small>
    </div>

    <c:if test="${param.submitted == 'success'}">
        <div class="alert alert-success d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-check-circle-fill"></i>
            <span>Your leave request has been submitted and is pending approval.</span>
        </div>
    </c:if>

    <c:if test="${not empty error}">
        <div class="alert alert-danger d-flex align-items-center gap-2 py-2 mb-3">
            <i class="bi bi-exclamation-circle-fill"></i><span>${error}</span>
        </div>
    </c:if>

    <div class="row g-4">
        <c:if test="${not empty employee}">
            <div class="col-lg-4">
                <div class="employee-card mb-3">
                    <div class="d-flex align-items-center gap-3 mb-3">
                        <div class="bg-white bg-opacity-25 rounded-3 d-flex align-items-center justify-content-center"
                             style="width:48px;height:48px;">
                            <i class="bi bi-person-badge fs-4"></i>
                        </div>
                        <div>
                            <div class="fw-bold">${employee.fullName}</div>
                            <div class="opacity-75 small">${employee.employeeCode}</div>
                        </div>
                    </div>
                    <div class="small">
                        <div class="opacity-75">Department</div>
                        <div class="fw-medium mb-2">${employee.departmentName}</div>
                    </div>
                </div>

                <div class="card border-0 shadow-sm rounded-3 p-3">
                    <div class="small text-muted fw-medium mb-2">
                        <i class="bi bi-info-circle me-1"></i>Leave Types
                    </div>
                    <ul class="small text-muted ps-3 mb-0">
                        <li><strong>Annual Leave</strong> &mdash; paid yearly leave</li>
                        <li><strong>Sick Leave</strong> &mdash; medical-related absence</li>
                        <li><strong>Personal Leave</strong> &mdash; private matters</li>
                        <li><strong>Maternity Leave</strong> &mdash; social-insurance paid maternity absence</li>
                        <li><strong>Unpaid Leave</strong> &mdash; without salary</li>
                    </ul>
                </div>
            </div>
        </c:if>

        <div class="${not empty employee ? 'col-lg-8' : 'col-12'}">
            <div class="card border-0 shadow-sm rounded-3 p-4">

                <c:choose>
                    <c:when test="${empty employee}">
                        <div class="text-center text-muted py-4">
                            <i class="bi bi-exclamation-triangle fs-2 d-block mb-2 opacity-50"></i>
                            Your account is not linked to an employee record.<br>
                            Please contact HR to complete your profile before submitting a leave request.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <form action="${pageContext.request.contextPath}/leave-requests?action=submit"
                              method="post" id="leaveForm" novalidate>

                            <div class="section-title">Leave Details</div>
                            <div class="row g-3 mb-3">
                                <div class="col-12">
                                    <label for="leaveType" class="form-label required">Leave Type</label>
                                    <select id="leaveType" name="leaveType" class="form-select" required>
                                        <option value="">-- Select leave type --</option>
                                        <c:forEach var="t" items="${leaveTypes}">
                                            <option value="${t}"
                                                    ${param.leaveType == t.name() ? 'selected' : ''}>
                                                ${t.dbValue}
                                            </option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="col-md-6">
                                    <label for="startDate" class="form-label required">Start Date</label>
                                    <input type="date" id="startDate" name="startDate"
                                           class="form-control"
                                           value="${not empty param.startDate ? param.startDate : ''}"
                                           required>
                                </div>
                                <div class="col-md-6">
                                    <label for="endDate" class="form-label required">End Date</label>
                                    <input type="date" id="endDate" name="endDate"
                                           class="form-control"
                                           value="${not empty param.endDate ? param.endDate : ''}"
                                           required>
                                </div>
                                <div class="col-12">
                                    <div class="day-counter d-flex align-items-center gap-2" id="dayCounter" style="display:none;">
                                        <i class="bi bi-calendar-range"></i>
                                        <span>Total: <strong id="dayCount">0</strong> day(s)</span>
                                    </div>
                                </div>
                            </div>

                            <div class="section-title mt-4">Reason</div>
                            <div class="mb-4">
                                <label for="reason" class="form-label required">Reason</label>
                                <textarea id="reason" name="reason" class="form-control" rows="4"
                                          maxlength="500" placeholder="Briefly describe why you need this leave..."
                                          required>${not empty param.reason ? param.reason : ''}</textarea>
                                <div class="form-text d-flex justify-content-between">
                                    <span>Be clear and concise.</span>
                                    <span><span id="charCount">0</span>/500</span>
                                </div>
                            </div>

                            <div class="d-flex justify-content-end gap-2">
                                <a href="${pageContext.request.contextPath}/home"
                                   class="btn btn-outline-secondary px-4">Cancel</a>
                                <button type="submit" class="btn btn-primary px-4 fw-medium"
                                        style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                                    <i class="bi bi-send me-2"></i>Submit Request
                                </button>
                            </div>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const startInput   = document.getElementById('startDate');
    const endInput     = document.getElementById('endDate');
    const dayCounter   = document.getElementById('dayCounter');
    const dayCountEl   = document.getElementById('dayCount');
    const reasonInput  = document.getElementById('reason');
    const charCountEl  = document.getElementById('charCount');

    function updateDayCount() {
        if (!startInput || !endInput) return;
        const s = startInput.value, e = endInput.value;
        if (!s || !e) { if (dayCounter) dayCounter.style.display = 'none'; return; }
        const start = new Date(s), end = new Date(e);
        if (end < start) { if (dayCounter) dayCounter.style.display = 'none'; return; }
        const diff = Math.round((end - start) / (1000*60*60*24)) + 1;
        if (dayCountEl) dayCountEl.textContent = diff;
        if (dayCounter) dayCounter.style.display = 'flex';
    }

    if (startInput) startInput.addEventListener('change', updateDayCount);
    if (endInput)   endInput.addEventListener('change', updateDayCount);
    updateDayCount();

    function updateCharCount() {
        if (!reasonInput || !charCountEl) return;
        charCountEl.textContent = reasonInput.value.length;
    }
    if (reasonInput) reasonInput.addEventListener('input', updateCharCount);
    updateCharCount();

    const today = new Date();
    today.setDate(today.getDate() + 1);
    const tomorrow = today.toISOString().split('T')[0];
    if (startInput && !startInput.min) startInput.min = tomorrow;
    if (endInput   && !endInput.min)   endInput.min   = tomorrow;

    if (startInput) {
        startInput.addEventListener('change', () => {
            if (endInput && (!endInput.value || endInput.value < startInput.value)) {
                endInput.value = startInput.value;
                updateDayCount();
            }
            if (endInput) endInput.min = startInput.value;
        });
    }
</script>
</body>
</html>

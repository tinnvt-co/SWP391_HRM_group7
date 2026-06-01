<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="attendanceAdd" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>New Attendance Record &mdash; HRM System</title>
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
        .info-badge {
            background:#f0f9ff; border:1px solid #bae6fd;
            border-radius:10px; padding:0.75rem 1rem; font-size:0.85rem; color:#0369a1;
        }
    </style>
</head>
<body>
<%@ include file="/views/common/navbar.jsp" %>
<%@ include file="/views/common/sidebar.jsp" %>

<div class="main-content">
    <div class="d-flex align-items-center gap-2 mb-4">
        <a href="${pageContext.request.contextPath}/attendance" class="btn btn-sm btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Back
        </a>
        <div>
            <h5 class="fw-bold text-dark mb-0">New Attendance Record</h5>
            <small class="text-muted">Create a daily attendance record for a subordinate employee</small>
        </div>
    </div>

    <div class="row justify-content-center">
        <div class="col-xl-8">
            <c:if test="${empty employees}">
                <div class="info-badge d-flex align-items-center gap-2 mb-4">
                    <i class="bi bi-info-circle-fill"></i>
                    <span>You currently manage no employees. Ask HR to assign employees under you before creating attendance records.</span>
                </div>
            </c:if>

            <c:if test="${not empty error}">
                <div class="alert alert-danger d-flex align-items-center gap-2 py-2 mb-3">
                    <i class="bi bi-exclamation-circle-fill"></i><span>${error}</span>
                </div>
            </c:if>

            <div class="card border-0 shadow-sm rounded-3 p-4">
                <form action="${pageContext.request.contextPath}/attendance?action=add"
                      method="post" id="attForm" novalidate>

                    <div class="section-title">Employee &amp; Date</div>
                    <div class="row g-3 mb-4">
                        <div class="col-md-7">
                            <label for="employeeId" class="form-label required">Employee</label>
                            <select id="employeeId" name="employeeId" class="form-select" required>
                                <option value="">-- Select an employee --</option>
                                <c:forEach var="e" items="${employees}">
                                    <option value="${e.employeeId}"
                                            ${param.employeeId == e.employeeId ? 'selected' : ''}>
                                        ${e.fullName} &middot; ${e.employeeCode} &middot; ${e.departmentName}
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-md-5">
                            <label for="workDate" class="form-label required">Work Date</label>
                            <input type="date" id="workDate" name="workDate" class="form-control"
                                   value="${not empty param.workDate ? param.workDate : ''}"
                                   required>
                        </div>
                    </div>

                    <div class="section-title">Status</div>
                    <div class="row g-3 mb-4">
                        <div class="col-md-6">
                            <label for="attendanceStatus" class="form-label required">Attendance Status</label>
                            <select id="attendanceStatus" name="attendanceStatus" class="form-select" required>
                                <option value="">-- Select status --</option>
                                <c:forEach var="s" items="${statuses}">
                                    <option value="${s}"
                                            ${param.attendanceStatus == s.name() ? 'selected' : ''}>
                                        ${s.dbValue}
                                    </option>
                                </c:forEach>
                            </select>
                            <div class="form-text">Present / Late requires check-in and check-out time.</div>
                        </div>
                        <div class="col-md-3">
                            <label for="checkInTime" class="form-label">Check-in</label>
                            <input type="time" id="checkInTime" name="checkInTime" class="form-control"
                                   value="${not empty param.checkInTime ? param.checkInTime : ''}">
                        </div>
                        <div class="col-md-3">
                            <label for="checkOutTime" class="form-label">Check-out</label>
                            <input type="time" id="checkOutTime" name="checkOutTime" class="form-control"
                                   value="${not empty param.checkOutTime ? param.checkOutTime : ''}">
                        </div>
                        <div class="col-md-3">
                            <label for="overtimeHours" class="form-label">Overtime (hours)</label>
                            <input type="number" step="0.25" min="0" id="overtimeHours" name="overtimeHours"
                                   class="form-control" placeholder="0"
                                   value="${not empty param.overtimeHours ? param.overtimeHours : '0'}">
                        </div>
                        <div class="col-md-9">
                            <label class="form-label">Working Hours (auto)</label>
                            <input type="text" id="workingHoursPreview" class="form-control bg-light"
                                   value="0.00" disabled>
                        </div>
                    </div>

                    <div class="section-title">Note</div>
                    <div class="mb-4">
                        <label for="note" class="form-label">Note</label>
                        <textarea id="note" name="note" class="form-control" rows="3"
                                  maxlength="255"
                                  placeholder="Optional remarks (e.g. late check-in reason, OT approved by...)">${not empty param.note ? param.note : ''}</textarea>
                        <div class="form-text d-flex justify-content-between">
                            <span>Up to 255 characters.</span>
                            <span><span id="charCount">0</span>/255</span>
                        </div>
                    </div>

                    <div class="d-flex justify-content-end gap-2">
                        <a href="${pageContext.request.contextPath}/attendance"
                           class="btn btn-outline-secondary px-4">Cancel</a>
                        <button type="submit" class="btn btn-primary px-4 fw-medium"
                                style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;"
                                ${empty employees ? 'disabled' : ''}>
                            <i class="bi bi-check-lg me-2"></i>Create Record
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const checkIn   = document.getElementById('checkInTime');
    const checkOut  = document.getElementById('checkOutTime');
    const preview   = document.getElementById('workingHoursPreview');
    const noteInput = document.getElementById('note');
    const charCount = document.getElementById('charCount');
    const workDate  = document.getElementById('workDate');

    if (workDate) workDate.max = new Date().toISOString().split('T')[0];

    function updateHours() {
        if (!checkIn || !checkOut || !preview) return;
        const a = checkIn.value, b = checkOut.value;
        if (!a || !b) { preview.value = '0.00'; return; }
        const [h1, m1] = a.split(':').map(Number);
        const [h2, m2] = b.split(':').map(Number);
        const minutes = (h2 * 60 + m2) - (h1 * 60 + m1);
        if (minutes <= 0) { preview.value = '0.00'; return; }
        preview.value = (minutes / 60).toFixed(2);
    }
    if (checkIn)  checkIn.addEventListener('change', updateHours);
    if (checkOut) checkOut.addEventListener('change', updateHours);
    updateHours();

    function updateCharCount() {
        if (!noteInput || !charCount) return;
        charCount.textContent = noteInput.value.length;
    }
    if (noteInput) noteInput.addEventListener('input', updateCharCount);
    updateCharCount();
</script>
</body>
</html>

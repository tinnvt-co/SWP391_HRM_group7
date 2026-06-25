<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activePage" value="attendanceList" scope="request"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit Attendance Record &mdash; HRM System</title>
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
        .employee-banner {
            background: linear-gradient(135deg, #1a3c5e, #2d6a9f);
            color: white; border-radius: 12px; padding: 1rem 1.25rem;
        }
        .info-badge {
            background:#fff8e1; border:1px solid #fde68a;
            border-radius:10px; padding:0.75rem 1rem;
            font-size:0.85rem; color:#92400e;
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
            <h5 class="fw-bold text-dark mb-0">Edit Attendance Record</h5>
            <small class="text-muted">Update record before verification</small>
        </div>
    </div>

    <div class="row justify-content-center">
        <div class="col-xl-8">

            <div class="employee-banner d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="bg-white bg-opacity-25 rounded-3 d-flex align-items-center justify-content-center"
                         style="width:44px;height:44px;">
                        <i class="bi bi-person-badge fs-4"></i>
                    </div>
                    <div>
                        <div class="fw-bold">${record.employeeFullName}</div>
                        <div class="opacity-75 small">${record.employeeCode}</div>
                    </div>
                </div>
                <span class="badge bg-white bg-opacity-25 text-white px-3">
                    <i class="bi bi-hourglass-split me-1"></i>${record.verificationStatus}
                </span>
            </div>

            <div class="info-badge d-flex align-items-center gap-2 mb-3">
                <i class="bi bi-info-circle-fill"></i>
                <span>Editing is allowed only while verification status is <strong>Pending</strong> or <strong>Rejected</strong>. Once verified, the record is locked.</span>
            </div>

            <c:if test="${not empty error}">
                <div class="alert alert-danger d-flex align-items-center gap-2 py-2 mb-3">
                    <i class="bi bi-exclamation-circle-fill"></i><span>${error}</span>
                </div>
            </c:if>

            <div class="card border-0 shadow-sm rounded-3 p-4">
                <form action="${pageContext.request.contextPath}/attendance?action=edit"
                      method="post" novalidate>
                    <input type="hidden" name="id" value="${record.attendanceId}">

                    <div class="section-title">Date</div>
                    <div class="row g-3 mb-4">
                        <div class="col-md-6">
                            <label for="workDate" class="form-label required">Work Date</label>
                            <input type="date" id="workDate" name="workDate" class="form-control"
                                   value="${record.workDate}" required>
                        </div>
                    </div>

                    <div class="section-title">Status</div>
                    <div class="row g-3 mb-4">
                        <div class="col-md-6">
                            <label for="attendanceStatus" class="form-label required">Attendance Status</label>
                            <select id="attendanceStatus" name="attendanceStatus" class="form-select" required>
                                <c:forEach var="s" items="${statuses}">
                                    <option value="${s}"
                                            ${record.attendanceStatus == s ? 'selected' : ''}>
                                        ${s.dbValue}
                                    </option>
                                </c:forEach>
                            </select>
                            <div class="form-text">Select the attendance status for this day.</div>
                        </div>
                        <div class="col-md-6">
                            <label for="overtimeHours" class="form-label">Overtime (hours)</label>
                            <input type="number" step="0.25" min="0" id="overtimeHours" name="overtimeHours"
                                   class="form-control"
                                   value="${record.overtimeHours}">
                        </div>
                    </div>

                    <div class="section-title">Note</div>
                    <div class="mb-4">
                        <label for="note" class="form-label">Note</label>
                        <textarea id="note" name="note" class="form-control" rows="3"
                                  maxlength="255">${record.note}</textarea>
                        <div class="form-text d-flex justify-content-between">
                            <span>Up to 255 characters.</span>
                            <span><span id="charCount">0</span>/255</span>
                        </div>
                    </div>

                    <div class="d-flex justify-content-end gap-2">
                        <a href="${pageContext.request.contextPath}/attendance"
                           class="btn btn-outline-secondary px-4">Cancel</a>
                        <button type="submit" class="btn btn-primary px-4 fw-medium"
                                style="background:linear-gradient(135deg,#1a3c5e,#2d6a9f);border:none;">
                            <i class="bi bi-check-lg me-2"></i>Save Changes
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const noteInput = document.getElementById('note');
    const charCount = document.getElementById('charCount');
    const workDate  = document.getElementById('workDate');

    if (workDate) workDate.max = new Date().toISOString().split('T')[0];

    function updateCharCount() {
        if (!noteInput || !charCount) return;
        charCount.textContent = noteInput.value.length;
    }
    if (noteInput) noteInput.addEventListener('input', updateCharCount);
    updateCharCount();
</script>
</body>
</html>

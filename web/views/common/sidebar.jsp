<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<div class="sidebar">
    <div class="sidebar-label">Navigation</div>
    <ul class="nav flex-column">
        <li class="nav-item">
            <a class="nav-link ${activePage == 'home' ? 'active' : ''}"
               href="${pageContext.request.contextPath}/home">
                <i class="bi bi-house-door me-2"></i>Dashboard
            </a>
        </li>
        <c:if test="${fn:contains(permissions, 'VIEW_USER_LIST')}">
            <li class="nav-item">
                <a class="nav-link ${activePage == 'users' ? 'active' : ''}"
                   href="${pageContext.request.contextPath}/users">
                    <i class="bi bi-people me-2"></i>Employees
                </a>
            </li>
        </c:if>
        <c:if test="${fn:contains(permissions, 'VIEW_ROLE_LIST')}">
            <li class="nav-item">
                <a class="nav-link ${activePage == 'roles' ? 'active' : ''}"
                   href="${pageContext.request.contextPath}/roles">
                    <i class="bi bi-shield-check me-2"></i>Roles & Permissions
                </a>
            </li>
        </c:if>
        <c:if test="${fn:contains(permissions, 'VIEW_DEPARTMENT_LIST')}">
            <li class="nav-item">
                <a class="nav-link ${activePage == 'departments' ? 'active' : ''}"
                   href="${pageContext.request.contextPath}/departments">
                    <i class="bi bi-diagram-3 me-2"></i>Departments
                </a>
            </li>
        </c:if>
        <c:if test="${fn:contains(permissions, 'VIEW_EMPLOYEE_LIST')}">
            <li class="nav-item">
                <a class="nav-link ${activePage == 'hrEmployees' ? 'active' : ''}"
                   href="${pageContext.request.contextPath}/hr/employees">
                    <i class="bi bi-person-vcard me-2"></i>Employees
                </a>
            </li>
        </c:if>
        <c:if test="${fn:contains(permissions, 'SUBMIT_LEAVE_REQUEST')
                   or fn:contains(permissions, 'VIEW_LEAVE_REQUEST_STATUS')
                   or fn:contains(permissions, 'VIEW_LEAVE_REQUEST_LIST')}">
            <div class="sidebar-label mt-2">Leave</div>
            <c:if test="${fn:contains(permissions, 'SUBMIT_LEAVE_REQUEST')}">
                <li class="nav-item">
                    <a class="nav-link ${activePage == 'leaveSubmit' ? 'active' : ''}"
                       href="${pageContext.request.contextPath}/leave-requests">
                        <i class="bi bi-calendar-plus me-2"></i>Submit Leave Request
                    </a>
                </li>
            </c:if>
            <c:if test="${fn:contains(permissions, 'VIEW_LEAVE_REQUEST_STATUS')}">
                <li class="nav-item">
                    <a class="nav-link ${activePage == 'leaveStatus' ? 'active' : ''}"
                       href="${pageContext.request.contextPath}/leave-requests?action=status">
                        <i class="bi bi-list-check me-2"></i>My Leave Requests
                    </a>
                </li>
            </c:if>
            <c:if test="${fn:contains(permissions, 'VIEW_LEAVE_REQUEST_LIST')}">
                <li class="nav-item">
                    <a class="nav-link ${activePage == 'leaveList' ? 'active' : ''}"
                       href="${pageContext.request.contextPath}/leave-requests?action=list">
                        <i class="bi bi-clipboard-check me-2"></i>Leave Requests
                    </a>
                </li>
            </c:if>
        </c:if>
        <c:if test="${fn:contains(permissions, 'VIEW_ATTENDANCE')
                   or fn:contains(permissions, 'VERIFY_STAFF_ATTENDANCE')}">
            <div class="sidebar-label mt-2">Attendance</div>
            <c:if test="${fn:contains(permissions, 'VIEW_ATTENDANCE')}">
                <li class="nav-item">
                    <a class="nav-link ${activePage == 'attendanceList' ? 'active' : ''}"
                       href="${pageContext.request.contextPath}/attendance">
                        <i class="bi bi-calendar2-week me-2"></i>Attendance Records
                    </a>
                </li>
            </c:if>
        </c:if>
        <c:if test="${fn:contains(permissions, 'VIEW_CONTRACT_LIST')
                   or fn:contains(permissions, 'VIEW_MY_CONTRACT')}">
            <div class="sidebar-label mt-2">Contract</div>
            <c:if test="${fn:contains(permissions, 'VIEW_CONTRACT_LIST')}">
                <li class="nav-item">
                    <a class="nav-link ${activePage == 'contracts' ? 'active' : ''}"
                       href="${pageContext.request.contextPath}/contracts">
                        <i class="bi bi-file-earmark-text me-2"></i>Contracts
                    </a>
                </li>
            </c:if>
            <c:if test="${fn:contains(permissions, 'VIEW_MY_CONTRACT')}">
                <li class="nav-item">
                    <a class="nav-link ${activePage == 'myContract' ? 'active' : ''}"
                       href="${pageContext.request.contextPath}/my-contract">
                        <i class="bi bi-file-earmark-person me-2"></i>My Contract
                    </a>
                </li>
            </c:if>
        </c:if>
        <div class="sidebar-label mt-2">Account</div>
        <li class="nav-item">
            <a class="nav-link ${activePage == 'profile' ? 'active' : ''}"
               href="${pageContext.request.contextPath}/profile">
                <i class="bi bi-person me-2"></i>My Profile
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${activePage == 'changePassword' ? 'active' : ''}"
               href="${pageContext.request.contextPath}/change-password">
                <i class="bi bi-key me-2"></i>Change Password
            </a>
        </li>
        <li class="nav-item mt-2">
            <a class="nav-link text-danger" href="${pageContext.request.contextPath}/logout">
                <i class="bi bi-box-arrow-right me-2"></i>Sign Out
            </a>
        </li>
    </ul>
</div>

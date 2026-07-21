package service;

import dao.AttendanceRecordDAO;
import dao.AttendanceReportDAO;
import model.AttendanceReport;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AttendanceAutoConfirmService {

    private final AttendanceRecordDAO attendanceDAO = new AttendanceRecordDAO();
    private final AttendanceReportDAO reportDAO = new AttendanceReportDAO();

    public int runDueAutoConfirm() throws SQLException {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(2);
        List<AttendanceRecordDAO.AutoConfirmBatch> batches =
                attendanceDAO.findAutoConfirmBatches(cutoff);

        int updatedRecords = 0;
        for (AttendanceRecordDAO.AutoConfirmBatch batch : batches) {
            int updated = attendanceDAO.autoVerifyPendingByManagerMonth(
                    batch.managerUserId, batch.departmentId, batch.year, batch.month);
            if (updated <= 0) continue;
            updatedRecords += updated;

            List<AttendanceRecordDAO.MonthlySummary> summaries =
                    attendanceDAO.aggregateMonthByDepartment(
                            batch.year, batch.month, batch.departmentId);
            for (AttendanceRecordDAO.MonthlySummary s : summaries) {
                AttendanceReport report = new AttendanceReport();
                report.setEmployeeId(s.employeeId);
                report.setManagerId(batch.managerUserId);
                report.setDepartmentId(s.departmentId);
                report.setReportMonth(batch.month);
                report.setReportYear(batch.year);
                report.setActualWorkingDays(BigDecimal.valueOf(s.actualWorkingDays));
                report.setPaidLeaveDays(BigDecimal.valueOf(s.paidLeaveDays));
                report.setUnpaidLeaveDays(BigDecimal.valueOf(s.unpaidLeaveDays));
                report.setMaternityLeaveDays(BigDecimal.valueOf(s.maternityLeaveDays));
                report.setOvertimeHours(s.overtimeHours);
                report.setLatePenaltyAmount(s.latePenaltyAmount);
                if ("HR".equalsIgnoreCase(batch.departmentCode)) {
                    reportDAO.upsertApprovedByHrManager(
                            report, null, "Auto-approved after 2-day attendance confirmation timeout");
                } else {
                    reportDAO.upsertSubmitted(report);
                }
            }
        }
        return updatedRecords;
    }
}

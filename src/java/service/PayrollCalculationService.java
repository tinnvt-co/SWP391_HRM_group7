package service;

import dao.AllowanceTypeDAO;
import dao.AttendanceRecordDAO;
import dao.ContractDAO;
import dao.HolidayDAO;
import model.AttendanceRecord;
import model.AttendanceReport;
import model.Contract;
import model.Payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Builds one payroll line from an employee's submitted attendance report.
 *
 * Formula:
 *   daily salary = basic salary / standard working days
 *   work salary = daily salary * actual working days, with paid leave included
 *   normal OT = hourly salary * normal OT hours * 1.5
 *   weekend OT = hourly salary * weekend OT hours * 2.0
 *   holiday OT = hourly salary * holiday OT hours * 3.0
 *   gross = work salary + active allowances + KPI bonus + OT salary
 *   insurance base = work salary + KPI bonus + OT salary
 *   deductions = employee insurance + PIT + advance payment + late penalty
 *   net = gross - deductions
 *
 * Maternity benefit is shown separately as a social-insurance benefit and is
 * not included in the company-paid net salary.
 */
public class PayrollCalculationService {

    public static final BigDecimal SOCIAL_INSURANCE_RATE = new BigDecimal("0.08");
    public static final BigDecimal HEALTH_INSURANCE_RATE = new BigDecimal("0.015");
    public static final BigDecimal UNEMPLOYMENT_INSURANCE_RATE = new BigDecimal("0.01");
    public static final BigDecimal NORMAL_OT_MULTIPLIER = new BigDecimal("1.5");
    public static final BigDecimal WEEKEND_OT_MULTIPLIER = new BigDecimal("2.0");
    public static final BigDecimal HOLIDAY_OT_MULTIPLIER = new BigDecimal("3.0");

    private static final BigDecimal HOURS_PER_DAY = new BigDecimal("8");
    private static final int SCALE = 0;

    private final ContractDAO contractDAO = new ContractDAO();
    private final AllowanceTypeDAO allowanceDAO = new AllowanceTypeDAO();
    private final AttendanceRecordDAO attendanceDAO = new AttendanceRecordDAO();
    private final HolidayDAO holidayDAO = new HolidayDAO();

    public static final class BuildResult {
        public final Payroll payroll;
        public final String skipReason;

        private BuildResult(Payroll p, String reason) {
            this.payroll = p;
            this.skipReason = reason;
        }

        static BuildResult ok(Payroll p) { return new BuildResult(p, null); }
        static BuildResult skip(String reason) { return new BuildResult(null, reason); }
    }

    public BuildResult build(int periodId, AttendanceReport report) throws SQLException {
        Contract c = contractDAO.findActiveByEmployeeId(report.getEmployeeId());
        if (c == null) {
            return BuildResult.skip("No active contract for " + report.getEmployeeCode());
        }

        BigDecimal basic = nz(c.getBasicSalary());
        BigDecimal stdDays = nz(c.getStandardWorkingDays());
        if (stdDays.signum() <= 0) stdDays = new BigDecimal("26");

        BigDecimal actualDays = nz(report.getActualWorkingDays());
        BigDecimal kpi = nz(report.getKpiBonus());
        BigDecimal advance = nz(report.getAdvancePayment());
        BigDecimal latePenalty = nz(report.getLatePenaltyAmount());
        BigDecimal maternityLeaveDays = nz(report.getMaternityLeaveDays());

        BigDecimal daily = basic.divide(stdDays, 4, RoundingMode.HALF_UP);
        BigDecimal workSalary = daily.multiply(actualDays);

        BigDecimal hourly = basic.divide(new BigDecimal("26"), 4, RoundingMode.HALF_UP)
                .divide(HOURS_PER_DAY, 4, RoundingMode.HALF_UP);
        OvertimeBreakdown overtime = classifyOvertime(report);
        BigDecimal normalOtSalary = hourly.multiply(overtime.normalHours).multiply(NORMAL_OT_MULTIPLIER);
        BigDecimal weekendOtSalary = hourly.multiply(overtime.weekendHours).multiply(WEEKEND_OT_MULTIPLIER);
        BigDecimal holidayOtSalary = hourly.multiply(overtime.holidayHours).multiply(HOLIDAY_OT_MULTIPLIER);
        BigDecimal otSalary = normalOtSalary.add(weekendOtSalary).add(holidayOtSalary);

        BigDecimal allowance = nz(allowanceDAO.sumActiveAllowances());
        BigDecimal gross = workSalary.add(allowance).add(kpi).add(otSalary);
        BigDecimal insuranceBase = workSalary.add(kpi).add(otSalary);
        BigDecimal socialInsurance = insuranceBase.multiply(SOCIAL_INSURANCE_RATE);
        BigDecimal healthInsurance = insuranceBase.multiply(HEALTH_INSURANCE_RATE);
        BigDecimal unemploymentInsurance = insuranceBase.multiply(UNEMPLOYMENT_INSURANCE_RATE);
        BigDecimal personalIncomeTax = BigDecimal.ZERO;
        BigDecimal deduction = socialInsurance
                .add(healthInsurance)
                .add(unemploymentInsurance)
                .add(personalIncomeTax)
                .add(advance)
                .add(latePenalty);
        BigDecimal net = gross.subtract(deduction);
        BigDecimal socialInsuranceBenefit = daily.multiply(maternityLeaveDays);

        Payroll p = new Payroll();
        p.setPayrollPeriodId(periodId);
        p.setEmployeeId(report.getEmployeeId());
        p.setContractId(c.getContractId());
        p.setAttendanceReportId(report.getAttendanceReportId());
        p.setBasicSalary(round(basic));
        p.setActualWorkingDays(actualDays);
        p.setWorkSalary(round(workSalary));
        p.setTotalAllowance(round(allowance));
        p.setKpiBonus(round(kpi));
        p.setNormalOvertimeHours(overtime.normalHours);
        p.setWeekendOvertimeHours(overtime.weekendHours);
        p.setHolidayOvertimeHours(overtime.holidayHours);
        p.setNormalOvertimeSalary(round(normalOtSalary));
        p.setWeekendOvertimeSalary(round(weekendOtSalary));
        p.setHolidayOvertimeSalary(round(holidayOtSalary));
        p.setOvertimeSalary(round(otSalary));
        p.setGrossSalary(round(gross));
        p.setInsuranceBase(round(insuranceBase));
        p.setSocialInsurance(round(socialInsurance));
        p.setHealthInsurance(round(healthInsurance));
        p.setUnemploymentInsurance(round(unemploymentInsurance));
        p.setPersonalIncomeTax(round(personalIncomeTax));
        p.setAdvancePayment(round(advance));
        p.setLatePenaltyAmount(round(latePenalty));
        p.setTotalDeduction(round(deduction));
        p.setNetSalary(round(net));
        p.setMaternityLeaveDays(maternityLeaveDays);
        p.setSocialInsuranceBenefit(round(socialInsuranceBenefit));
        p.setOvertimeHours(overtime.totalHours());
        return BuildResult.ok(p);
    }

    private OvertimeBreakdown classifyOvertime(AttendanceReport report) throws SQLException {
        OvertimeBreakdown breakdown = new OvertimeBreakdown();
        List<AttendanceRecord> records = attendanceDAO.findVerifiedByEmployeeMonth(
                report.getEmployeeId(), report.getReportYear(), report.getReportMonth());
        Set<LocalDate> holidayDates = holidayDAO.findActiveDatesByMonth(
                report.getReportYear(), report.getReportMonth());

        for (AttendanceRecord record : records) {
            BigDecimal hours = nz(record.getOvertimeHours());
            if (hours.signum() <= 0 || record.getWorkDate() == null) continue;

            LocalDate date = record.getWorkDate();
            if (holidayDates.contains(date)) {
                breakdown.holidayHours = breakdown.holidayHours.add(hours);
            } else if (isWeekend(date)) {
                breakdown.weekendHours = breakdown.weekendHours.add(hours);
            } else {
                breakdown.normalHours = breakdown.normalHours.add(hours);
            }
        }
        return breakdown;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal round(BigDecimal v) {
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static final class OvertimeBreakdown {
        BigDecimal normalHours = BigDecimal.ZERO;
        BigDecimal weekendHours = BigDecimal.ZERO;
        BigDecimal holidayHours = BigDecimal.ZERO;

        BigDecimal totalHours() {
            return normalHours.add(weekendHours).add(holidayHours);
        }
    }
}

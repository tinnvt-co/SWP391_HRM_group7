package service;

import dao.AllowanceTypeDAO;
import dao.AttendanceRecordDAO;
import dao.ContractDAO;
import dao.EmployeeDAO;
import dao.HolidayDAO;
import dao.PayrollPeriodDAO;
import model.AttendanceRecord;
import model.AttendanceReport;
import model.AllowanceType;
import model.Contract;
import model.Employee;
import model.Payroll;
import model.PayrollPeriod;

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
 *   attendance bonus = 500,000 VND when the period has full attendance
 *   gross = work salary + payable allowances + attendance bonus + KPI bonus + OT salary
 *   insurance base = work salary + KPI bonus + OT salary
 *   taxable income = gross - tax-exempt lunch allowance - employee insurance
 *                    - personal deduction - dependent deductions
 *   PIT = seven-bracket progressive tax requested by the payroll specification
 *   deductions = employee insurance + PIT + advance payment + late penalty
 *   net = gross - deductions
 *
 * Allowances are paid by the company only when the employee has company-paid
 * working/leave days in the period. Maternity benefit is shown separately as a
 * social-insurance benefit and is not included in the company-paid net salary.
 *
 * Fixed monthly contracts ignore attendance deductions for company-paid salary:
 * work salary equals the contract basic salary, allowance comes from the
 * contract fixed allowance amount, and attendance remains an audit record.
 */
public class PayrollCalculationService {

    public static final BigDecimal SOCIAL_INSURANCE_RATE = new BigDecimal("0.08");
    public static final BigDecimal HEALTH_INSURANCE_RATE = new BigDecimal("0.015");
    public static final BigDecimal UNEMPLOYMENT_INSURANCE_RATE = new BigDecimal("0.01");
    public static final BigDecimal NORMAL_OT_MULTIPLIER = new BigDecimal("1.5");
    public static final BigDecimal WEEKEND_OT_MULTIPLIER = new BigDecimal("2.0");
    public static final BigDecimal HOLIDAY_OT_MULTIPLIER = new BigDecimal("3.0");
    public static final BigDecimal FULL_ATTENDANCE_BONUS = new BigDecimal("500000");
    public static final BigDecimal PERSONAL_DEDUCTION = new BigDecimal("11000000");
    public static final BigDecimal DEPENDENT_DEDUCTION = new BigDecimal("4400000");
    public static final BigDecimal LUNCH_TAX_EXEMPT_LIMIT = new BigDecimal("730000");

    private static final BigDecimal HOURS_PER_DAY = new BigDecimal("8");
    private static final int SCALE = 0;

    private final ContractDAO contractDAO = new ContractDAO();
    private final AllowanceTypeDAO allowanceDAO = new AllowanceTypeDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final AttendanceRecordDAO attendanceDAO = new AttendanceRecordDAO();
    private final HolidayDAO holidayDAO = new HolidayDAO();
    private final PayrollPeriodDAO periodDAO = new PayrollPeriodDAO();

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
        Contract c = contractDAO.findEffectiveByEmployeeId(
                report.getEmployeeId(), report.getReportYear(), report.getReportMonth());
        if (c == null) {
            return BuildResult.skip("No effective contract for " + report.getEmployeeCode()
                    + " in " + report.getReportMonth() + "/" + report.getReportYear());
        }

        BigDecimal basic = nz(c.getBasicSalary());
        PayrollPeriod period = periodDAO.findById(periodId);
        BigDecimal stdDays = period == null
                ? nz(report.getStandardWorkingDays())
                : nz(period.getStandardWorkingDays());
        if (stdDays.signum() <= 0) {
            return BuildResult.skip("Payroll period has no valid standard working-day snapshot.");
        }

        BigDecimal actualDays = nz(report.getActualWorkingDays());
        BigDecimal expectedDays = nz(report.getExpectedWorkingDays());
        if (expectedDays.signum() <= 0) expectedDays = stdDays;
        BigDecimal kpi = nz(report.getKpiBonus());
        BigDecimal advance = nz(report.getAdvancePayment());
        BigDecimal latePenalty = nz(report.getLatePenaltyAmount());
        BigDecimal maternityLeaveDays = nz(report.getMaternityLeaveDays());
        boolean fixedMonthly = c.getSalaryPolicy() == Contract.SalaryPolicy.FixedMonthly;

        BigDecimal daily = basic.divide(stdDays, 4, RoundingMode.HALF_UP);
        BigDecimal workSalary = fixedMonthly ? basic : daily.multiply(actualDays);

        BigDecimal hourly = basic.divide(stdDays, 4, RoundingMode.HALF_UP)
                .divide(HOURS_PER_DAY, 4, RoundingMode.HALF_UP);
        OvertimeBreakdown overtime = classifyOvertime(report);
        BigDecimal normalOtSalary = fixedMonthly
                ? BigDecimal.ZERO
                : hourly.multiply(overtime.normalHours).multiply(NORMAL_OT_MULTIPLIER);
        BigDecimal weekendOtSalary = fixedMonthly
                ? BigDecimal.ZERO
                : hourly.multiply(overtime.weekendHours).multiply(WEEKEND_OT_MULTIPLIER);
        BigDecimal holidayOtSalary = fixedMonthly
                ? BigDecimal.ZERO
                : hourly.multiply(overtime.holidayHours).multiply(HOLIDAY_OT_MULTIPLIER);
        BigDecimal otSalary = normalOtSalary.add(weekendOtSalary).add(holidayOtSalary);

        boolean fullMaternityMonth = maternityLeaveDays.compareTo(expectedDays) >= 0;
        AllowanceBreakdown allowanceBreakdown = fixedMonthly
                || fullMaternityMonth || actualDays.signum() <= 0
                ? new AllowanceBreakdown()
                : calculateAllowances(
                        employeeDAO.findRoleNameByEmployeeId(report.getEmployeeId()));
        BigDecimal allowance = fixedMonthly
                ? nz(c.getFixedAllowanceAmount())
                : allowanceBreakdown.total;
        BigDecimal attendanceBonus = !fixedMonthly
                && qualifiesForFullAttendanceBonus(actualDays, expectedDays, fullMaternityMonth, overtime)
                    ? FULL_ATTENDANCE_BONUS
                    : BigDecimal.ZERO;
        BigDecimal payableLatePenalty = fixedMonthly ? BigDecimal.ZERO : latePenalty;

        BigDecimal gross = workSalary.add(allowance).add(attendanceBonus).add(kpi).add(otSalary);
        BigDecimal insuranceBase = workSalary.add(kpi).add(otSalary);
        BigDecimal socialInsurance = insuranceBase.multiply(SOCIAL_INSURANCE_RATE);
        BigDecimal healthInsurance = insuranceBase.multiply(HEALTH_INSURANCE_RATE);
        BigDecimal unemploymentInsurance = insuranceBase.multiply(UNEMPLOYMENT_INSURANCE_RATE);
        BigDecimal employeeInsurance = socialInsurance
                .add(healthInsurance)
                .add(unemploymentInsurance);
        Employee employee = employeeDAO.findById(report.getEmployeeId());
        int dependentCount = employee == null ? 0 : Math.max(employee.getDependentCount(), 0);
        BigDecimal dependentDeduction = DEPENDENT_DEDUCTION
                .multiply(BigDecimal.valueOf(dependentCount));
        BigDecimal taxableIncome = gross
                .subtract(allowanceBreakdown.taxExempt)
                .subtract(employeeInsurance)
                .subtract(PERSONAL_DEDUCTION)
                .subtract(dependentDeduction)
                .max(BigDecimal.ZERO);
        BigDecimal personalIncomeTax = calculatePersonalIncomeTax(taxableIncome);
        BigDecimal deduction = socialInsurance
                .add(healthInsurance)
                .add(unemploymentInsurance)
                .add(personalIncomeTax)
                .add(advance)
                .add(payableLatePenalty);
        BigDecimal net = gross.subtract(deduction);
        BigDecimal socialInsuranceBenefit = daily.multiply(maternityLeaveDays.min(stdDays));

        Payroll p = new Payroll();
        p.setPayrollPeriodId(periodId);
        p.setEmployeeId(report.getEmployeeId());
        p.setContractId(c.getContractId());
        p.setAttendanceReportId(report.getAttendanceReportId());
        p.setBasicSalary(round(basic));
        p.setActualWorkingDays(actualDays);
        p.setWorkSalary(round(workSalary));
        p.setTotalAllowance(round(allowance));
        p.setAttendanceBonusAmount(round(attendanceBonus));
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
        p.setLatePenaltyAmount(round(payableLatePenalty));
        p.setTotalDeduction(round(deduction));
        p.setNetSalary(round(net));
        p.setMaternityLeaveDays(maternityLeaveDays);
        p.setSocialInsuranceBenefit(round(socialInsuranceBenefit));
        p.setOvertimeHours(overtime.totalHours());
        if (fixedMonthly) {
            p.setNote("Fixed monthly salary policy; attendance is recorded for audit only.");
        }
        return BuildResult.ok(p);
    }

    private boolean qualifiesForFullAttendanceBonus(BigDecimal actualDays, BigDecimal stdDays,
                                                    boolean fullMaternityMonth,
                                                    OvertimeBreakdown attendance) {
        return !fullMaternityMonth
                && actualDays.compareTo(stdDays) >= 0
                && !attendance.hasFullAttendanceBlocker;
    }

    private OvertimeBreakdown classifyOvertime(AttendanceReport report) throws SQLException {
        OvertimeBreakdown breakdown = new OvertimeBreakdown();
        List<AttendanceRecord> records = attendanceDAO.findVerifiedByEmployeeMonth(
                report.getEmployeeId(), report.getReportYear(), report.getReportMonth());
        Set<LocalDate> holidayDates = holidayDAO.findActiveDatesByMonth(
                report.getReportYear(), report.getReportMonth());

        for (AttendanceRecord record : records) {
            BigDecimal hours = nz(record.getOvertimeHours());
            if (record.getAttendanceStatus() == AttendanceRecord.AttendanceStatus.Late
                    || record.getAttendanceStatus() == AttendanceRecord.AttendanceStatus.Absent
                    || record.getAttendanceStatus() == AttendanceRecord.AttendanceStatus.Leave
                    || record.getAttendanceStatus() == AttendanceRecord.AttendanceStatus.UnpaidLeave
                    || record.getAttendanceStatus() == AttendanceRecord.AttendanceStatus.MaternityLeave) {
                breakdown.hasFullAttendanceBlocker = true;
            }

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

    private AllowanceBreakdown calculateAllowances(String roleName) throws SQLException {
        AllowanceBreakdown result = new AllowanceBreakdown();
        for (AllowanceType allowanceType : allowanceDAO.findActiveForRole(roleName)) {
            BigDecimal amount = nz(allowanceType.getAmount());
            result.total = result.total.add(amount);
            if (AllowanceType.LUNCH_CODE.equalsIgnoreCase(allowanceType.getAllowanceCode())) {
                result.taxExempt = result.taxExempt.add(amount.min(LUNCH_TAX_EXEMPT_LIMIT));
            }
        }
        return result;
    }

    public static BigDecimal calculatePersonalIncomeTax(BigDecimal taxableIncome) {
        BigDecimal income = nz(taxableIncome).max(BigDecimal.ZERO);
        if (income.compareTo(new BigDecimal("5000000")) <= 0) {
            return income.multiply(new BigDecimal("0.05"));
        }
        if (income.compareTo(new BigDecimal("10000000")) <= 0) {
            return income.multiply(new BigDecimal("0.10"))
                    .subtract(new BigDecimal("250000"));
        }
        if (income.compareTo(new BigDecimal("18000000")) <= 0) {
            return income.multiply(new BigDecimal("0.15"))
                    .subtract(new BigDecimal("750000"));
        }
        if (income.compareTo(new BigDecimal("32000000")) <= 0) {
            return income.multiply(new BigDecimal("0.20"))
                    .subtract(new BigDecimal("1650000"));
        }
        if (income.compareTo(new BigDecimal("52000000")) <= 0) {
            return income.multiply(new BigDecimal("0.25"))
                    .subtract(new BigDecimal("3250000"));
        }
        if (income.compareTo(new BigDecimal("80000000")) <= 0) {
            return income.multiply(new BigDecimal("0.30"))
                    .subtract(new BigDecimal("5850000"));
        }
        return income.multiply(new BigDecimal("0.35"))
                .subtract(new BigDecimal("9850000"));
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
        boolean hasFullAttendanceBlocker;

        BigDecimal totalHours() {
            return normalHours.add(weekendHours).add(holidayHours);
        }
    }

    private static final class AllowanceBreakdown {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal taxExempt = BigDecimal.ZERO;
    }
}

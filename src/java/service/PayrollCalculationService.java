package service;

import dao.AllowanceTypeDAO;
import dao.ContractDAO;
import model.AttendanceReport;
import model.Contract;
import model.Payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;

/**
 * Builds a Payroll line from an employee's monthly attendance report and their
 * active contract. Pure calculation — no DB writes here (the servlet persists).
 *
 * Formula (per the agreed business rule):
 *   daily        = basic_salary / standard_working_days
 *   work_salary  = daily * actual_working_days
 *   ot_salary    = (basic_salary / 26 / 8) * overtime_hours * 1.5
 *   allowance    = sum(active global allowance types)
 *   gross        = work_salary + allowance + kpi_bonus + ot_salary
 *   insurance    = gross * 10.5%
 *   deduction    = insurance + advance_payment
 *   net          = gross - deduction
 */
public class PayrollCalculationService {

    public static final BigDecimal INSURANCE_RATE = new BigDecimal("0.105");
    public static final BigDecimal OT_MULTIPLIER  = new BigDecimal("1.5");
    private static final BigDecimal HOURS_PER_DAY = new BigDecimal("8");
    private static final int SCALE = 0; // VND — whole dong

    private final ContractDAO contractDAO = new ContractDAO();
    private final AllowanceTypeDAO allowanceDAO = new AllowanceTypeDAO();

    /** Result of building a payroll line, with a reason when it can't be built. */
    public static final class BuildResult {
        public final Payroll payroll;   // null if skipped
        public final String skipReason; // null if built
        private BuildResult(Payroll p, String reason) { this.payroll = p; this.skipReason = reason; }
        static BuildResult ok(Payroll p) { return new BuildResult(p, null); }
        static BuildResult skip(String reason) { return new BuildResult(null, reason); }
    }

    /**
     * Build a payroll line for one employee from their attendance report.
     * Skips (with a reason) if the employee has no Active contract.
     */
    public BuildResult build(int periodId, AttendanceReport report) throws SQLException {
        Contract c = contractDAO.findActiveByEmployeeId(report.getEmployeeId());
        if (c == null) {
            return BuildResult.skip("No active contract for " + report.getEmployeeCode());
        }

        BigDecimal basic   = nz(c.getBasicSalary());
        BigDecimal stdDays = nz(c.getStandardWorkingDays());
        if (stdDays.signum() <= 0) stdDays = new BigDecimal("26");

        BigDecimal actualDays = nz(report.getActualWorkingDays());
        BigDecimal otHours    = nz(report.getOvertimeHours());
        BigDecimal kpi        = nz(report.getKpiBonus());
        BigDecimal advance    = nz(report.getAdvancePayment());

        BigDecimal daily      = basic.divide(stdDays, 4, RoundingMode.HALF_UP);
        BigDecimal workSalary = daily.multiply(actualDays);

        BigDecimal otRate     = basic.divide(new BigDecimal("26"), 4, RoundingMode.HALF_UP)
                                     .divide(HOURS_PER_DAY, 4, RoundingMode.HALF_UP);
        BigDecimal otSalary   = otRate.multiply(otHours).multiply(OT_MULTIPLIER);

        BigDecimal allowance  = nz(allowanceDAO.sumActiveAllowances());

        BigDecimal gross      = workSalary.add(allowance).add(kpi).add(otSalary);
        BigDecimal insurance  = gross.multiply(INSURANCE_RATE);
        BigDecimal deduction  = insurance.add(advance);
        BigDecimal net        = gross.subtract(deduction);

        Payroll p = new Payroll();
        p.setPayrollPeriodId(periodId);
        p.setEmployeeId(report.getEmployeeId());
        p.setContractId(c.getContractId());
        p.setAttendanceReportId(report.getAttendanceReportId());
        p.setBasicSalary(round(basic));
        p.setActualWorkingDays(actualDays);
        p.setTotalAllowance(round(allowance));
        p.setKpiBonus(round(kpi));
        p.setOvertimeSalary(round(otSalary));
        p.setGrossSalary(round(gross));
        p.setTotalDeduction(round(deduction));
        p.setNetSalary(round(net));
        p.setAdvancePayment(round(advance));
        p.setOvertimeHours(otHours);
        return BuildResult.ok(p);
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private static BigDecimal round(BigDecimal v) { return v.setScale(SCALE, RoundingMode.HALF_UP); }
}

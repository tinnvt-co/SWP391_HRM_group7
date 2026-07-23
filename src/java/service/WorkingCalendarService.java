package service;

import dao.ContractDAO;
import dao.WorkScheduleDAO;
import model.Contract;
import model.WorkSchedule;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.time.DayOfWeek;

public class WorkingCalendarService {

    private final ContractDAO contractDAO;
    private final WorkScheduleDAO scheduleDAO;

    public WorkingCalendarService() {
        this(new ContractDAO(), new WorkScheduleDAO());
    }

    WorkingCalendarService(ContractDAO contractDAO, WorkScheduleDAO scheduleDAO) {
        this.contractDAO = contractDAO;
        this.scheduleDAO = scheduleDAO;
    }

    public BigDecimal standardWorkingDays(int year, int month) throws SQLException {
        WorkSchedule schedule = scheduleDAO.findDefault();
        return BigDecimal.valueOf(countScheduledDays(
                YearMonth.of(year, month).atDay(1),
                YearMonth.of(year, month).atEndOfMonth(),
                schedule));
    }

    public PeriodDays periodDaysForEmployee(int employeeId, int year, int month)
            throws SQLException {
        YearMonth period = YearMonth.of(year, month);
        Contract contract = contractDAO.findEffectiveByEmployeeId(employeeId, year, month);
        WorkSchedule schedule = contract == null ? scheduleDAO.findDefault() : contract.getWorkSchedule();
        if (schedule == null && contract != null && contract.getWorkScheduleId() > 0) {
            schedule = scheduleDAO.findById(contract.getWorkScheduleId());
        }
        if (schedule == null) schedule = scheduleDAO.findDefault();

        LocalDate periodStart = period.atDay(1);
        LocalDate periodEnd = period.atEndOfMonth();
        int standard = countScheduledDays(periodStart, periodEnd, schedule);

        LocalDate eligibleStart = periodStart;
        LocalDate eligibleEnd = periodEnd;
        if (contract != null) {
            if (contract.getStartDate() != null && contract.getStartDate().isAfter(eligibleStart)) {
                eligibleStart = contract.getStartDate();
            }
            if (contract.getEndDate() != null && contract.getEndDate().isBefore(eligibleEnd)) {
                eligibleEnd = contract.getEndDate();
            }
        }
        int expected = eligibleStart.isAfter(eligibleEnd)
                ? 0 : countScheduledDays(eligibleStart, eligibleEnd, schedule);
        return new PeriodDays(BigDecimal.valueOf(standard), BigDecimal.valueOf(expected));
    }

    static int countScheduledDays(LocalDate start, LocalDate end, WorkSchedule schedule) {
        if (start == null || end == null || start.isAfter(end) || schedule == null) return 0;
        Set<DayOfWeek> workingDays = schedule.workingDaySet();
        int count = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (workingDays.contains(date.getDayOfWeek())) count++;
        }
        return count;
    }

    public static final class PeriodDays {
        public final BigDecimal standardWorkingDays;
        public final BigDecimal expectedWorkingDays;

        PeriodDays(BigDecimal standardWorkingDays, BigDecimal expectedWorkingDays) {
            this.standardWorkingDays = standardWorkingDays;
            this.expectedWorkingDays = expectedWorkingDays;
        }
    }
}

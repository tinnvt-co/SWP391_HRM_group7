package model;

import java.math.BigDecimal;

public class AllowanceSettings {

    private BigDecimal lunchAllowance = BigDecimal.ZERO;
    private BigDecimal transportationAllowance = BigDecimal.ZERO;
    private BigDecimal phoneAllowance = BigDecimal.ZERO;
    private BigDecimal responsibilityAllowance = BigDecimal.ZERO;
    private int activeContractCount;
    private BigDecimal totalMonthlyAllowance = BigDecimal.ZERO;

    public BigDecimal getLunchAllowance() {
        return lunchAllowance;
    }

    public void setLunchAllowance(BigDecimal lunchAllowance) {
        this.lunchAllowance = lunchAllowance;
    }

    public BigDecimal getTransportationAllowance() {
        return transportationAllowance;
    }

    public void setTransportationAllowance(BigDecimal transportationAllowance) {
        this.transportationAllowance = transportationAllowance;
    }

    public BigDecimal getPhoneAllowance() {
        return phoneAllowance;
    }

    public void setPhoneAllowance(BigDecimal phoneAllowance) {
        this.phoneAllowance = phoneAllowance;
    }

    public BigDecimal getResponsibilityAllowance() {
        return responsibilityAllowance;
    }

    public void setResponsibilityAllowance(BigDecimal responsibilityAllowance) {
        this.responsibilityAllowance = responsibilityAllowance;
    }

    public int getActiveContractCount() {
        return activeContractCount;
    }

    public void setActiveContractCount(int activeContractCount) {
        this.activeContractCount = activeContractCount;
    }

    public BigDecimal getTotalMonthlyAllowance() {
        return totalMonthlyAllowance;
    }

    public void setTotalMonthlyAllowance(BigDecimal totalMonthlyAllowance) {
        this.totalMonthlyAllowance = totalMonthlyAllowance;
    }
}

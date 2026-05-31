package com.example.transfer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BillingResponse {

    private final String accountId;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final List<TransferItem> transfers;
    private final BigDecimal totalIncomingAmount;
    private final BigDecimal totalOutgoingAmount;

    public BillingResponse(String accountId, LocalDate fromDate, LocalDate toDate, 
                          List<TransferItem> transfers, BigDecimal totalIncomingAmount, 
                          BigDecimal totalOutgoingAmount) {
        this.accountId = accountId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.transfers = transfers;
        this.totalIncomingAmount = totalIncomingAmount;
        this.totalOutgoingAmount = totalOutgoingAmount;
    }

    public String getAccountId() {
        return accountId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public List<TransferItem> getTransfers() {
        return transfers;
    }

    public BigDecimal getTotalIncomingAmount() {
        return totalIncomingAmount;
    }

    public BigDecimal getTotalOutgoingAmount() {
        return totalOutgoingAmount;
    }
}
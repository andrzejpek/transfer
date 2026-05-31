package com.example.transfer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferItem {

    private final Long transferId;
    private final String sourceAccountId;
    private final String destinationAccountId;
    private final BigDecimal amount;
    private final TransferDirection direction;
    private final LocalDateTime createdAt;

    public TransferItem(Long transferId, String sourceAccountId, String destinationAccountId, 
                       BigDecimal amount, TransferDirection direction, LocalDateTime createdAt) {
        this.transferId = transferId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.direction = direction;
        this.createdAt = createdAt;
    }

    public Long getTransferId() {
        return transferId;
    }

    public String getSourceAccountId() {
        return sourceAccountId;
    }

    public String getDestinationAccountId() {
        return destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransferDirection getDirection() {
        return direction;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
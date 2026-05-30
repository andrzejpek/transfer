package com.example.transfer.dto;

public class TransferResponse {

    private String status;
    private Long transferId;

    public TransferResponse() {
    }

    public TransferResponse(String status, Long transferId) {
        this.status = status;
        this.transferId = transferId;
    }

    public String getStatus() {
        return status;
    }

    public Long getTransferId() {
        return transferId;
    }
}
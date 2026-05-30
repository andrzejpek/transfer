package com.example.transfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "transfer_id", nullable = false)
    private Long transferId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public IdempotencyRecord() {
    }

    public IdempotencyRecord(String idempotencyKey, String requestHash, Long transferId, LocalDateTime createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.transferId = transferId;
        this.createdAt = createdAt;
    }
    public String getRequestHash() {
        return requestHash;
    }

    public Long getTransferId() {
        return transferId;
    }
}
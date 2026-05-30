package com.example.transfer.controller;

import com.example.transfer.dto.TransferRequest;
import com.example.transfer.dto.TransferResponse;
import com.example.transfer.exception.InvalidTransferException;
import com.example.transfer.service.RetryingTransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final RetryingTransferService retryingTransferService;

    public TransferController(RetryingTransferService retryingTransferService) {
        this.retryingTransferService = retryingTransferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidTransferException("Idempotency-Key header is required");
        }

        TransferResponse response = retryingTransferService.transfer(idempotencyKey, request);
        return ResponseEntity.ok(response);
    }
}
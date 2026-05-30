package com.example.transfer.service;

import com.example.transfer.dto.TransferRequest;
import com.example.transfer.dto.TransferResponse;
import com.example.transfer.exception.OptimisticLockingRetryFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class RetryingTransferService {

    private static final Logger log = LoggerFactory.getLogger(RetryingTransferService.class);

    private final TransferService transferService;
    private final int maxAttempts;

    public RetryingTransferService(TransferService transferService,
                                   @Value("${transfer.retry.max-attempts:3}") int maxAttempts) {
        this.transferService = transferService;
        this.maxAttempts = maxAttempts;
    }

    public TransferResponse transfer(String idempotencyKey, TransferRequest request) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return transferService.execute(idempotencyKey, request);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("Optimistic locking conflict on attempt {}/{} for idempotency key '{}'",
                        attempt, maxAttempts, idempotencyKey);
            }
        }
        throw new OptimisticLockingRetryFailedException(maxAttempts);
    }
}
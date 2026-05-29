package com.example.transfer.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key already used with a different request payload: " + idempotencyKey);
    }
}
package com.example.transfer.exception;

public class OptimisticLockingRetryFailedException extends RuntimeException {

    public OptimisticLockingRetryFailedException(int attempts) {
        super("Transfer failed after " + attempts + " attempts due to concurrent modification");
    }
}
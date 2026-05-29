package com.example.transfer.service;

import com.example.transfer.dto.TransferRequest;
import com.example.transfer.dto.TransferResponse;
import com.example.transfer.exception.AccountNotFoundException;
import com.example.transfer.exception.OptimisticLockingRetryFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryingTransferServiceTest {

    @Mock
    private TransferService transferService;

    private RetryingTransferService retryingTransferService;

    private TransferRequest request;

    @BeforeEach
    void setUp() {
        retryingTransferService = new RetryingTransferService(transferService, 3);
        request = new TransferRequest("ACC-A", "ACC-B", new BigDecimal("100.00"));
    }

    @Test
    void successOnFirstAttempt_callsExecuteOnce() {
        TransferResponse expected = new TransferResponse("OK", 1L);
        when(transferService.execute("key", request)).thenReturn(expected);

        TransferResponse result = retryingTransferService.transfer("key", request);

        assertSame(expected, result);
        verify(transferService, times(1)).execute("key", request);
    }

    @Test
    void allAttemptsFailWithLockException_throwsOptimisticLockingRetryFailed() {
        when(transferService.execute("key", request))
                .thenThrow(new ObjectOptimisticLockingFailureException(Object.class, null));

        assertThrows(OptimisticLockingRetryFailedException.class,
                () -> retryingTransferService.transfer("key", request));

        verify(transferService, times(3)).execute("key", request);
    }

    @Test
    void lockExceptionThenSuccess_retriesAndReturns() {
        TransferResponse expected = new TransferResponse("OK", 2L);
        when(transferService.execute("key", request))
                .thenThrow(new ObjectOptimisticLockingFailureException(Object.class, null))
                .thenReturn(expected);

        TransferResponse result = retryingTransferService.transfer("key", request);

        assertSame(expected, result);
        verify(transferService, times(2)).execute("key", request);
    }

    @Test
    void nonLockExceptionOnFirstAttempt_propagatesImmediatelyWithoutRetry() {
        when(transferService.execute("key", request))
                .thenThrow(new AccountNotFoundException("ACC-A"));

        assertThrows(AccountNotFoundException.class,
                () -> retryingTransferService.transfer("key", request));

        verify(transferService, times(1)).execute("key", request);
    }
}
package com.example.transfer.service;

import com.example.transfer.domain.Account;
import com.example.transfer.domain.IdempotencyRecord;
import com.example.transfer.dto.TransferRequest;
import com.example.transfer.dto.TransferResponse;
import com.example.transfer.exception.AccountNotFoundException;
import com.example.transfer.exception.IdempotencyConflictException;
import com.example.transfer.exception.InsufficientFundsException;
import com.example.transfer.exception.InvalidTransferException;
import com.example.transfer.repository.AccountRepository;
import com.example.transfer.repository.IdempotencyRecordRepository;
import com.example.transfer.repository.TransferRepository;
import com.example.transfer.util.RequestHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @InjectMocks
    private TransferService transferService;

    private TransferRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new TransferRequest("ACC-A", "ACC-B", new BigDecimal("100.00"));
    }

    @Test
    void sameAccount_throwsInvalidTransferException() {
        TransferRequest req = new TransferRequest("ACC-A", "ACC-A", new BigDecimal("50.00"));
        assertThrows(InvalidTransferException.class,
                () -> transferService.execute("key", req));
    }

    @Test
    void duplicateKeyWithMatchingHash_returnsCachedResponse() {
        String hash = RequestHashUtil.compute(validRequest);
        IdempotencyRecord record = new IdempotencyRecord("key", hash, 99L, LocalDateTime.now());
        when(idempotencyRecordRepository.findByIdempotencyKey("key")).thenReturn(Optional.of(record));

        TransferResponse response = transferService.execute("key", validRequest);

        assertEquals("DUPLICATE", response.getStatus());
        assertEquals(99L, response.getTransferId());
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void duplicateKeyWithDifferentHash_throwsIdempotencyConflictException() {
        IdempotencyRecord record = new IdempotencyRecord("key", "differenthash", 99L, LocalDateTime.now());
        when(idempotencyRecordRepository.findByIdempotencyKey("key")).thenReturn(Optional.of(record));

        assertThrows(IdempotencyConflictException.class,
                () -> transferService.execute("key", validRequest));
    }

    @Test
    void fromAccountNotFound_throwsAccountNotFoundException() {
        when(idempotencyRecordRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(accountRepository.findById("ACC-A")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> transferService.execute("key", validRequest));
    }

    @Test
    void toAccountNotFound_throwsAccountNotFoundException() {
        when(idempotencyRecordRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(accountRepository.findById("ACC-A")).thenReturn(Optional.of(new Account("ACC-A", new BigDecimal("500.00"))));
        when(accountRepository.findById("ACC-B")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> transferService.execute("key", validRequest));
    }

    @Test
    void insufficientBalance_throwsInsufficientFundsException() {
        when(idempotencyRecordRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(accountRepository.findById("ACC-A")).thenReturn(Optional.of(new Account("ACC-A", new BigDecimal("50.00"))));
        when(accountRepository.findById("ACC-B")).thenReturn(Optional.of(new Account("ACC-B", new BigDecimal("0.00"))));

        assertThrows(InsufficientFundsException.class,
                () -> transferService.execute("key", validRequest));
    }

    @Test
    void exactBalance_transferSucceeds() {
        when(idempotencyRecordRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        Account source = new Account("ACC-A", new BigDecimal("100.00"));
        Account dest = new Account("ACC-B", new BigDecimal("0.00"));
        when(accountRepository.findById("ACC-A")).thenReturn(Optional.of(source));
        when(accountRepository.findById("ACC-B")).thenReturn(Optional.of(dest));

        com.example.transfer.domain.Transfer savedTransfer = mock(com.example.transfer.domain.Transfer.class);
        when(savedTransfer.getId()).thenReturn(1L);
        when(transferRepository.save(any())).thenReturn(savedTransfer);
        when(idempotencyRecordRepository.save(any())).thenReturn(null);

        TransferResponse response = transferService.execute("key", validRequest);

        assertEquals("OK", response.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(source.getBalance()));
    }
}
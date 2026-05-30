package com.example.transfer.service;

import com.example.transfer.domain.Account;
import com.example.transfer.domain.IdempotencyRecord;
import com.example.transfer.domain.Transfer;
import com.example.transfer.dto.TransferRequest;
import com.example.transfer.dto.TransferResponse;
import com.example.transfer.exception.AccountNotFoundException;
import com.example.transfer.exception.IdempotencyConflictException;
import com.example.transfer.exception.InvalidTransferException;
import com.example.transfer.repository.AccountRepository;
import com.example.transfer.repository.IdempotencyRecordRepository;
import com.example.transfer.repository.TransferRepository;
import com.example.transfer.util.RequestHashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public TransferService(AccountRepository accountRepository,
                           TransferRepository transferRepository,
                           IdempotencyRecordRepository idempotencyRecordRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    @Transactional
    public TransferResponse execute(String idempotencyKey, TransferRequest request) {
        if (request.getFromAccount().equals(request.getToAccount())) {
            throw new InvalidTransferException("Source and destination accounts must be different");
        }

        String requestHash = RequestHashUtil.compute(request);

        Optional<IdempotencyRecord> existing = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (record.getRequestHash().equals(requestHash)) {
                return new TransferResponse("DUPLICATE", record.getTransferId());
            }
            throw new IdempotencyConflictException(idempotencyKey);
        }

        Account source = accountRepository.findById(request.getFromAccount())
                .orElseThrow(() -> new AccountNotFoundException(request.getFromAccount()));
        Account destination = accountRepository.findById(request.getToAccount())
                .orElseThrow(() -> new AccountNotFoundException(request.getToAccount()));

        source.withdraw(request.getAmount());
        destination.deposit(request.getAmount());

        Transfer transfer = new Transfer(
                request.getFromAccount(),
                request.getToAccount(),
                request.getAmount(),
                LocalDateTime.now()
        );
        Transfer savedTransfer = transferRepository.save(transfer);

        IdempotencyRecord idempotencyRecord = new IdempotencyRecord(
                idempotencyKey,
                requestHash,
                savedTransfer.getId(),
                LocalDateTime.now()
        );
        idempotencyRecordRepository.save(idempotencyRecord);

        return new TransferResponse("OK", savedTransfer.getId());
    }
}
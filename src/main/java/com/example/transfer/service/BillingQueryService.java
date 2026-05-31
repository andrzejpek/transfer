package com.example.transfer.service;

import com.example.transfer.domain.Transfer;
import com.example.transfer.dto.BillingResponse;
import com.example.transfer.dto.TransferDirection;
import com.example.transfer.dto.TransferItem;
import com.example.transfer.exception.AccountNotFoundException;
import com.example.transfer.exception.InvalidTransferException;
import com.example.transfer.repository.AccountRepository;
import com.example.transfer.repository.BillingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BillingQueryService {

    private final BillingRepository billingRepository;
    private final AccountRepository accountRepository;

    public BillingQueryService(BillingRepository billingRepository, AccountRepository accountRepository) {
        this.billingRepository = billingRepository;
        this.accountRepository = accountRepository;
    }

    public BillingResponse getBillingForAccount(String accountId, LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);
        validateAccountExists(accountId);

        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.atTime(LocalTime.MAX);

        List<Transfer> transfers = billingRepository.findTransfersForAccountInDateRange(
                accountId, fromDateTime, toDateTime);

        List<TransferItem> transferItems = transfers.stream()
                .map(transfer -> mapToTransferItem(transfer, accountId))
                .collect(Collectors.toList());

        BigDecimal totalIncoming = calculateTotalIncoming(transfers, accountId);
        BigDecimal totalOutgoing = calculateTotalOutgoing(transfers, accountId);

        return new BillingResponse(accountId, fromDate, toDate, transferItems, totalIncoming, totalOutgoing);
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new InvalidTransferException("fromDate must not be after toDate");
        }
    }

    private void validateAccountExists(String accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
    }

    private TransferItem mapToTransferItem(Transfer transfer, String accountId) {
        TransferDirection direction = transfer.getToAccount().equals(accountId) 
                ? TransferDirection.INCOMING 
                : TransferDirection.OUTGOING;

        return new TransferItem(
                transfer.getId(),
                transfer.getFromAccount(),
                transfer.getToAccount(),
                transfer.getAmount(),
                direction,
                transfer.getDate()
        );
    }

    private BigDecimal calculateTotalIncoming(List<Transfer> transfers, String accountId) {
        return transfers.stream()
                .filter(transfer -> transfer.getToAccount().equals(accountId))
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalOutgoing(List<Transfer> transfers, String accountId) {
        return transfers.stream()
                .filter(transfer -> transfer.getFromAccount().equals(accountId))
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
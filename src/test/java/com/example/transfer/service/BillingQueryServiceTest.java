package com.example.transfer.service;

import com.example.transfer.domain.Transfer;
import com.example.transfer.dto.BillingResponse;
import com.example.transfer.dto.TransferDirection;
import com.example.transfer.dto.TransferItem;
import com.example.transfer.exception.AccountNotFoundException;
import com.example.transfer.exception.InvalidTransferException;
import com.example.transfer.repository.AccountRepository;
import com.example.transfer.repository.BillingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingQueryServiceTest {

    @Mock
    private BillingRepository billingRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private BillingQueryService billingQueryService;

    private LocalDate fromDate;
    private LocalDate toDate;
    private String accountId;

    @BeforeEach
    void setUp() {
        accountId = "ACC-123";
        fromDate = LocalDate.of(2025, 1, 1);
        toDate = LocalDate.of(2025, 1, 31);
    }

    @Test
    void invalidDateRange_throwsInvalidTransferException() {
        LocalDate invalidFromDate = LocalDate.of(2025, 2, 1);
        LocalDate invalidToDate = LocalDate.of(2025, 1, 31);

        assertThrows(InvalidTransferException.class,
                () -> billingQueryService.getBillingForAccount(accountId, invalidFromDate, invalidToDate));
    }

    @Test
    void accountNotFound_throwsAccountNotFoundException() {
        when(accountRepository.existsById(accountId)).thenReturn(false);

        assertThrows(AccountNotFoundException.class,
                () -> billingQueryService.getBillingForAccount(accountId, fromDate, toDate));
    }

    @Test
    void noTransfers_returnsEmptyBilling() {
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(billingRepository.findTransfersForAccountInDateRange(eq(accountId), any(), any()))
                .thenReturn(Collections.emptyList());

        BillingResponse response = billingQueryService.getBillingForAccount(accountId, fromDate, toDate);

        assertEquals(accountId, response.getAccountId());
        assertEquals(fromDate, response.getFromDate());
        assertEquals(toDate, response.getToDate());
        assertTrue(response.getTransfers().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalIncomingAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalOutgoingAmount()));
    }

    @Test
    void incomingTransfersOnly_calculatesCorrectTotals() {
        when(accountRepository.existsById(accountId)).thenReturn(true);

        Transfer transfer1 = new Transfer("ACC-456", accountId, new BigDecimal("100.00"), 
                LocalDateTime.of(2025, 1, 15, 10, 0));
        transfer1 = createTransferWithId(transfer1, 1L);

        Transfer transfer2 = new Transfer("ACC-789", accountId, new BigDecimal("50.00"), 
                LocalDateTime.of(2025, 1, 20, 14, 30));
        transfer2 = createTransferWithId(transfer2, 2L);

        List<Transfer> transfers = Arrays.asList(transfer1, transfer2);
        when(billingRepository.findTransfersForAccountInDateRange(eq(accountId), any(), any()))
                .thenReturn(transfers);

        BillingResponse response = billingQueryService.getBillingForAccount(accountId, fromDate, toDate);

        assertEquals(2, response.getTransfers().size());
        assertEquals(0, new BigDecimal("150.00").compareTo(response.getTotalIncomingAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalOutgoingAmount()));

        TransferItem item1 = response.getTransfers().get(0);
        assertEquals(TransferDirection.INCOMING, item1.getDirection());
        assertEquals("ACC-456", item1.getSourceAccountId());
        assertEquals(accountId, item1.getDestinationAccountId());
    }

    @Test
    void outgoingTransfersOnly_calculatesCorrectTotals() {
        when(accountRepository.existsById(accountId)).thenReturn(true);

        Transfer transfer1 = new Transfer(accountId, "ACC-456", new BigDecimal("75.00"), 
                LocalDateTime.of(2025, 1, 10, 9, 0));
        transfer1 = createTransferWithId(transfer1, 3L);

        Transfer transfer2 = new Transfer(accountId, "ACC-789", new BigDecimal("25.00"), 
                LocalDateTime.of(2025, 1, 25, 16, 45));
        transfer2 = createTransferWithId(transfer2, 4L);

        List<Transfer> transfers = Arrays.asList(transfer1, transfer2);
        when(billingRepository.findTransfersForAccountInDateRange(eq(accountId), any(), any()))
                .thenReturn(transfers);

        BillingResponse response = billingQueryService.getBillingForAccount(accountId, fromDate, toDate);

        assertEquals(2, response.getTransfers().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalIncomingAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(response.getTotalOutgoingAmount()));

        TransferItem item1 = response.getTransfers().get(0);
        assertEquals(TransferDirection.OUTGOING, item1.getDirection());
        assertEquals(accountId, item1.getSourceAccountId());
        assertEquals("ACC-456", item1.getDestinationAccountId());
    }

    @Test
    void mixedTransfers_calculatesCorrectTotals() {
        when(accountRepository.existsById(accountId)).thenReturn(true);

        Transfer incoming = new Transfer("ACC-456", accountId, new BigDecimal("200.00"), 
                LocalDateTime.of(2025, 1, 5, 11, 0));
        incoming = createTransferWithId(incoming, 5L);

        Transfer outgoing = new Transfer(accountId, "ACC-789", new BigDecimal("150.00"), 
                LocalDateTime.of(2025, 1, 15, 13, 30));
        outgoing = createTransferWithId(outgoing, 6L);

        List<Transfer> transfers = Arrays.asList(incoming, outgoing);
        when(billingRepository.findTransfersForAccountInDateRange(eq(accountId), any(), any()))
                .thenReturn(transfers);

        BillingResponse response = billingQueryService.getBillingForAccount(accountId, fromDate, toDate);

        assertEquals(2, response.getTransfers().size());
        assertEquals(0, new BigDecimal("200.00").compareTo(response.getTotalIncomingAmount()));
        assertEquals(0, new BigDecimal("150.00").compareTo(response.getTotalOutgoingAmount()));
    }

    private Transfer createTransferWithId(Transfer transfer, Long id) {
        Transfer transferWithId = new Transfer(transfer.getFromAccount(), transfer.getToAccount(), 
                transfer.getAmount(), transfer.getDate());
        try {
            java.lang.reflect.Field idField = Transfer.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(transferWithId, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set transfer ID", e);
        }
        return transferWithId;
    }
}
package com.example.transfer;

import com.example.transfer.domain.Account;
import com.example.transfer.domain.Transfer;
import com.example.transfer.repository.AccountRepository;
import com.example.transfer.repository.TransferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BillingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @BeforeEach
    void setUp() {
        transferRepository.deleteAll();
        accountRepository.deleteAll();

        accountRepository.save(new Account("ACC-001", new BigDecimal("1000.00")));
        accountRepository.save(new Account("ACC-002", new BigDecimal("500.00")));
        accountRepository.save(new Account("ACC-003", new BigDecimal("200.00")));

        // Create test transfers
        transferRepository.save(new Transfer("ACC-002", "ACC-001", new BigDecimal("100.00"), 
                LocalDateTime.of(2025, 1, 15, 10, 0)));
        transferRepository.save(new Transfer("ACC-001", "ACC-003", new BigDecimal("50.00"), 
                LocalDateTime.of(2025, 1, 20, 14, 30)));
        transferRepository.save(new Transfer("ACC-003", "ACC-001", new BigDecimal("75.00"), 
                LocalDateTime.of(2025, 1, 25, 9, 15)));
        // Transfer outside date range
        transferRepository.save(new Transfer("ACC-001", "ACC-002", new BigDecimal("200.00"), 
                LocalDateTime.of(2024, 12, 15, 16, 45)));
    }

    @Test
    void happyPath_returnsCorrectBillingData() throws Exception {
        mockMvc.perform(get("/accounts/ACC-001/billings")
                        .param("fromDate", "2025-01-01")
                        .param("toDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-001"))
                .andExpect(jsonPath("$.fromDate").value("2025-01-01"))
                .andExpect(jsonPath("$.toDate").value("2025-01-31"))
                .andExpect(jsonPath("$.transfers").isArray())
                .andExpect(jsonPath("$.transfers").value(org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$.totalIncomingAmount").value(175.00))
                .andExpect(jsonPath("$.totalOutgoingAmount").value(50.00));
    }

    @Test
    void incomingTransfersOnly_returnsCorrectData() throws Exception {
        mockMvc.perform(get("/accounts/ACC-003/billings")
                        .param("fromDate", "2025-01-01")
                        .param("toDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncomingAmount").value(50.00))
                .andExpect(jsonPath("$.totalOutgoingAmount").value(75.00))
                .andExpect(jsonPath("$.transfers").value(org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void noTransfersInDateRange_returnsEmptyResult() throws Exception {
        mockMvc.perform(get("/accounts/ACC-001/billings")
                        .param("fromDate", "2025-02-01")
                        .param("toDate", "2025-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transfers").value(org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.totalIncomingAmount").value(0))
                .andExpect(jsonPath("$.totalOutgoingAmount").value(0));
    }

    @Test
    void nonExistentAccount_returns404() throws Exception {
        mockMvc.perform(get("/accounts/GHOST/billings")
                        .param("fromDate", "2025-01-01")
                        .param("toDate", "2025-01-31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidDateRange_returns400() throws Exception {
        mockMvc.perform(get("/accounts/ACC-001/billings")
                        .param("fromDate", "2025-02-01")
                        .param("toDate", "2025-01-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidDateFormat_returns400() throws Exception {
        mockMvc.perform(get("/accounts/ACC-001/billings")
                        .param("fromDate", "invalid-date")
                        .param("toDate", "2025-01-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingDateParameters_returns400() throws Exception {
        mockMvc.perform(get("/accounts/ACC-001/billings"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferDirections_correctlyIdentified() throws Exception {
        mockMvc.perform(get("/accounts/ACC-001/billings")
                        .param("fromDate", "2025-01-01")
                        .param("toDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transfers[?(@.sourceAccountId == 'ACC-002' && @.destinationAccountId == 'ACC-001')].direction").value("INCOMING"))
                .andExpect(jsonPath("$.transfers[?(@.sourceAccountId == 'ACC-001' && @.destinationAccountId == 'ACC-003')].direction").value("OUTGOING"));
    }
}
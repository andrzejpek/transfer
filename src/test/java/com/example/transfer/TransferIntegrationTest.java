package com.example.transfer;

import com.example.transfer.domain.Account;
import com.example.transfer.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        accountRepository.save(new Account("ACC-001", new BigDecimal("1000.00")));
        accountRepository.save(new Account("ACC-002", new BigDecimal("500.00")));
    }

    @Test
    void happyPath_transferSucceeds() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "fromAccount", "ACC-001",
                "toAccount", "ACC-002",
                "amount", "200.00"
        );

        mockMvc.perform(post("/transfers")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.transferId").isNumber());
    }

    @Test
    void idempotency_sameKeyAndPayload_returnsCachedResponse() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "fromAccount", "ACC-001",
                "toAccount", "ACC-002",
                "amount", "100.00"
        );
        String content = objectMapper.writeValueAsString(body);

        String firstResponse = mockMvc.perform(post("/transfers")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String secondResponse = mockMvc.perform(post("/transfers")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"))
                .andReturn().getResponse().getContentAsString();

        Long firstId = objectMapper.readTree(firstResponse).get("transferId").asLong();
        Long secondId = objectMapper.readTree(secondResponse).get("transferId").asLong();
        assert firstId.equals(secondId) : "transferId must match on duplicate call";
    }

    @Test
    void idempotency_sameKeyDifferentPayload_returns409() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/transfers")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromAccount", "ACC-001",
                                "toAccount", "ACC-002",
                                "amount", "100.00"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/transfers")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromAccount", "ACC-001",
                                "toAccount", "ACC-002",
                                "amount", "999.00"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void missingIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromAccount", "ACC-001",
                                "toAccount", "ACC-002",
                                "amount", "100.00"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonExistentFromAccount_returns404() throws Exception {
        mockMvc.perform(post("/transfers")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromAccount", "GHOST",
                                "toAccount", "ACC-002",
                                "amount", "100.00"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void insufficientFunds_returns422() throws Exception {
        mockMvc.perform(post("/transfers")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromAccount", "ACC-001",
                                "toAccount", "ACC-002",
                                "amount", "9999.00"
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void sameAccount_returns400() throws Exception {
        mockMvc.perform(post("/transfers")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromAccount", "ACC-001",
                                "toAccount", "ACC-001",
                                "amount", "50.00"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void negativeAmount_returns400() throws Exception {
        mockMvc.perform(post("/transfers")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromAccount", "ACC-001",
                                "toAccount", "ACC-002",
                                "amount", "-10.00"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
package com.example.transfer.util;

import com.example.transfer.dto.TransferRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RequestHashUtilTest {

    @Test
    void sameInputProducesSameHash() {
        TransferRequest r1 = new TransferRequest("A", "B", new BigDecimal("100.00"));
        TransferRequest r2 = new TransferRequest("A", "B", new BigDecimal("100.00"));
        assertEquals(RequestHashUtil.compute(r1), RequestHashUtil.compute(r2));
    }

    @Test
    void differentFromAccountProducesDifferentHash() {
        TransferRequest r1 = new TransferRequest("A", "B", new BigDecimal("100.00"));
        TransferRequest r2 = new TransferRequest("X", "B", new BigDecimal("100.00"));
        assertNotEquals(RequestHashUtil.compute(r1), RequestHashUtil.compute(r2));
    }

    @Test
    void differentToAccountProducesDifferentHash() {
        TransferRequest r1 = new TransferRequest("A", "B", new BigDecimal("100.00"));
        TransferRequest r2 = new TransferRequest("A", "Y", new BigDecimal("100.00"));
        assertNotEquals(RequestHashUtil.compute(r1), RequestHashUtil.compute(r2));
    }

    @Test
    void differentAmountProducesDifferentHash() {
        TransferRequest r1 = new TransferRequest("A", "B", new BigDecimal("100.00"));
        TransferRequest r2 = new TransferRequest("A", "B", new BigDecimal("200.00"));
        assertNotEquals(RequestHashUtil.compute(r1), RequestHashUtil.compute(r2));
    }

    @Test
    void hashIsNonNull() {
        TransferRequest r = new TransferRequest("A", "B", new BigDecimal("50.00"));
        assertNotNull(RequestHashUtil.compute(r));
    }

    @Test
    void hashIs64HexChars() {
        TransferRequest r = new TransferRequest("A", "B", new BigDecimal("50.00"));
        assertEquals(64, RequestHashUtil.compute(r).length());
    }
}
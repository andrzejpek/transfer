package com.example.transfer.controller;

import com.example.transfer.dto.BillingResponse;
import com.example.transfer.service.BillingQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/accounts")
public class BillingController {

    private final BillingQueryService billingQueryService;

    public BillingController(BillingQueryService billingQueryService) {
        this.billingQueryService = billingQueryService;
    }

    @GetMapping("/{accountId}/billings")
    public ResponseEntity<BillingResponse> getBilling(
            @PathVariable String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        BillingResponse response = billingQueryService.getBillingForAccount(accountId, fromDate, toDate);
        return ResponseEntity.ok(response);
    }
}
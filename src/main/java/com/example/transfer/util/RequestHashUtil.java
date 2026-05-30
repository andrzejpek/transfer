package com.example.transfer.util;

import com.example.transfer.dto.TransferRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RequestHashUtil {

    private RequestHashUtil() {
    }

    public static String compute(TransferRequest request) {
        String input = request.getFromAccount() + ":"
                + request.getToAccount() + ":"
                + request.getAmount().stripTrailingZeros().toPlainString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
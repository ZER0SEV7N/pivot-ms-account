package com.pe.pivot.account.infrastructure.util;

/**
 * Utility class strictly responsible for masking sensitive Personally Identifiable Information (PII)
 * and account numbers in application logs.
 * Adheres to the Single Responsibility Principle (SRP).
 */
public final class DataMaskingUtil {

    private DataMaskingUtil() { } // Private constructor to prevent instantiation


    public static String maskAccountNumber(String accountNumber) {
        if(accountNumber == null || accountNumber.length() < 4) {
            return "****"; // Return masked if account number is too short or null
        }
        int visibleChars = 4;
        String tail = accountNumber.substring(accountNumber.length() - visibleChars);
        return "*".repeat(accountNumber.length() - visibleChars) + tail;
    }
}

package com.pe.pivot.account.infrastructure.util;

/**
 * Utility class strictly responsible for masking sensitive Personally Identifiable Information
 * (PII) and account numbers in application logs. Adheres to the Single Responsibility Principle
 * (SRP).
 */
public final class DataMaskingUtil {

  // Private constructor to prevent instantiation.
  private DataMaskingUtil() {
  }

  /**
   * Masks an account number by replacing all but the last four digits with asterisks. If the
   * string has more than 4 characters, it will return a string with the last 4 characters visible
   * and the rest masked. If the String has less than 4 characters, it will return "****" to
   * indicate that the number is too short to display.
   *
   * @param accountNumber - the account number to be masked
   * @return - the masked account number, or "****" if the input is null or too short
   */
  public static String maskAccountNumber(String accountNumber) {
    if (accountNumber == null || accountNumber.length() < 4) {
      return "****"; // Return masked if account number is too short or null
    }

    // Mask all but the last 4 characters of the account number
    int visibleChars = 4;
    String tail = accountNumber.substring(accountNumber.length() - visibleChars);
    return "*".repeat(accountNumber.length() - visibleChars) + tail;
  }
}

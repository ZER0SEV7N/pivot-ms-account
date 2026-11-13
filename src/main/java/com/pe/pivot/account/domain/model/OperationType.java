package com.pe.pivot.account.domain.model;

import com.pe.pivot.account.domain.exception.BusinessRuleException;

import java.math.BigDecimal;

/**
 * Rich Enumeration implementing the Strategy Pattern for balance arithmetic.
 * Eliminates conditional branching (if/else/switch) when applying deposits or withdrawals.
 */
public enum OperationType {

    /**
     * Represents a deposit operation on a bank account.
     * The new balance is calculated by adding the deposit amount and subtracting any applicable commission.
     * If the resulting balance is negative, a BusinessRuleException is thrown.
     */
    DEPOSIT {
        /**
         * @param currentBalance - The current balance of the bank account.
         * @param amount - The amount to be deposited.
         * @param commission - The commission fee to be applied to the deposit.
         * @return The new balance after the deposit and commission are applied.
         * @throws BusinessRuleException if the resulting balance is negative.
         */
        @Override
        public BigDecimal calculateNewBalance(BigDecimal currentBalance, BigDecimal amount, BigDecimal commission) {
            BigDecimal netBalance = currentBalance.add(amount).subtract(commission);
            if (netBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("Deposit amount is insufficient to cover the transaction commission fee (" + commission + ").");
            }
            return netBalance;
        }
    },

    /**
     * Represents a withdrawal operation on a bank account.
     * The new balance is calculated by subtracting the withdrawal amount and any applicable commission from the current balance.
     * If the current balance is insufficient to cover the total required amount (withdrawal + commission), a BusinessRuleException is thrown.
     */
    WITHDRAWAL {
        /**
         * Calculates the new balance after a withdrawal operation.
         * @param currentBalance - The current balance of the bank account.
         * @param amount - The amount to be withdrawn.
         * @param commission - The commission fee to be applied to the withdrawal.
         * @return - The new balance after the withdrawal and commission are applied.
         * @throws BusinessRuleException if the current balance is insufficient to cover the total required amount (withdrawal + commission).
         */
        @Override
        public BigDecimal calculateNewBalance(BigDecimal currentBalance, BigDecimal amount, BigDecimal commission) {
            BigDecimal totalRequired = amount.add(commission);
            if (currentBalance.compareTo(totalRequired) < 0) {
                throw new BusinessRuleException(
                        "Insufficient funds. Required (amount + commission): " + totalRequired
                                + ", Available: " + currentBalance);
            }
            return currentBalance.subtract(totalRequired);
        }
    };

    /**
     * Calculates the new balance after applying the operation (deposit or withdrawal) with the given amount and commission.
     * @param currentBalance - The current balance of the bank account.
     * @param amount - The amount to be deposited or withdrawn.
     * @param commission - The commission fee to be applied to the operation.
     * @return - The new balance after the operation and commission are applied.
     * @throws BusinessRuleException if the current balance is insufficient to cover the total required amount (withdrawal + commission).
     */
    public abstract BigDecimal calculateNewBalance(BigDecimal currentBalance, BigDecimal amount, BigDecimal commission);
}
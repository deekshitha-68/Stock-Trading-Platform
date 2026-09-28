package com.codealpha.stocktrading.exception;

/**
 * Thrown when a user attempts to buy stocks but does not have
 * enough virtual cash to cover the total purchase cost.
 */
public class InsufficientBalanceException extends Exception {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}

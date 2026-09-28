package com.codealpha.stocktrading.exception;

/**
 * Thrown when a user attempts to sell more shares of a stock
 * than they currently own in their portfolio.
 */
public class InsufficientSharesException extends Exception {

    public InsufficientSharesException(String message) {
        super(message);
    }
}

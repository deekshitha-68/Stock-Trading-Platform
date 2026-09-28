package com.codealpha.stocktrading.exception;

/**
 * Thrown when a user references a stock ticker symbol that does not
 * exist in the simulated market.
 */
public class InvalidStockSymbolException extends Exception {

    public InvalidStockSymbolException(String message) {
        super(message);
    }
}

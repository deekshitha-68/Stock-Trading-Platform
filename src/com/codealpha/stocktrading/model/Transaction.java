package com.codealpha.stocktrading.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Immutable record of a single trade (buy or sell).
 * 
 * Every field is set at construction time and never modified, making
 * Transaction objects safe to store in collections without defensive copies.
 * 
 * Uses BigDecimal for monetary values and LocalDateTime for the timestamp.
 */
public class Transaction {

    private final TransactionType type;
    private final String stockSymbol;
    private final int quantity;
    private final BigDecimal pricePerShare;
    private final BigDecimal totalAmount;
    private final LocalDateTime timestamp;

    /**
     * Constructs a Transaction.
     *
     * @param type          BUY or SELL
     * @param stockSymbol   the ticker symbol traded
     * @param quantity      the number of shares traded
     * @param pricePerShare the price per share at the time of the trade
     * @param totalAmount   the total monetary value of the trade (price × qty)
     * @param timestamp     when the trade was executed
     */
    public Transaction(TransactionType type, String stockSymbol, int quantity,
                       BigDecimal pricePerShare, BigDecimal totalAmount,
                       LocalDateTime timestamp) {
        this.type = type;
        this.stockSymbol = stockSymbol.toUpperCase();
        this.quantity = quantity;
        this.pricePerShare = pricePerShare.setScale(2, RoundingMode.HALF_UP);
        this.totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        this.timestamp = timestamp;
    }

    // ── Getters (all read-only) ──────────────────────────────────────

    public TransactionType getType() {
        return type;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPricePerShare() {
        return pricePerShare;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("%s  %-4s  %-6s  qty=%-5d  @$%-10s  total=$%-12s",
                timestamp, type, stockSymbol, quantity, pricePerShare, totalAmount);
    }
}

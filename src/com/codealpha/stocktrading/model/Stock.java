package com.codealpha.stocktrading.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Represents a single tradable stock in the simulated market.
 * 
 * Holds the stock's ticker symbol, company name, current price, and previous
 * price (used to calculate the percentage change displayed in the market view).
 * 
 * All monetary values use BigDecimal to avoid floating-point precision issues.
 */
public class Stock {

    private final String symbol;
    private final String companyName;
    private BigDecimal currentPrice;
    private BigDecimal previousPrice;

    /**
     * Constructs a Stock with the given identity and initial price.
     *
     * @param symbol      the ticker symbol (e.g., "AAPL")
     * @param companyName the full company name (e.g., "Apple Inc.")
     * @param initialPrice the starting simulated price
     */
    public Stock(String symbol, String companyName, BigDecimal initialPrice) {
        this.symbol = symbol.toUpperCase();
        this.companyName = companyName;
        this.currentPrice = initialPrice.setScale(2, RoundingMode.HALF_UP);
        this.previousPrice = this.currentPrice; // no change yet
    }

    // ── Getters ──────────────────────────────────────────────────────

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getPreviousPrice() {
        return previousPrice;
    }

    // ── Price mutation ───────────────────────────────────────────────

    /**
     * Updates the stock's price. Stores the old price as previousPrice
     * so the UI can display the price change.
     *
     * @param newPrice the new simulated price (must be > 0)
     */
    public void setCurrentPrice(BigDecimal newPrice) {
        this.previousPrice = this.currentPrice;
        this.currentPrice = newPrice.setScale(2, RoundingMode.HALF_UP);
    }

    // ── Derived calculations ─────────────────────────────────────────

    /**
     * Calculates the percentage change from previousPrice to currentPrice.
     *
     * @return the change as a percentage (e.g., 2.50 means +2.50%), or ZERO if previousPrice is zero
     */
    public BigDecimal getPriceChangePercent() {
        if (previousPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(previousPrice)
                .divide(previousPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return String.format("%-6s %-25s $%10s", symbol, companyName, currentPrice);
    }
}

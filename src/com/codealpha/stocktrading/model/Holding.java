package com.codealpha.stocktrading.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Represents a user's position in a single stock.
 * 
 * Tracks the number of shares owned and the total cost basis (sum of all
 * purchase amounts for those shares). From these two values we can derive:
 *   - average cost per share  = totalCostBasis / quantity
 *   - unrealized P/L          = (currentPrice - avgCost) × quantity
 *   - current market value     = currentPrice × quantity
 * 
 * When shares are sold, the cost basis is reduced proportionally using the
 * average-cost method.
 */
public class Holding {

    private final String stockSymbol;
    private int quantity;
    private BigDecimal totalCostBasis; // sum of (price × qty) for all buys still held

    /**
     * Creates a new Holding.
     *
     * @param stockSymbol the ticker symbol this holding is for
     * @param quantity    the initial number of shares
     * @param costBasis   the total purchase cost for those initial shares
     */
    public Holding(String stockSymbol, int quantity, BigDecimal costBasis) {
        this.stockSymbol = stockSymbol.toUpperCase();
        this.quantity = quantity;
        this.totalCostBasis = costBasis.setScale(2, RoundingMode.HALF_UP);
    }

    // ── Getters ──────────────────────────────────────────────────────

    public String getStockSymbol() {
        return stockSymbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalCostBasis() {
        return totalCostBasis;
    }

    // ── Derived calculations ─────────────────────────────────────────

    /**
     * @return the average price paid per share (totalCostBasis / quantity)
     */
    public BigDecimal getAverageCostPerShare() {
        if (quantity == 0) {
            return BigDecimal.ZERO;
        }
        return totalCostBasis.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    /**
     * @param currentPrice the stock's current market price
     * @return the total market value of this holding (currentPrice × quantity)
     */
    public BigDecimal getMarketValue(BigDecimal currentPrice) {
        return currentPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * @param currentPrice the stock's current market price
     * @return unrealized profit/loss = marketValue − totalCostBasis
     */
    public BigDecimal getUnrealizedPL(BigDecimal currentPrice) {
        return getMarketValue(currentPrice).subtract(totalCostBasis);
    }

    // ── Mutators ─────────────────────────────────────────────────────

    /**
     * Adds shares to this holding (called on a BUY).
     *
     * @param additionalQty   number of new shares purchased
     * @param additionalCost  total cost of the new shares (price × qty)
     */
    public void addShares(int additionalQty, BigDecimal additionalCost) {
        this.quantity += additionalQty;
        this.totalCostBasis = this.totalCostBasis.add(additionalCost).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Removes shares from this holding (called on a SELL).
     * Reduces the cost basis proportionally using the average-cost method.
     *
     * @param sellQty the number of shares to sell
     * @return the cost basis freed by this sale (avgCost × sellQty), useful for
     *         calculating realized P/L
     */
    public BigDecimal removeShares(int sellQty) {
        BigDecimal avgCost = getAverageCostPerShare();
        BigDecimal costFreed = avgCost.multiply(BigDecimal.valueOf(sellQty)).setScale(2, RoundingMode.HALF_UP);
        this.quantity -= sellQty;
        this.totalCostBasis = this.totalCostBasis.subtract(costFreed).setScale(2, RoundingMode.HALF_UP);

        // Guard against tiny negative residuals from rounding
        if (this.quantity == 0) {
            this.totalCostBasis = BigDecimal.ZERO;
        }
        return costFreed;
    }

    @Override
    public String toString() {
        return String.format("Holding{%s, qty=%d, costBasis=%s}", stockSymbol, quantity, totalCostBasis);
    }
}

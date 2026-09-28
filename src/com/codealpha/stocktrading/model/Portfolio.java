package com.codealpha.stocktrading.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates a user's investment data: their stock holdings and transaction history.
 * 
 * Holdings are stored in a HashMap keyed by stock symbol for O(1) lookup.
 * Transactions are stored in an ArrayList in chronological order.
 * 
 * The portfolio does NOT hold a reference to the stock market; it stores
 * only positions and history. Market prices are passed in when computing
 * values, keeping Portfolio independent of the market data source.
 */
public class Portfolio {

    private final Map<String, Holding> holdings;   // symbol → Holding
    private final List<Transaction> transactions;  // chronological order

    /**
     * Constructs an empty portfolio.
     */
    public Portfolio() {
        this.holdings = new HashMap<>();
        this.transactions = new ArrayList<>();
    }

    // ── Holdings management ──────────────────────────────────────────

    /**
     * Returns the holding for the given symbol, or null if the user doesn't own it.
     */
    public Holding getHolding(String symbol) {
        return holdings.get(symbol.toUpperCase());
    }

    /**
     * Returns an unmodifiable view of all holdings.
     */
    public Map<String, Holding> getHoldings() {
        return Collections.unmodifiableMap(holdings);
    }

    /**
     * Adds shares to an existing holding or creates a new one.
     *
     * @param symbol   the stock symbol
     * @param quantity the number of shares purchased
     * @param cost     the total cost of the purchase (price × qty)
     */
    public void addOrUpdateHolding(String symbol, int quantity, BigDecimal cost) {
        String key = symbol.toUpperCase();
        Holding existing = holdings.get(key);
        if (existing != null) {
            existing.addShares(quantity, cost);
        } else {
            holdings.put(key, new Holding(key, quantity, cost));
        }
    }

    /**
     * Removes a holding entirely (called when quantity reaches zero after a sell).
     */
    public void removeHolding(String symbol) {
        holdings.remove(symbol.toUpperCase());
    }

    // ── Transaction history ──────────────────────────────────────────

    /**
     * Records a transaction in the history.
     */
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    /**
     * Returns an unmodifiable view of all transactions in chronological order.
     */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    // ── Aggregate calculations ───────────────────────────────────────

    /**
     * Computes the total market value of all holdings using the supplied
     * current prices.
     *
     * @param currentPrices a map of symbol → current price
     * @return the sum of (currentPrice × quantity) for every holding
     */
    public BigDecimal getTotalMarketValue(Map<String, BigDecimal> currentPrices) {
        BigDecimal total = BigDecimal.ZERO;
        for (Holding h : holdings.values()) {
            BigDecimal price = currentPrices.getOrDefault(h.getStockSymbol(), BigDecimal.ZERO);
            total = total.add(h.getMarketValue(price));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Computes the total unrealized profit/loss across all holdings.
     *
     * @param currentPrices a map of symbol → current price
     * @return the sum of unrealized P/L for every holding
     */
    public BigDecimal getTotalUnrealizedPL(Map<String, BigDecimal> currentPrices) {
        BigDecimal total = BigDecimal.ZERO;
        for (Holding h : holdings.values()) {
            BigDecimal price = currentPrices.getOrDefault(h.getStockSymbol(), BigDecimal.ZERO);
            total = total.add(h.getUnrealizedPL(price));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Computes the total cost basis across all holdings.
     */
    public BigDecimal getTotalCostBasis() {
        BigDecimal total = BigDecimal.ZERO;
        for (Holding h : holdings.values()) {
            total = total.add(h.getTotalCostBasis());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return String.format("Portfolio{holdings=%d, transactions=%d}", holdings.size(), transactions.size());
    }
}

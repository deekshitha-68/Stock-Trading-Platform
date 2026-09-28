package com.codealpha.stocktrading.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Represents the registered trader/user in the simulation.
 * 
 * Each user has a username, a virtual cash balance (BigDecimal), and
 * exactly one Portfolio that tracks their holdings and transactions.
 * 
 * The User class provides methods to deduct and credit the cash balance,
 * as well as to compute the total account value (cash + holdings).
 */
public class User {

    private final String username;
    private BigDecimal cashBalance;
    private final Portfolio portfolio;

    /**
     * Constructs a new User with the given name and starting balance.
     *
     * @param username       the display name of the user
     * @param startingBalance the initial virtual cash balance
     */
    public User(String username, BigDecimal startingBalance) {
        this.username = username;
        this.cashBalance = startingBalance.setScale(2, RoundingMode.HALF_UP);
        this.portfolio = new Portfolio();
    }

    /**
     * Constructs a User with an existing portfolio (used when loading from file).
     */
    public User(String username, BigDecimal cashBalance, Portfolio portfolio) {
        this.username = username;
        this.cashBalance = cashBalance.setScale(2, RoundingMode.HALF_UP);
        this.portfolio = portfolio;
    }

    // ── Getters ──────────────────────────────────────────────────────

    public String getUsername() {
        return username;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    // ── Balance operations ───────────────────────────────────────────

    /**
     * Deducts an amount from the cash balance (called on BUY).
     *
     * @param amount the amount to deduct
     * @throws IllegalArgumentException if amount is negative
     */
    public void deductBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot deduct a negative amount.");
        }
        this.cashBalance = this.cashBalance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Credits an amount to the cash balance (called on SELL).
     *
     * @param amount the amount to add
     * @throws IllegalArgumentException if amount is negative
     */
    public void addBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot add a negative amount.");
        }
        this.cashBalance = this.cashBalance.add(amount).setScale(2, RoundingMode.HALF_UP);
    }

    // ── Derived calculations ─────────────────────────────────────────

    /**
     * Computes the total account value: cash balance + market value of all holdings.
     *
     * @param currentPrices a map of symbol → current market price
     * @return cash + total holdings market value
     */
    public BigDecimal getTotalAccountValue(Map<String, BigDecimal> currentPrices) {
        return cashBalance.add(portfolio.getTotalMarketValue(currentPrices)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return String.format("User{%s, balance=$%s}", username, cashBalance);
    }
}

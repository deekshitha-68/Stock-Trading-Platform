package com.codealpha.stocktrading.service;

import com.codealpha.stocktrading.exception.InsufficientBalanceException;
import com.codealpha.stocktrading.exception.InsufficientSharesException;
import com.codealpha.stocktrading.exception.InvalidStockSymbolException;
import com.codealpha.stocktrading.model.*;
import com.codealpha.stocktrading.persistence.FileStorageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Core orchestrator for the Stock Trading Platform.
 * 
 * Responsibilities:
 *   - Initialize the simulated stock market (10 fake stocks).
 *   - Register users.
 *   - Execute buy and sell operations with full validation.
 *   - Delegate price simulation to PriceSimulator.
 *   - Delegate persistence to FileStorageService.
 *   - Provide query methods for the UI (market data, portfolio, history).
 * 
 * All business rules and validation are enforced here, keeping models
 * and UI layers clean.
 */
public class StockTradingSystem {

    private static final BigDecimal DEFAULT_STARTING_BALANCE = new BigDecimal("100000.00");

    private Map<String, Stock> stocks;
    private User user;
    private final PriceSimulator priceSimulator;
    private final FileStorageService storageService;

    /**
     * Constructs the trading system, initializes the price simulator
     * and storage service.
     */
    public StockTradingSystem() {
        this.priceSimulator = new PriceSimulator();
        this.storageService = new FileStorageService();
        this.stocks = new LinkedHashMap<>(); // preserve insertion order for display
        this.user = null;
    }

    // ── Initialization ───────────────────────────────────────────────

    /**
     * Initializes the market with 10 simulated stocks.
     * Called on first run when no save data exists.
     */
    public void initializeDefaultStocks() {
        addStock(new Stock("AAPL", "Apple Inc.", new BigDecimal("178.50")));
        addStock(new Stock("GOOGL", "Alphabet Inc.", new BigDecimal("141.25")));
        addStock(new Stock("MSFT", "Microsoft Corp.", new BigDecimal("378.90")));
        addStock(new Stock("AMZN", "Amazon.com Inc.", new BigDecimal("185.60")));
        addStock(new Stock("TSLA", "Tesla Inc.", new BigDecimal("248.75")));
        addStock(new Stock("META", "Meta Platforms Inc.", new BigDecimal("505.30")));
        addStock(new Stock("NVDA", "NVIDIA Corp.", new BigDecimal("875.40")));
        addStock(new Stock("JPM", "JPMorgan Chase & Co.", new BigDecimal("198.20")));
        addStock(new Stock("V", "Visa Inc.", new BigDecimal("279.15")));
        addStock(new Stock("JNJ", "Johnson & Johnson", new BigDecimal("156.80")));
    }

    private void addStock(Stock stock) {
        stocks.put(stock.getSymbol(), stock);
    }

    // ── User management ──────────────────────────────────────────────

    /**
     * Registers a new user with the given name and starting balance.
     *
     * @param username        the user's display name (must be non-empty)
     * @param startingBalance the initial virtual cash (must be positive)
     * @throws IllegalArgumentException if name is blank or balance is not positive
     */
    public void registerUser(String username, BigDecimal startingBalance) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        if (startingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Starting balance must be positive.");
        }
        this.user = new User(username.trim(), startingBalance);
    }

    /**
     * Registers a user with the default starting balance ($100,000).
     */
    public void registerUser(String username) {
        registerUser(username, DEFAULT_STARTING_BALANCE);
    }

    public boolean isUserRegistered() {
        return user != null;
    }

    public User getUser() {
        return user;
    }

    // ── Market data ──────────────────────────────────────────────────

    /**
     * Returns all stocks in the simulated market.
     */
    public Collection<Stock> getAllStocks() {
        return stocks.values();
    }

    /**
     * Returns a single stock by its symbol.
     *
     * @param symbol the ticker symbol
     * @return the Stock object
     * @throws InvalidStockSymbolException if the symbol doesn't exist
     */
    public Stock getStockBySymbol(String symbol) throws InvalidStockSymbolException {
        Stock stock = stocks.get(symbol.toUpperCase());
        if (stock == null) {
            throw new InvalidStockSymbolException(
                    "Stock symbol '" + symbol.toUpperCase() + "' not found in the market.");
        }
        return stock;
    }

    /**
     * Builds a map of symbol → current price, used by Portfolio for value calculations.
     */
    public Map<String, BigDecimal> getCurrentPriceMap() {
        Map<String, BigDecimal> prices = new HashMap<>();
        for (Stock stock : stocks.values()) {
            prices.put(stock.getSymbol(), stock.getCurrentPrice());
        }
        return prices;
    }

    // ── Trading operations ───────────────────────────────────────────

    /**
     * Executes a BUY order.
     * 
     * Flow:
     *   1. Validate symbol exists.
     *   2. Validate quantity > 0.
     *   3. Calculate total cost.
     *   4. Validate sufficient balance.
     *   5. Deduct cash.
     *   6. Add/update holding.
     *   7. Record transaction.
     *
     * @param symbol   the stock to buy
     * @param quantity the number of shares
     * @return a confirmation message string
     * @throws InvalidStockSymbolException if symbol not in market
     * @throws InsufficientBalanceException if cash < total cost
     * @throws IllegalArgumentException if quantity ≤ 0
     */
    public String buyStock(String symbol, int quantity)
            throws InvalidStockSymbolException, InsufficientBalanceException {

        // Validate symbol
        Stock stock = getStockBySymbol(symbol);

        // Validate quantity
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer.");
        }

        // Calculate total cost
        BigDecimal pricePerShare = stock.getCurrentPrice();
        BigDecimal totalCost = pricePerShare.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);

        // Validate sufficient balance
        if (user.getCashBalance().compareTo(totalCost) < 0) {
            throw new InsufficientBalanceException(String.format(
                    "Insufficient balance. Required: $%s, Available: $%s",
                    totalCost, user.getCashBalance()));
        }

        // Execute the trade
        user.deductBalance(totalCost);
        user.getPortfolio().addOrUpdateHolding(stock.getSymbol(), quantity, totalCost);

        // Record the transaction
        Transaction txn = new Transaction(
                TransactionType.BUY,
                stock.getSymbol(),
                quantity,
                pricePerShare,
                totalCost,
                LocalDateTime.now()
        );
        user.getPortfolio().addTransaction(txn);

        return String.format("✓ Bought %d shares of %s at $%s each. Total cost: $%s",
                quantity, stock.getSymbol(), pricePerShare, totalCost);
    }

    /**
     * Executes a SELL order.
     * 
     * Flow:
     *   1. Validate symbol exists.
     *   2. Validate quantity > 0.
     *   3. Validate user owns enough shares.
     *   4. Calculate total revenue.
     *   5. Remove shares from holding (proportionally reduce cost basis).
     *   6. Remove holding entirely if quantity reaches 0.
     *   7. Credit cash.
     *   8. Record transaction.
     *
     * @param symbol   the stock to sell
     * @param quantity the number of shares
     * @return a confirmation message string
     * @throws InvalidStockSymbolException if symbol not in market
     * @throws InsufficientSharesException if user doesn't own enough shares
     * @throws IllegalArgumentException if quantity ≤ 0
     */
    public String sellStock(String symbol, int quantity)
            throws InvalidStockSymbolException, InsufficientSharesException {

        // Validate symbol
        Stock stock = getStockBySymbol(symbol);

        // Validate quantity
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer.");
        }

        // Validate user owns enough shares
        Holding holding = user.getPortfolio().getHolding(stock.getSymbol());
        if (holding == null || holding.getQuantity() < quantity) {
            int owned = (holding == null) ? 0 : holding.getQuantity();
            throw new InsufficientSharesException(String.format(
                    "Insufficient shares. Requested to sell: %d, Currently owned: %d",
                    quantity, owned));
        }

        // Calculate total revenue
        BigDecimal pricePerShare = stock.getCurrentPrice();
        BigDecimal totalRevenue = pricePerShare.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);

        // Remove shares (average-cost method reduces cost basis proportionally)
        BigDecimal costFreed = holding.removeShares(quantity);

        // Remove holding if completely sold
        if (holding.getQuantity() == 0) {
            user.getPortfolio().removeHolding(stock.getSymbol());
        }

        // Credit cash
        user.addBalance(totalRevenue);

        // Record the transaction
        Transaction txn = new Transaction(
                TransactionType.SELL,
                stock.getSymbol(),
                quantity,
                pricePerShare,
                totalRevenue,
                LocalDateTime.now()
        );
        user.getPortfolio().addTransaction(txn);

        // Calculate realized P/L for this sale
        BigDecimal realizedPL = totalRevenue.subtract(costFreed);
        String plSign = realizedPL.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";

        return String.format("✓ Sold %d shares of %s at $%s each. Total revenue: $%s (Realized P/L: %s$%s)",
                quantity, stock.getSymbol(), pricePerShare, totalRevenue, plSign, realizedPL);
    }

    // ── Price simulation ─────────────────────────────────────────────

    /**
     * Triggers one tick of price simulation for all stocks.
     */
    public void simulatePriceTick() {
        priceSimulator.simulateTick(stocks.values());
    }

    // ── Persistence ──────────────────────────────────────────────────

    /**
     * Saves all state (stocks, user, portfolio) to CSV files.
     */
    public void saveState() {
        try {
            storageService.saveStocks(stocks.values());
            if (user != null) {
                storageService.saveUser(user);
                storageService.saveHoldings(user.getPortfolio().getHoldings().values());
                storageService.saveTransactions(user.getPortfolio().getTransactions());
            }
            System.out.println("✓ Data saved successfully.");
        } catch (Exception e) {
            System.out.println("⚠ Error saving data: " + e.getMessage());
        }
    }

    /**
     * Loads state from CSV files if they exist.
     *
     * @return true if data was loaded, false if no save files found
     */
    public boolean loadState() {
        if (!storageService.dataExists()) {
            return false;
        }
        try {
            // Load stocks
            Map<String, Stock> loadedStocks = storageService.loadStocks();
            if (!loadedStocks.isEmpty()) {
                this.stocks = new LinkedHashMap<>(loadedStocks);
            }

            // Load user (with holdings and transactions)
            User loadedUser = storageService.loadUser(this.stocks);
            if (loadedUser != null) {
                this.user = loadedUser;
            }

            return true;
        } catch (Exception e) {
            System.out.println("⚠ Error loading saved data: " + e.getMessage());
            System.out.println("  Starting with fresh data.");
            return false;
        }
    }

    /**
     * Returns the default starting balance.
     */
    public BigDecimal getDefaultStartingBalance() {
        return DEFAULT_STARTING_BALANCE;
    }
}

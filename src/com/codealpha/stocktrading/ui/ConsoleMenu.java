package com.codealpha.stocktrading.ui;

import com.codealpha.stocktrading.exception.InsufficientBalanceException;
import com.codealpha.stocktrading.exception.InsufficientSharesException;
import com.codealpha.stocktrading.exception.InvalidStockSymbolException;
import com.codealpha.stocktrading.model.*;
import com.codealpha.stocktrading.service.StockTradingSystem;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Scanner;

/**
 * Console-based user interface for the Stock Trading Platform.
 * 
 * Handles all input/output: displays menus, reads user choices, validates
 * input format, calls StockTradingSystem methods, and displays results.
 * 
 * All business logic is delegated to StockTradingSystem; this class
 * only handles presentation and input parsing.
 */
public class ConsoleMenu {

    private static final String SEPARATOR = "═══════════════════════════════════════════════════════════════════════════";
    private static final String THIN_SEP  = "───────────────────────────────────────────────────────────────────────────";
    private static final DateTimeFormatter DISPLAY_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Scanner scanner;
    private final StockTradingSystem system;

    public ConsoleMenu(StockTradingSystem system) {
        this.scanner = new Scanner(System.in);
        this.system = system;
    }

    /**
     * Entry point for the console UI.
     * Handles registration if needed, then runs the main menu loop.
     */
    public void start() {
        printBanner();

        // Try to load saved data
        boolean loaded = system.loadState();
        if (loaded && system.isUserRegistered()) {
            System.out.println("✓ Welcome back, " + system.getUser().getUsername() + "!");
            System.out.println("  Saved data loaded successfully.");
        } else {
            // Initialize default stocks and register user
            system.initializeDefaultStocks();
            handleRegistration();
        }

        System.out.println();

        // Main menu loop
        boolean running = true;
        while (running) {
            showMainMenu();
            int choice = readMenuChoice(0, 8);

            switch (choice) {
                case 1 -> handleViewMarket();
                case 2 -> handleViewStockDetail();
                case 3 -> handleBuy();
                case 4 -> handleSell();
                case 5 -> handleViewPortfolio();
                case 6 -> handleViewTransactions();
                case 7 -> handleSimulateTick();
                case 8 -> {
                    handleSaveAndExit();
                    running = false;
                }
                case 0 -> {
                    System.out.println("Exiting without saving. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }

        scanner.close();
    }

    // ── Banner & Menu ────────────────────────────────────────────────

    private void printBanner() {
        System.out.println(SEPARATOR);
        System.out.println("     ╔═══════════════════════════════════════════════════════╗");
        System.out.println("     ║        STOCK TRADING PLATFORM — SIMULATOR            ║");
        System.out.println("     ║                  CodeAlpha Task 2                     ║");
        System.out.println("     ╚═══════════════════════════════════════════════════════╝");
        System.out.println(SEPARATOR);
        System.out.println("  ⚠  DISCLAIMER: This is a SIMULATION. No real money,");
        System.out.println("     no real stocks, no real trades. For educational use only.");
        System.out.println(SEPARATOR);
        System.out.println();
    }

    private void showMainMenu() {
        System.out.println();
        System.out.println(THIN_SEP);
        System.out.printf("  %s  |  Cash: $%s  |  Portfolio Value: $%s%n",
                system.getUser().getUsername(),
                system.getUser().getCashBalance(),
                system.getUser().getTotalAccountValue(system.getCurrentPriceMap()));
        System.out.println(THIN_SEP);
        System.out.println("  [1] View Market (All Stocks)");
        System.out.println("  [2] View Stock Details");
        System.out.println("  [3] Buy Stock");
        System.out.println("  [4] Sell Stock");
        System.out.println("  [5] View Portfolio");
        System.out.println("  [6] View Transaction History");
        System.out.println("  [7] Simulate Market Tick (Update Prices)");
        System.out.println("  [8] Save & Exit");
        System.out.println("  [0] Exit Without Saving");
        System.out.println(THIN_SEP);
        System.out.print("  Enter your choice: ");
    }

    // ── Registration ─────────────────────────────────────────────────

    private void handleRegistration() {
        System.out.println("  ── New User Registration ──");
        System.out.print("  Enter your name: ");
        String name = scanner.nextLine().trim();

        while (name.isEmpty()) {
            System.out.print("  Name cannot be empty. Enter your name: ");
            name = scanner.nextLine().trim();
        }

        System.out.printf("  Starting balance: $%s%n", system.getDefaultStartingBalance());
        System.out.print("  Enter custom balance (or press Enter for default): ");
        String balanceInput = scanner.nextLine().trim();

        BigDecimal balance;
        if (balanceInput.isEmpty()) {
            balance = system.getDefaultStartingBalance();
        } else {
            try {
                balance = new BigDecimal(balanceInput);
                if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("  Invalid balance. Using default.");
                    balance = system.getDefaultStartingBalance();
                }
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Using default balance.");
                balance = system.getDefaultStartingBalance();
            }
        }

        system.registerUser(name, balance);
        System.out.printf("  ✓ Welcome, %s! Your starting balance is $%s.%n",
                name, balance);
    }

    // ── Menu handlers ────────────────────────────────────────────────

    private void handleViewMarket() {
        System.out.println();
        System.out.println("  ══════════════════════════ SIMULATED MARKET DATA ══════════════════════════");
        System.out.printf("  %-8s %-26s %12s %10s%n", "Symbol", "Company", "Price", "Change %");
        System.out.println("  " + THIN_SEP.substring(2));

        for (Stock stock : system.getAllStocks()) {
            BigDecimal change = stock.getPriceChangePercent();
            String changeStr = (change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + change + "%";
            System.out.printf("  %-8s %-26s $%10s %10s%n",
                    stock.getSymbol(),
                    stock.getCompanyName(),
                    stock.getCurrentPrice(),
                    changeStr);
        }

        System.out.println("  " + THIN_SEP.substring(2));
        System.out.println("  (All prices are simulated — not real market data)");
    }

    private void handleViewStockDetail() {
        System.out.print("\n  Enter stock symbol: ");
        String symbol = scanner.nextLine().trim().toUpperCase();

        try {
            Stock stock = system.getStockBySymbol(symbol);
            BigDecimal change = stock.getPriceChangePercent();
            String changeStr = (change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + change + "%";

            System.out.println();
            System.out.println("  ── Stock Details ──");
            System.out.println("  Symbol:        " + stock.getSymbol());
            System.out.println("  Company:       " + stock.getCompanyName());
            System.out.println("  Current Price: $" + stock.getCurrentPrice());
            System.out.println("  Previous Price:$" + stock.getPreviousPrice());
            System.out.println("  Change:        " + changeStr);
            System.out.println("  (Simulated data only)");

            // Show user's position if they own this stock
            Holding h = system.getUser().getPortfolio().getHolding(symbol);
            if (h != null) {
                System.out.println();
                System.out.println("  ── Your Position ──");
                System.out.println("  Shares Owned:  " + h.getQuantity());
                System.out.println("  Avg Cost:      $" + h.getAverageCostPerShare());
                System.out.println("  Market Value:  $" + h.getMarketValue(stock.getCurrentPrice()));
                System.out.println("  Unrealized P/L:$" + h.getUnrealizedPL(stock.getCurrentPrice()));
            }
        } catch (InvalidStockSymbolException e) {
            System.out.println("  ✗ " + e.getMessage());
        }
    }

    private void handleBuy() {
        System.out.println("\n  ── Buy Stock ──");
        System.out.print("  Enter stock symbol: ");
        String symbol = scanner.nextLine().trim().toUpperCase();

        int quantity = readPositiveInt("  Enter quantity to buy: ");
        if (quantity <= 0) return; // user entered invalid input, message already shown

        try {
            String result = system.buyStock(symbol, quantity);
            System.out.println("  " + result);
        } catch (InvalidStockSymbolException | InsufficientBalanceException |
                 IllegalArgumentException e) {
            System.out.println("  ✗ " + e.getMessage());
        }
    }

    private void handleSell() {
        System.out.println("\n  ── Sell Stock ──");

        // Show current holdings first
        Map<String, Holding> holdings = system.getUser().getPortfolio().getHoldings();
        if (holdings.isEmpty()) {
            System.out.println("  You don't own any stocks to sell.");
            return;
        }

        System.out.println("  Your holdings:");
        for (Holding h : holdings.values()) {
            System.out.printf("    %s: %d shares%n", h.getStockSymbol(), h.getQuantity());
        }

        System.out.print("  Enter stock symbol to sell: ");
        String symbol = scanner.nextLine().trim().toUpperCase();

        int quantity = readPositiveInt("  Enter quantity to sell: ");
        if (quantity <= 0) return;

        try {
            String result = system.sellStock(symbol, quantity);
            System.out.println("  " + result);
        } catch (InvalidStockSymbolException | InsufficientSharesException |
                 IllegalArgumentException e) {
            System.out.println("  ✗ " + e.getMessage());
        }
    }

    private void handleViewPortfolio() {
        User user = system.getUser();
        Portfolio portfolio = user.getPortfolio();
        Map<String, BigDecimal> prices = system.getCurrentPriceMap();

        System.out.println();
        System.out.println("  ════════════════════════════════ PORTFOLIO ════════════════════════════════");
        System.out.println("  User: " + user.getUsername());
        System.out.println("  " + THIN_SEP.substring(2));

        Map<String, Holding> holdings = portfolio.getHoldings();
        if (holdings.isEmpty()) {
            System.out.println("  Your portfolio is empty. Buy some stocks to get started!");
        } else {
            System.out.printf("  %-8s %8s %12s %12s %14s %14s%n",
                    "Symbol", "Qty", "Avg Cost", "Curr Price", "Market Value", "Unrealized P/L");
            System.out.println("  " + THIN_SEP.substring(2));

            for (Holding h : holdings.values()) {
                BigDecimal price = prices.getOrDefault(h.getStockSymbol(), BigDecimal.ZERO);
                BigDecimal marketVal = h.getMarketValue(price);
                BigDecimal pl = h.getUnrealizedPL(price);
                String plStr = (pl.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + "$" + pl;

                System.out.printf("  %-8s %8d $%10s $%10s $%12s %14s%n",
                        h.getStockSymbol(),
                        h.getQuantity(),
                        h.getAverageCostPerShare(),
                        price,
                        marketVal,
                        plStr);
            }
        }

        System.out.println("  " + THIN_SEP.substring(2));

        BigDecimal holdingsValue = portfolio.getTotalMarketValue(prices);
        BigDecimal totalPL = portfolio.getTotalUnrealizedPL(prices);
        BigDecimal totalValue = user.getTotalAccountValue(prices);
        String plStr = (totalPL.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + "$" + totalPL;

        System.out.printf("  Cash Balance:          $%s%n", user.getCashBalance());
        System.out.printf("  Holdings Value:        $%s%n", holdingsValue);
        System.out.printf("  Total Account Value:   $%s%n", totalValue);
        System.out.printf("  Total Unrealized P/L:  %s%n", plStr);
        System.out.println("  " + THIN_SEP.substring(2));
    }

    private void handleViewTransactions() {
        var transactions = system.getUser().getPortfolio().getTransactions();

        System.out.println();
        System.out.println("  ═══════════════════════════ TRANSACTION HISTORY ═══════════════════════════");

        if (transactions.isEmpty()) {
            System.out.println("  No transactions yet. Make your first trade!");
        } else {
            System.out.printf("  %-20s %-6s %-8s %8s %12s %14s%n",
                    "Date/Time", "Type", "Symbol", "Qty", "Price", "Total");
            System.out.println("  " + THIN_SEP.substring(2));

            for (Transaction t : transactions) {
                System.out.printf("  %-20s %-6s %-8s %8d $%10s $%12s%n",
                        t.getTimestamp().format(DISPLAY_DT),
                        t.getType(),
                        t.getStockSymbol(),
                        t.getQuantity(),
                        t.getPricePerShare(),
                        t.getTotalAmount());
            }

            System.out.println("  " + THIN_SEP.substring(2));
            System.out.printf("  Total transactions: %d%n", transactions.size());
        }
    }

    private void handleSimulateTick() {
        system.simulatePriceTick();
        System.out.println("  ✓ Market prices updated! (Simulated random fluctuation)");
        handleViewMarket();
    }

    private void handleSaveAndExit() {
        System.out.println("\n  Saving data...");
        system.saveState();
        System.out.println("  Goodbye, " + system.getUser().getUsername() + "! See you next time.");
    }

    // ── Input helpers ────────────────────────────────────────────────

    /**
     * Reads a menu choice integer from the user, handling invalid input gracefully.
     *
     * @param min minimum valid value
     * @param max maximum valid value
     * @return the user's validated choice
     */
    private int readMenuChoice(int min, int max) {
        while (true) {
            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) {
                    return choice;
                }
                System.out.printf("  Please enter a number between %d and %d: ", min, max);
            } catch (NumberFormatException e) {
                System.out.print("  Invalid input. Please enter a number: ");
            }
        }
    }

    /**
     * Reads a positive integer from the user.
     * Returns -1 if the user enters invalid input after too many attempts.
     */
    private int readPositiveInt(String prompt) {
        int attempts = 0;
        while (attempts < 3) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value > 0) {
                    return value;
                }
                System.out.println("  ✗ Quantity must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("  ✗ Invalid input. Please enter a whole number.");
            }
            attempts++;
        }
        System.out.println("  ✗ Too many invalid attempts. Returning to menu.");
        return -1;
    }
}

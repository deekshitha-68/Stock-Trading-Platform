package com.codealpha.stocktrading.persistence;

import com.codealpha.stocktrading.model.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Handles saving and loading application state using simple CSV files.
 * 
 * File layout (all stored in the "data/" directory):
 *   - stocks.csv     : symbol,companyName,currentPrice,previousPrice
 *   - user.csv       : username,cashBalance
 *   - holdings.csv   : stockSymbol,quantity,totalCostBasis
 *   - transactions.csv: type,stockSymbol,quantity,pricePerShare,totalAmount,timestamp
 * 
 * On save: overwrites existing files with current state.
 * On load: reads files if they exist; returns defaults if they don't.
 * Corrupted files are caught with try-catch and logged — the app falls back
 * to default state rather than crashing.
 */
public class FileStorageService {

    private static final String DATA_DIR = "data";
    private static final String STOCKS_FILE = DATA_DIR + File.separator + "stocks.csv";
    private static final String USER_FILE = DATA_DIR + File.separator + "user.csv";
    private static final String HOLDINGS_FILE = DATA_DIR + File.separator + "holdings.csv";
    private static final String TRANSACTIONS_FILE = DATA_DIR + File.separator + "transactions.csv";

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ── Save methods ─────────────────────────────────────────────────

    /**
     * Saves all stocks to stocks.csv.
     */
    public void saveStocks(Collection<Stock> stocks) throws IOException {
        ensureDataDir();
        try (PrintWriter writer = new PrintWriter(new FileWriter(STOCKS_FILE))) {
            writer.println("symbol,companyName,currentPrice,previousPrice");
            for (Stock s : stocks) {
                writer.printf("%s,%s,%s,%s%n",
                        s.getSymbol(),
                        escapeCSV(s.getCompanyName()),
                        s.getCurrentPrice(),
                        s.getPreviousPrice());
            }
        }
    }

    /**
     * Saves user data to user.csv.
     */
    public void saveUser(User user) throws IOException {
        ensureDataDir();
        try (PrintWriter writer = new PrintWriter(new FileWriter(USER_FILE))) {
            writer.println("username,cashBalance");
            writer.printf("%s,%s%n", escapeCSV(user.getUsername()), user.getCashBalance());
        }
    }

    /**
     * Saves all holdings to holdings.csv.
     */
    public void saveHoldings(Collection<Holding> holdings) throws IOException {
        ensureDataDir();
        try (PrintWriter writer = new PrintWriter(new FileWriter(HOLDINGS_FILE))) {
            writer.println("stockSymbol,quantity,totalCostBasis");
            for (Holding h : holdings) {
                writer.printf("%s,%d,%s%n",
                        h.getStockSymbol(),
                        h.getQuantity(),
                        h.getTotalCostBasis());
            }
        }
    }

    /**
     * Saves all transactions to transactions.csv.
     */
    public void saveTransactions(List<Transaction> transactions) throws IOException {
        ensureDataDir();
        try (PrintWriter writer = new PrintWriter(new FileWriter(TRANSACTIONS_FILE))) {
            writer.println("type,stockSymbol,quantity,pricePerShare,totalAmount,timestamp");
            for (Transaction t : transactions) {
                writer.printf("%s,%s,%d,%s,%s,%s%n",
                        t.getType(),
                        t.getStockSymbol(),
                        t.getQuantity(),
                        t.getPricePerShare(),
                        t.getTotalAmount(),
                        t.getTimestamp().format(DT_FORMAT));
            }
        }
    }

    // ── Load methods ─────────────────────────────────────────────────

    /**
     * Checks if any save data exists.
     */
    public boolean dataExists() {
        return Files.exists(Paths.get(STOCKS_FILE)) || Files.exists(Paths.get(USER_FILE));
    }

    /**
     * Loads stocks from stocks.csv.
     *
     * @return a LinkedHashMap of symbol → Stock (preserves insertion order)
     */
    public Map<String, Stock> loadStocks() throws IOException {
        Map<String, Stock> stockMap = new LinkedHashMap<>();
        Path path = Paths.get(STOCKS_FILE);
        if (!Files.exists(path)) {
            return stockMap;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(STOCKS_FILE))) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = parseCSVLine(line);
                if (parts.length >= 4) {
                    String symbol = parts[0].trim();
                    String companyName = unescapeCSV(parts[1].trim());
                    BigDecimal currentPrice = new BigDecimal(parts[2].trim());
                    BigDecimal previousPrice = new BigDecimal(parts[3].trim());

                    Stock stock = new Stock(symbol, companyName, currentPrice);
                    // Restore previousPrice by setting current to previous then back
                    // (a small trick since setCurrentPrice stores old as previous)
                    stock.setCurrentPrice(previousPrice);
                    stock.setCurrentPrice(currentPrice);
                    stockMap.put(symbol.toUpperCase(), stock);
                }
            }
        }
        return stockMap;
    }

    /**
     * Loads user, holdings, and transactions from their respective CSV files.
     *
     * @param stocks the loaded stock map (needed for cross-referencing, not used directly here)
     * @return the reconstructed User, or null if no user file exists
     */
    public User loadUser(Map<String, Stock> stocks) throws IOException {
        Path userPath = Paths.get(USER_FILE);
        if (!Files.exists(userPath)) {
            return null;
        }

        // Load user basics
        String username = null;
        BigDecimal cashBalance = BigDecimal.ZERO;

        try (BufferedReader reader = new BufferedReader(new FileReader(USER_FILE))) {
            String line = reader.readLine(); // skip header
            line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                String[] parts = parseCSVLine(line.trim());
                username = unescapeCSV(parts[0].trim());
                cashBalance = new BigDecimal(parts[1].trim());
            }
        }

        if (username == null) {
            return null;
        }

        // Create portfolio and populate it
        Portfolio portfolio = new Portfolio();

        // Load holdings
        Path holdingsPath = Paths.get(HOLDINGS_FILE);
        if (Files.exists(holdingsPath)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(HOLDINGS_FILE))) {
                String line = reader.readLine(); // skip header
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    String[] parts = parseCSVLine(line);
                    if (parts.length >= 3) {
                        String symbol = parts[0].trim().toUpperCase();
                        int quantity = Integer.parseInt(parts[1].trim());
                        BigDecimal costBasis = new BigDecimal(parts[2].trim());
                        portfolio.addOrUpdateHolding(symbol, quantity, costBasis);
                    }
                }
            }
        }

        // Load transactions
        Path txnPath = Paths.get(TRANSACTIONS_FILE);
        if (Files.exists(txnPath)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(TRANSACTIONS_FILE))) {
                String line = reader.readLine(); // skip header
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    String[] parts = parseCSVLine(line);
                    if (parts.length >= 6) {
                        TransactionType type = TransactionType.valueOf(parts[0].trim());
                        String symbol = parts[1].trim().toUpperCase();
                        int quantity = Integer.parseInt(parts[2].trim());
                        BigDecimal pricePerShare = new BigDecimal(parts[3].trim());
                        BigDecimal totalAmount = new BigDecimal(parts[4].trim());
                        LocalDateTime timestamp = LocalDateTime.parse(parts[5].trim(), DT_FORMAT);

                        Transaction txn = new Transaction(type, symbol, quantity,
                                pricePerShare, totalAmount, timestamp);
                        portfolio.addTransaction(txn);
                    }
                }
            }
        }

        return new User(username, cashBalance, portfolio);
    }

    // ── CSV helpers ──────────────────────────────────────────────────

    /**
     * Ensures the data directory exists.
     */
    private void ensureDataDir() throws IOException {
        Files.createDirectories(Paths.get(DATA_DIR));
    }

    /**
     * Escapes a value for CSV by wrapping in quotes if it contains commas.
     */
    private String escapeCSV(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Removes surrounding quotes from a CSV field.
     */
    private String unescapeCSV(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
            value = value.replace("\"\"", "\"");
        }
        return value;
    }

    /**
     * Parses a CSV line, respecting quoted fields that may contain commas.
     */
    private String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}

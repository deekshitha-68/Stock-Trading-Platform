/**
 * Stock Trading Platform — Service Layer
 * 
 * Contains all business logic, price simulation, and persistence:
 *   - PriceSimulator: random-walk price fluctuation engine
 *   - StorageService: localStorage JSON read/write wrapper
 *   - TradingSystem: central orchestrator (validation, buy/sell, state management)
 * 
 * SIMULATION ONLY — No real money, no real stocks.
 */

// ─── Price Simulator ──────────────────────────────────────
/**
 * Simulates market price movements using a random-walk algorithm.
 * Each tick adjusts prices by ±5%, with a $0.01 floor.
 */
class PriceSimulator {
    constructor() {
        this.maxChangePercent = 0.05;
        this.minPrice = 0.01;
    }

    simulateTick(stocks) {
        for (const stock of stocks) {
            const changeFraction = (Math.random() * 2 - 1) * this.maxChangePercent;
            const newPrice = stock.currentPrice * (1 + changeFraction);
            stock.setPrice(Math.max(this.minPrice, newPrice));
        }
    }
}

// ─── Storage Service ──────────────────────────────────────
/**
 * Handles reading/writing application state to localStorage.
 * All data is stored as JSON strings under prefixed keys.
 */
class StorageService {
    constructor() {
        this.PREFIX = 'stp_';
    }

    saveUser(user) {
        if (!user) return;
        localStorage.setItem(this.PREFIX + 'user', JSON.stringify(user.toJSON()));
    }

    saveStocks(stocks) {
        const arr = stocks.map(s => s.toJSON());
        localStorage.setItem(this.PREFIX + 'stocks', JSON.stringify(arr));
    }

    loadUser() {
        const data = localStorage.getItem(this.PREFIX + 'user');
        if (!data) return null;
        try {
            return User.fromJSON(JSON.parse(data));
        } catch (e) {
            console.warn('Failed to load user data:', e);
            return null;
        }
    }

    loadStocks() {
        const data = localStorage.getItem(this.PREFIX + 'stocks');
        if (!data) return null;
        try {
            return JSON.parse(data).map(s => Stock.fromJSON(s));
        } catch (e) {
            console.warn('Failed to load stock data:', e);
            return null;
        }
    }

    hasData() {
        return localStorage.getItem(this.PREFIX + 'user') !== null;
    }

    clearAll() {
        localStorage.removeItem(this.PREFIX + 'user');
        localStorage.removeItem(this.PREFIX + 'stocks');
    }
}

// ─── Trading System (Core Orchestrator) ───────────────────
/**
 * Central service class that orchestrates all platform operations.
 * 
 * Responsibilities:
 *   - Initialize simulated stock market (10 stocks)
 *   - Register users with virtual cash balance
 *   - Execute buy/sell with full validation
 *   - Trigger price simulation
 *   - Manage persistence via StorageService
 */
class TradingSystem {
    constructor() {
        this.stocks = [];
        this.stockMap = new Map();
        this.user = null;
        this.priceSimulator = new PriceSimulator();
        this.storage = new StorageService();
    }

    // ── Initialization ─────────────────────────────────────

    initializeDefaultStocks() {
        const defaults = [
            new Stock('AAPL', 'Apple Inc.', 178.50),
            new Stock('GOOGL', 'Alphabet Inc.', 141.25),
            new Stock('MSFT', 'Microsoft Corp.', 378.90),
            new Stock('AMZN', 'Amazon.com Inc.', 185.60),
            new Stock('TSLA', 'Tesla Inc.', 248.75),
            new Stock('META', 'Meta Platforms Inc.', 505.30),
            new Stock('NVDA', 'NVIDIA Corp.', 875.40),
            new Stock('JPM', 'JPMorgan Chase & Co.', 198.20),
            new Stock('V', 'Visa Inc.', 279.15),
            new Stock('JNJ', 'Johnson & Johnson', 156.80)
        ];
        this.stocks = defaults;
        this.stockMap.clear();
        for (const s of defaults) {
            this.stockMap.set(s.symbol, s);
        }
    }

    // ── User Management ────────────────────────────────────

    registerUser(username, startingBalance) {
        if (!username || username.trim() === '') {
            throw new Error('Username cannot be empty.');
        }
        if (startingBalance <= 0) {
            throw new Error('Starting balance must be positive.');
        }
        this.user = new User(username.trim(), startingBalance);
        this.save();
    }

    isLoggedIn() {
        return this.user !== null;
    }

    logout() {
        this.storage.clearAll();
        this.user = null;
    }

    // ── Market Data ────────────────────────────────────────

    getStock(symbol) {
        return this.stockMap.get(symbol.toUpperCase()) || null;
    }

    getAllStocks() {
        return this.stocks;
    }

    getCurrentPriceMap() {
        const map = new Map();
        for (const s of this.stocks) {
            map.set(s.symbol, s.currentPrice);
        }
        return map;
    }

    // ── Buy Operation ──────────────────────────────────────
    /**
     * Execute a BUY order with full validation.
     * 
     * Validation:
     *   1. Symbol must exist in market
     *   2. Quantity must be a positive integer
     *   3. User balance must cover total cost
     * 
     * On success: deducts cash, updates holding, logs transaction, saves state.
     */
    buyStock(symbol, quantity) {
        symbol = symbol.toUpperCase();

        // Validate symbol
        const stock = this.stockMap.get(symbol);
        if (!stock) {
            throw new Error(`Stock symbol '${symbol}' not found in the market.`);
        }

        // Validate quantity
        if (!Number.isInteger(quantity) || quantity <= 0) {
            throw new Error('Quantity must be a positive whole number.');
        }

        // Calculate total cost
        const totalCost = round2(stock.currentPrice * quantity);

        // Validate balance
        if (this.user.cashBalance < totalCost) {
            throw new Error(
                `Insufficient balance. Need $${totalCost.toFixed(2)}, ` +
                `available $${this.user.cashBalance.toFixed(2)}.`
            );
        }

        // Execute trade
        this.user.deductBalance(totalCost);
        this.user.portfolio.addOrUpdateHolding(symbol, quantity, totalCost);

        // Record transaction
        const txn = new Transaction(
            TransactionType.BUY, symbol, quantity,
            stock.currentPrice, totalCost
        );
        this.user.portfolio.addTransaction(txn);

        // Persist
        this.save();

        return {
            success: true,
            message: `Successfully bought ${quantity} share${quantity > 1 ? 's' : ''} of ${stock.companyName} (${symbol})`,
            totalCost: totalCost,
            pricePerShare: stock.currentPrice
        };
    }

    // ── Sell Operation ─────────────────────────────────────
    /**
     * Execute a SELL order with full validation.
     * 
     * Validation:
     *   1. Symbol must exist in market
     *   2. Quantity must be a positive integer
     *   3. User must own >= requested quantity
     * 
     * On success: credits cash, reduces holding, logs transaction, saves state.
     * Uses average-cost method to reduce cost basis proportionally.
     */
    sellStock(symbol, quantity) {
        symbol = symbol.toUpperCase();

        // Validate symbol
        const stock = this.stockMap.get(symbol);
        if (!stock) {
            throw new Error(`Stock symbol '${symbol}' not found in the market.`);
        }

        // Validate quantity
        if (!Number.isInteger(quantity) || quantity <= 0) {
            throw new Error('Quantity must be a positive whole number.');
        }

        // Validate ownership
        const holding = this.user.portfolio.getHolding(symbol);
        if (!holding || holding.quantity < quantity) {
            const owned = holding ? holding.quantity : 0;
            throw new Error(
                `Insufficient shares. You own ${owned} share${owned !== 1 ? 's' : ''} of ${symbol}.`
            );
        }

        // Calculate revenue
        const totalRevenue = round2(stock.currentPrice * quantity);

        // Remove shares (average-cost method)
        const costFreed = holding.removeShares(quantity);

        // Remove holding entirely if fully sold
        if (holding.quantity === 0) {
            this.user.portfolio.removeHolding(symbol);
        }

        // Credit cash
        this.user.addBalance(totalRevenue);

        // Record transaction
        const txn = new Transaction(
            TransactionType.SELL, symbol, quantity,
            stock.currentPrice, totalRevenue
        );
        this.user.portfolio.addTransaction(txn);

        // Persist
        this.save();

        const realizedPL = round2(totalRevenue - costFreed);
        return {
            success: true,
            message: `Successfully sold ${quantity} share${quantity > 1 ? 's' : ''} of ${stock.companyName} (${symbol})`,
            totalRevenue: totalRevenue,
            realizedPL: realizedPL,
            pricePerShare: stock.currentPrice
        };
    }

    // ── Price Simulation ───────────────────────────────────

    simulatePriceTick() {
        this.priceSimulator.simulateTick(this.stocks);
        this.save();
    }

    // ── Persistence ────────────────────────────────────────

    save() {
        this.storage.saveUser(this.user);
        this.storage.saveStocks(this.stocks);
    }

    load() {
        if (!this.storage.hasData()) return false;

        const loadedStocks = this.storage.loadStocks();
        if (loadedStocks && loadedStocks.length > 0) {
            this.stocks = loadedStocks;
            this.stockMap.clear();
            for (const s of loadedStocks) {
                this.stockMap.set(s.symbol, s);
            }
        }

        const loadedUser = this.storage.loadUser();
        if (loadedUser) {
            this.user = loadedUser;
            return true;
        }
        return false;
    }
}

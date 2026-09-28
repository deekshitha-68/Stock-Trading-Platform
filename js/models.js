/**
 * Stock Trading Platform — OOP Model Classes
 * 
 * JavaScript ES6 classes mirroring Java OOP patterns:
 *   - Encapsulation (private-like fields, controlled access)
 *   - Composition (User → Portfolio → Holdings/Transactions)
 *   - Abstraction (clean public APIs, internal logic hidden)
 * 
 * All monetary values are rounded to 2 decimal places.
 * Each class has toJSON() / fromJSON() for localStorage persistence.
 * 
 * SIMULATION ONLY — No real money, no real stocks.
 */

// ─── Transaction Type Enum ────────────────────────────────
const TransactionType = Object.freeze({
    BUY: 'BUY',
    SELL: 'SELL'
});

// ─── Stock ────────────────────────────────────────────────
/**
 * Represents a single tradable stock in the simulated market.
 * Tracks current price, previous price, and computed price change.
 */
class Stock {
    constructor(symbol, companyName, currentPrice) {
        this.symbol = symbol.toUpperCase();
        this.companyName = companyName;
        this.currentPrice = round2(currentPrice);
        this.previousPrice = this.currentPrice;
    }

    setPrice(newPrice) {
        this.previousPrice = this.currentPrice;
        this.currentPrice = Math.max(0.01, round2(newPrice));
    }

    getPriceChange() {
        return round2(this.currentPrice - this.previousPrice);
    }

    getPriceChangePercent() {
        if (this.previousPrice === 0) return 0;
        return round2((this.currentPrice - this.previousPrice) / this.previousPrice * 100);
    }

    toJSON() {
        return {
            symbol: this.symbol,
            companyName: this.companyName,
            currentPrice: this.currentPrice,
            previousPrice: this.previousPrice
        };
    }

    static fromJSON(json) {
        const stock = new Stock(json.symbol, json.companyName, json.currentPrice);
        stock.previousPrice = json.previousPrice;
        return stock;
    }
}

// ─── Holding ──────────────────────────────────────────────
/**
 * Represents a user's position in a single stock.
 * Uses average-cost method for cost basis tracking.
 */
class Holding {
    constructor(stockSymbol, quantity, totalCostBasis) {
        this.stockSymbol = stockSymbol.toUpperCase();
        this.quantity = quantity;
        this.totalCostBasis = round2(totalCostBasis);
    }

    getAvgCost() {
        if (this.quantity === 0) return 0;
        return round2(this.totalCostBasis / this.quantity);
    }

    addShares(qty, cost) {
        this.quantity += qty;
        this.totalCostBasis = round2(this.totalCostBasis + cost);
    }

    removeShares(qty) {
        const avgCost = this.getAvgCost();
        const costFreed = round2(avgCost * qty);
        this.quantity -= qty;
        this.totalCostBasis = round2(this.totalCostBasis - costFreed);
        if (this.quantity === 0) this.totalCostBasis = 0;
        return costFreed;
    }

    getMarketValue(currentPrice) {
        return round2(currentPrice * this.quantity);
    }

    getUnrealizedPL(currentPrice) {
        return round2(this.getMarketValue(currentPrice) - this.totalCostBasis);
    }

    getUnrealizedPLPercent(currentPrice) {
        if (this.totalCostBasis === 0) return 0;
        return round2(this.getUnrealizedPL(currentPrice) / this.totalCostBasis * 100);
    }

    toJSON() {
        return {
            stockSymbol: this.stockSymbol,
            quantity: this.quantity,
            totalCostBasis: this.totalCostBasis
        };
    }

    static fromJSON(json) {
        return new Holding(json.stockSymbol, json.quantity, json.totalCostBasis);
    }
}

// ─── Transaction ──────────────────────────────────────────
/**
 * Immutable record of a single trade (buy or sell).
 * Each transaction gets a unique auto-incrementing ID.
 */
class Transaction {
    constructor(type, stockSymbol, quantity, pricePerShare, totalAmount, timestamp) {
        this.id = Transaction._nextId++;
        this.type = type;
        this.stockSymbol = stockSymbol.toUpperCase();
        this.quantity = quantity;
        this.pricePerShare = round2(pricePerShare);
        this.totalAmount = round2(totalAmount);
        this.timestamp = timestamp || new Date().toISOString();
    }

    toJSON() {
        return {
            id: this.id,
            type: this.type,
            stockSymbol: this.stockSymbol,
            quantity: this.quantity,
            pricePerShare: this.pricePerShare,
            totalAmount: this.totalAmount,
            timestamp: this.timestamp
        };
    }

    static fromJSON(json) {
        const txn = new Transaction(
            json.type, json.stockSymbol, json.quantity,
            json.pricePerShare, json.totalAmount, json.timestamp
        );
        txn.id = json.id;
        if (json.id >= Transaction._nextId) {
            Transaction._nextId = json.id + 1;
        }
        return txn;
    }
}
Transaction._nextId = 1;

// ─── Portfolio ────────────────────────────────────────────
/**
 * Aggregates a user's holdings and transaction history.
 * Holdings stored in a Map for O(1) lookup by symbol.
 */
class Portfolio {
    constructor() {
        this.holdings = new Map();
        this.transactions = [];
    }

    getHolding(symbol) {
        return this.holdings.get(symbol.toUpperCase()) || null;
    }

    addOrUpdateHolding(symbol, quantity, cost) {
        const key = symbol.toUpperCase();
        const existing = this.holdings.get(key);
        if (existing) {
            existing.addShares(quantity, cost);
        } else {
            this.holdings.set(key, new Holding(key, quantity, cost));
        }
    }

    removeHolding(symbol) {
        this.holdings.delete(symbol.toUpperCase());
    }

    addTransaction(txn) {
        this.transactions.push(txn);
    }

    getAllHoldings() {
        return Array.from(this.holdings.values());
    }

    getHoldingsCount() {
        return this.holdings.size;
    }

    getTotalMarketValue(priceMap) {
        let total = 0;
        for (const h of this.holdings.values()) {
            const price = priceMap.get(h.stockSymbol) || 0;
            total += h.getMarketValue(price);
        }
        return round2(total);
    }

    getTotalCostBasis() {
        let total = 0;
        for (const h of this.holdings.values()) {
            total += h.totalCostBasis;
        }
        return round2(total);
    }

    getTotalUnrealizedPL(priceMap) {
        let total = 0;
        for (const h of this.holdings.values()) {
            const price = priceMap.get(h.stockSymbol) || 0;
            total += h.getUnrealizedPL(price);
        }
        return round2(total);
    }

    toJSON() {
        return {
            holdings: Array.from(this.holdings.values()).map(h => h.toJSON()),
            transactions: this.transactions.map(t => t.toJSON())
        };
    }

    static fromJSON(json) {
        const p = new Portfolio();
        if (json.holdings) {
            for (const h of json.holdings) {
                const holding = Holding.fromJSON(h);
                p.holdings.set(holding.stockSymbol, holding);
            }
        }
        if (json.transactions) {
            for (const t of json.transactions) {
                p.transactions.push(Transaction.fromJSON(t));
            }
        }
        return p;
    }
}

// ─── User ─────────────────────────────────────────────────
/**
 * Represents the registered trader. Owns exactly one Portfolio.
 */
class User {
    constructor(username, cashBalance) {
        this.username = username;
        this.cashBalance = round2(cashBalance);
        this.portfolio = new Portfolio();
        this.createdAt = new Date().toISOString();
    }

    deductBalance(amount) {
        this.cashBalance = round2(this.cashBalance - amount);
    }

    addBalance(amount) {
        this.cashBalance = round2(this.cashBalance + amount);
    }

    getTotalAccountValue(priceMap) {
        return round2(this.cashBalance + this.portfolio.getTotalMarketValue(priceMap));
    }

    toJSON() {
        return {
            username: this.username,
            cashBalance: this.cashBalance,
            portfolio: this.portfolio.toJSON(),
            createdAt: this.createdAt
        };
    }

    static fromJSON(json) {
        const user = new User(json.username, json.cashBalance);
        user.portfolio = Portfolio.fromJSON(json.portfolio);
        user.createdAt = json.createdAt || new Date().toISOString();
        return user;
    }
}

// ─── Utility ──────────────────────────────────────────────
/**
 * Rounds a number to 2 decimal places.
 */
function round2(n) {
    return Math.round((n + Number.EPSILON) * 100) / 100;
}

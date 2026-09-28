# Stock Trading Platform — Web Simulator

> **CodeAlpha Internship — Task 2**

[![HTML](https://img.shields.io/badge/HTML5-E34F26?logo=html5&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/HTML)
[![CSS](https://img.shields.io/badge/CSS3-1572B6?logo=css3&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/CSS)
[![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?logo=javascript&logoColor=black)](https://developer.mozilla.org/en-US/docs/Web/JavaScript)

## ⚠️ Disclaimer

**This application is a SIMULATION ONLY.** It does **not** connect to any real stock exchange, use real money, or execute real trades. All stock tickers, prices, and balances are **simulated/fake data** for educational purposes.

---

## 📋 Project Description

A web-based stock trading simulator built with **HTML, CSS, and vanilla JavaScript** as part of the CodeAlpha Java Development Internship (Task 2). The application simulates a stock trading environment where users can:

- Register with a virtual cash balance
- View simulated market data with live price fluctuations
- Buy and sell stocks with full validation
- Track portfolio performance with profit/loss calculations
- Review complete transaction history

All logic runs entirely in the browser — no backend server or database required. Data persists across sessions via `localStorage`.

---

## ✨ Key Features

| Feature | Description |
|---------|-------------|
| **User Registration** | Register with a name and custom starting balance (default: $100,000) |
| **Dashboard** | At-a-glance overview: balance, portfolio value, investment, P/L, stocks owned |
| **Simulated Market** | 10 stocks with random-walk price fluctuations (±5% per tick) |
| **Buy Stocks** | Modal with real-time cost calculation, balance validation |
| **Sell Stocks** | Modal showing owned shares, revenue calculation, realized P/L |
| **Portfolio Tracking** | Holdings table: qty, avg cost, current price, market value, unrealized P/L |
| **Transaction History** | Chronological log of all trades with ID, type, price, total, timestamp |
| **Auto Price Updates** | Prices update every 30 seconds automatically |
| **Data Persistence** | All state saved to localStorage — survives browser refresh |
| **Responsive Design** | Works on desktop, tablet, and mobile |
| **Dark Theme** | Professional trading-platform aesthetic |

---

## 🛠 Tech Stack

| Component | Technology |
|-----------|-----------|
| Structure | HTML5 |
| Styling | CSS3 (custom properties, flexbox, grid) |
| Logic | Vanilla JavaScript (ES6+ classes) |
| Typography | Inter (Google Fonts) |
| Persistence | localStorage (JSON serialization) |
| Architecture | Single Page Application (SPA) |

**No frameworks, no libraries, no build tools, no server** — pure web standards.

---

## 📂 Folder Structure

```
Stock Trading Platform/
├── index.html              ← Single entry point (all pages as sections)
├── css/
│   └── style.css           ← Complete dark-theme stylesheet
├── js/
│   ├── models.js           ← OOP classes: Stock, User, Portfolio, Holding, Transaction
│   ├── services.js         ← Business logic: TradingSystem, PriceSimulator, StorageService
│   └── app.js              ← UI: navigation, page rendering, modals, events
├── src/                    ← (Java console version — bonus)
├── README.md
└── .gitignore
```

---

## 🚀 How to Run

### Option 1: Direct File Open
Simply double-click `index.html` or open it in any modern browser:
```
File → Open → index.html
```

### Option 2: Local Server (optional)
```bash
# Python
python -m http.server 8000

# Node.js
npx serve .

# Then open http://localhost:8000
```

**No compilation, no installation, no dependencies required.**

---

## 📸 Pages & Features

### 1. Login Page
- Clean registration form with username and starting balance
- Simulation disclaimer prominently displayed

### 2. Dashboard
- 5 stat cards: Balance, Portfolio Value, Investment, P/L, Stocks Owned
- Quick navigation buttons
- Recent transaction activity table

### 3. Market
- Table of 10 simulated stocks with current price and % change
- BUY/SELL buttons per stock
- Manual "Update Prices" button + auto-update every 30 seconds

### 4. Buy Modal
- Stock info, quantity input, real-time total cost calculation
- Balance check with error display
- Confirm/cancel actions

### 5. Sell Modal
- Shows owned shares, quantity selector with "Sell All" option
- Real-time revenue and estimated P/L calculation
- Ownership validation with error display

### 6. Portfolio
- Summary: total value, total invested, total P/L
- Holdings table: symbol, company, qty, avg cost, current price, value, P/L ($ and %)

### 7. Transaction History
- Reverse-chronological table
- Fields: ID, Type (BUY/SELL badge), Stock, Qty, Price, Total, Date/Time

### 8. Profile
- Avatar, username, member since date
- Account stats: balance, total value, total trades, stocks owned
- Logout button

---

## 🧩 OOP Concepts Demonstrated

| Concept | Implementation |
|---------|---------------|
| **Encapsulation** | All model classes use controlled field access with methods |
| **Composition** | User → Portfolio → Holdings + Transactions |
| **Abstraction** | TradingSystem hides business logic from UI layer |
| **Enum Pattern** | `TransactionType` frozen object (BUY/SELL) |
| **Serialization** | `toJSON()` / `fromJSON()` for persistence |
| **Separation of Concerns** | models.js (data) → services.js (logic) → app.js (UI) |

---

## 📊 CodeAlpha Task 2 Requirements Mapping

| # | Requirement | Implementation |
|---|------------|----------------|
| 1 | Simulate stock trading environment | 10 simulated stocks, random-walk pricing, buy/sell flow |
| 2 | Display market data | Market page with price table, % change, auto-updates |
| 3 | Allow buying stocks | Buy modal with validation, balance deduction, holding update |
| 4 | Allow selling stocks | Sell modal with validation, balance credit, P/L calculation |
| 5 | Track portfolio performance | Portfolio page: holdings, cost basis, market value, P/L |
| 6 | OOP for stocks, users, transactions | ES6 classes: Stock, User, Portfolio, Holding, Transaction |

---

## 💡 How It Works

### Price Simulation
Prices fluctuate via a **random-walk algorithm**: each tick generates a random % change in [-5%, +5%], applied to every stock's price. A $0.01 floor prevents zero/negative prices.

### Cost Basis (Average Cost Method)
- **On BUY**: Cost basis accumulates. Avg cost = total cost basis / total shares.
- **On SELL**: Cost basis reduced proportionally. Realized P/L = revenue − (avg cost × qty).
- **Unrealized P/L**: (current price − avg cost) × quantity held.

### Persistence
All state (user, stocks, holdings, transactions) is serialized to JSON and stored in `localStorage`. On page load, the app checks for existing data and restores the session automatically.

---

## 📝 License

This project is part of the CodeAlpha Java Development Internship program.  
Built for educational purposes only — **no real trading functionality**.

/**
 * Stock Trading Platform — Application Logic
 * 
 * Handles:
 *   - SPA navigation (show/hide page sections)
 *   - Page rendering (dashboard, market, portfolio, transactions, profile)
 *   - Buy/Sell modal logic with real-time calculations
 *   - Toast notifications
 *   - Price auto-update (every 30 seconds)
 *   - Login/logout flow
 * 
 * SIMULATION ONLY — No real money, no real stocks.
 */

// ═══════════════════ GLOBALS ════════════════════════════
const tradingSystem = new TradingSystem();
let currentPage = 'login';
let priceInterval = null;
let currentBuySymbol = null;
let currentSellSymbol = null;

// ═══════════════════ UTILITY FUNCTIONS ══════════════════

/**
 * Formats a number as USD currency string.
 */
function formatMoney(amount) {
    const negative = amount < 0;
    const abs = Math.abs(amount);
    const formatted = '$' + abs.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    return negative ? '-' + formatted : formatted;
}

/**
 * Formats an ISO date string for display.
 */
function formatDate(isoString) {
    const d = new Date(isoString);
    return d.toLocaleDateString('en-US', {
        year: 'numeric', month: 'short', day: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}

/**
 * Formats a date as short date only.
 */
function formatDateShort(isoString) {
    const d = new Date(isoString);
    return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

// ═══════════════════ TOAST NOTIFICATIONS ════════════════

/**
 * Shows a toast notification.
 * @param {string} message - The message to display
 * @param {'success'|'error'|'info'} type - The toast type
 */
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    const icons = { success: '✓', error: '✗', info: 'ℹ' };
    toast.innerHTML = `
        <span class="toast-icon">${icons[type] || 'ℹ'}</span>
        <span class="toast-message">${message}</span>
    `;

    container.appendChild(toast);

    // Auto-remove after 3.5 seconds
    setTimeout(() => {
        toast.classList.add('toast-exit');
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

// ═══════════════════ NAVIGATION ═════════════════════════

/**
 * Navigates to a page by showing/hiding sections.
 */
function navigateTo(pageName) {
    // Hide all pages
    document.querySelectorAll('.page').forEach(p => p.classList.add('hidden'));

    // Show target page
    const target = document.getElementById('page-' + pageName);
    if (target) {
        target.classList.remove('hidden');
    }

    // Update nav active state
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.toggle('active', link.dataset.page === pageName);
    });

    // Close mobile menu
    document.getElementById('nav-links').classList.remove('open');

    currentPage = pageName;

    // Render the page content
    switch (pageName) {
        case 'dashboard':    renderDashboard(); break;
        case 'market':       renderMarket(); break;
        case 'portfolio':    renderPortfolio(); break;
        case 'transactions': renderTransactions(); break;
        case 'profile':      renderProfile(); break;
    }

    // Update navbar balance
    updateNavBalance();
}

/**
 * Updates the balance display in the navbar.
 */
function updateNavBalance() {
    if (tradingSystem.isLoggedIn()) {
        document.getElementById('nav-balance').textContent =
            formatMoney(tradingSystem.user.cashBalance);
    }
}

// ═══════════════════ PAGE RENDERERS ═════════════════════

// ─── Dashboard ─────────────────────────────────────────
function renderDashboard() {
    const user = tradingSystem.user;
    const priceMap = tradingSystem.getCurrentPriceMap();

    document.getElementById('dash-greeting').textContent =
        `Welcome back, ${user.username}`;

    document.getElementById('dash-balance').textContent =
        formatMoney(user.cashBalance);

    const totalAccountValue = user.getTotalAccountValue(priceMap);
    document.getElementById('dash-portfolio-value').textContent =
        formatMoney(totalAccountValue);

    const totalInvested = user.portfolio.getTotalCostBasis();
    document.getElementById('dash-investment').textContent =
        formatMoney(totalInvested);

    const totalPL = user.portfolio.getTotalUnrealizedPL(priceMap);
    const plEl = document.getElementById('dash-pl');
    plEl.textContent = (totalPL >= 0 ? '+' : '') + formatMoney(totalPL);
    plEl.className = 'stat-value ' + (totalPL >= 0 ? 'text-success' : 'text-danger');

    document.getElementById('dash-stocks-count').textContent =
        user.portfolio.getHoldingsCount();

    // Recent transactions (last 5)
    const recentDiv = document.getElementById('dash-recent-transactions');
    const transactions = user.portfolio.transactions;

    if (transactions.length === 0) {
        recentDiv.innerHTML = '<p class="empty-state">No transactions yet. Start by buying some stocks!</p>';
    } else {
        const recent = transactions.slice(-5).reverse();
        let html = '<table class="data-table"><thead><tr>';
        html += '<th>Type</th><th>Stock</th><th class="text-right">Qty</th>';
        html += '<th class="text-right">Amount</th><th class="text-right">Date</th></tr></thead><tbody>';
        for (const t of recent) {
            const badgeClass = t.type === 'BUY' ? 'badge-buy' : 'badge-sell';
            html += `<tr>
                <td><span class="badge ${badgeClass}">${t.type}</span></td>
                <td class="stock-symbol">${t.stockSymbol}</td>
                <td class="text-right">${t.quantity}</td>
                <td class="text-right">${formatMoney(t.totalAmount)}</td>
                <td class="text-right text-muted">${formatDateShort(t.timestamp)}</td>
            </tr>`;
        }
        html += '</tbody></table>';
        recentDiv.innerHTML = html;
    }
}

// ─── Market ────────────────────────────────────────────
function renderMarket() {
    const tbody = document.getElementById('market-tbody');
    const stocks = tradingSystem.getAllStocks();
    let html = '';

    for (const stock of stocks) {
        const change = stock.getPriceChange();
        const changePct = stock.getPriceChangePercent();
        let changeClass, arrow;

        if (change > 0) {
            changeClass = 'price-up';
            arrow = '▲';
        } else if (change < 0) {
            changeClass = 'price-down';
            arrow = '▼';
        } else {
            changeClass = 'price-neutral';
            arrow = '—';
        }

        // Check if user owns this stock (for sell button visibility)
        const holding = tradingSystem.user.portfolio.getHolding(stock.symbol);
        const hasSellable = holding && holding.quantity > 0;

        html += `<tr>
            <td>
                <div class="stock-symbol">${stock.symbol}</div>
            </td>
            <td class="stock-company">${stock.companyName}</td>
            <td class="text-right stock-price" id="price-${stock.symbol}">
                ${formatMoney(stock.currentPrice)}
            </td>
            <td class="text-right">
                <span class="price-change ${changeClass}">
                    ${arrow} ${changePct >= 0 ? '+' : ''}${changePct.toFixed(2)}%
                </span>
            </td>
            <td class="text-center">
                <div class="action-buttons">
                    <button class="btn btn-buy btn-sm" onclick="showBuyModal('${stock.symbol}')">
                        BUY
                    </button>
                    <button class="btn btn-sell btn-sm" onclick="showSellModal('${stock.symbol}')"
                        ${!hasSellable ? 'disabled style="opacity:0.4;cursor:not-allowed;"' : ''}>
                        SELL
                    </button>
                </div>
            </td>
        </tr>`;
    }

    tbody.innerHTML = html;
}

// ─── Portfolio ─────────────────────────────────────────
function renderPortfolio() {
    const priceMap = tradingSystem.getCurrentPriceMap();
    const user = tradingSystem.user;
    const holdings = user.portfolio.getAllHoldings();

    // Summary
    const totalValue = user.portfolio.getTotalMarketValue(priceMap);
    const totalInvested = user.portfolio.getTotalCostBasis();
    const totalPL = user.portfolio.getTotalUnrealizedPL(priceMap);

    document.getElementById('port-total-value').textContent = formatMoney(totalValue);
    document.getElementById('port-total-invested').textContent = formatMoney(totalInvested);

    const plEl = document.getElementById('port-total-pl');
    plEl.textContent = (totalPL >= 0 ? '+' : '') + formatMoney(totalPL);
    plEl.className = 'summary-value ' + (totalPL >= 0 ? 'text-success' : 'text-danger');

    const content = document.getElementById('portfolio-content');

    if (holdings.length === 0) {
        content.innerHTML = `
            <div class="empty-state">
                <span class="empty-state-icon">📂</span>
                <p>Your portfolio is empty.</p>
                <p>Head to the <a href="#" onclick="navigateTo('market'); return false;">Market</a> to buy your first stock!</p>
            </div>`;
        return;
    }

    let html = '<table class="data-table"><thead><tr>';
    html += '<th>Symbol</th><th>Company</th>';
    html += '<th class="text-right">Qty</th>';
    html += '<th class="text-right">Avg Cost</th>';
    html += '<th class="text-right">Current Price</th>';
    html += '<th class="text-right">Market Value</th>';
    html += '<th class="text-right">P/L</th>';
    html += '</tr></thead><tbody>';

    for (const h of holdings) {
        const stock = tradingSystem.getStock(h.stockSymbol);
        const companyName = stock ? stock.companyName : h.stockSymbol;
        const currentPrice = priceMap.get(h.stockSymbol) || 0;
        const marketValue = h.getMarketValue(currentPrice);
        const pl = h.getUnrealizedPL(currentPrice);
        const plPct = h.getUnrealizedPLPercent(currentPrice);
        const plClass = pl >= 0 ? 'text-success' : 'text-danger';
        const plSign = pl >= 0 ? '+' : '';

        html += `<tr>
            <td class="stock-symbol">${h.stockSymbol}</td>
            <td class="stock-company">${companyName}</td>
            <td class="text-right">${h.quantity}</td>
            <td class="text-right">${formatMoney(h.getAvgCost())}</td>
            <td class="text-right">${formatMoney(currentPrice)}</td>
            <td class="text-right" style="font-weight:600;">${formatMoney(marketValue)}</td>
            <td class="text-right ${plClass}" style="font-weight:600;">
                ${plSign}${formatMoney(pl)}<br>
                <small>(${plSign}${plPct.toFixed(2)}%)</small>
            </td>
        </tr>`;
    }

    html += '</tbody></table>';
    content.innerHTML = html;
}

// ─── Transaction History ───────────────────────────────
function renderTransactions() {
    const content = document.getElementById('transactions-content');
    const transactions = tradingSystem.user.portfolio.transactions;

    if (transactions.length === 0) {
        content.innerHTML = `
            <div class="empty-state">
                <span class="empty-state-icon">📋</span>
                <p>No transactions yet.</p>
                <p>Your trade history will appear here after your first transaction.</p>
            </div>`;
        return;
    }

    // Show newest first
    const sorted = [...transactions].reverse();

    let html = '<table class="data-table"><thead><tr>';
    html += '<th>ID</th><th>Type</th><th>Stock</th>';
    html += '<th class="text-right">Qty</th>';
    html += '<th class="text-right">Price</th>';
    html += '<th class="text-right">Total</th>';
    html += '<th class="text-right">Date/Time</th>';
    html += '</tr></thead><tbody>';

    for (const t of sorted) {
        const badgeClass = t.type === 'BUY' ? 'badge-buy' : 'badge-sell';
        html += `<tr>
            <td class="text-muted">#${t.id}</td>
            <td><span class="badge ${badgeClass}">${t.type}</span></td>
            <td class="stock-symbol">${t.stockSymbol}</td>
            <td class="text-right">${t.quantity}</td>
            <td class="text-right">${formatMoney(t.pricePerShare)}</td>
            <td class="text-right" style="font-weight:600;">${formatMoney(t.totalAmount)}</td>
            <td class="text-right text-muted">${formatDate(t.timestamp)}</td>
        </tr>`;
    }

    html += '</tbody></table>';
    html += `<p style="text-align:center;color:var(--text-muted);font-size:13px;padding:12px 0;">
        Total transactions: ${transactions.length}</p>`;
    content.innerHTML = html;
}

// ─── Profile ───────────────────────────────────────────
function renderProfile() {
    const user = tradingSystem.user;
    const priceMap = tradingSystem.getCurrentPriceMap();

    document.getElementById('profile-avatar').textContent =
        user.username.charAt(0).toUpperCase();
    document.getElementById('profile-name').textContent = user.username;
    document.getElementById('profile-joined').textContent =
        formatDateShort(user.createdAt);
    document.getElementById('profile-balance').textContent =
        formatMoney(user.cashBalance);
    document.getElementById('profile-total-value').textContent =
        formatMoney(user.getTotalAccountValue(priceMap));
    document.getElementById('profile-total-trades').textContent =
        user.portfolio.transactions.length;
    document.getElementById('profile-stocks-count').textContent =
        user.portfolio.getHoldingsCount();
}

// ═══════════════════ BUY MODAL ══════════════════════════

function showBuyModal(symbol) {
    const stock = tradingSystem.getStock(symbol);
    if (!stock) return;

    currentBuySymbol = symbol;

    document.getElementById('buy-symbol').textContent = stock.symbol;
    document.getElementById('buy-company').textContent = stock.companyName;
    document.getElementById('buy-price').textContent = formatMoney(stock.currentPrice);
    document.getElementById('buy-quantity').value = 1;
    document.getElementById('buy-error').classList.add('hidden');

    updateBuyTotal();

    document.getElementById('buy-modal').classList.remove('hidden');
}

function updateBuyTotal() {
    if (!currentBuySymbol) return;

    const stock = tradingSystem.getStock(currentBuySymbol);
    const qtyInput = document.getElementById('buy-quantity');
    const qty = parseInt(qtyInput.value) || 0;
    const price = stock.currentPrice;
    const totalCost = round2(price * qty);
    const balance = tradingSystem.user.cashBalance;
    const balanceAfter = round2(balance - totalCost);

    document.getElementById('buy-price-display').textContent = formatMoney(price);
    document.getElementById('buy-qty-display').textContent = qty;
    document.getElementById('buy-total-cost').textContent = formatMoney(totalCost);
    document.getElementById('buy-balance').textContent = formatMoney(balance);

    const afterEl = document.getElementById('buy-balance-after');
    afterEl.textContent = formatMoney(balanceAfter);
    afterEl.className = balanceAfter < 0 ? 'text-danger' : '';

    // Show/hide error
    const errorEl = document.getElementById('buy-error');
    if (qty <= 0) {
        errorEl.textContent = 'Quantity must be at least 1.';
        errorEl.classList.remove('hidden');
    } else if (balanceAfter < 0) {
        errorEl.textContent = `Insufficient balance. You need ${formatMoney(totalCost)} but only have ${formatMoney(balance)}.`;
        errorEl.classList.remove('hidden');
    } else {
        errorEl.classList.add('hidden');
    }
}

function confirmBuy() {
    if (!currentBuySymbol) return;

    const qty = parseInt(document.getElementById('buy-quantity').value);

    try {
        const result = tradingSystem.buyStock(currentBuySymbol, qty);
        closeModal('buy-modal');
        showToast(result.message, 'success');
        // Re-render the current page
        navigateTo(currentPage);
    } catch (e) {
        document.getElementById('buy-error').textContent = e.message;
        document.getElementById('buy-error').classList.remove('hidden');
    }
}

// ═══════════════════ SELL MODAL ═════════════════════════

function showSellModal(symbol) {
    const stock = tradingSystem.getStock(symbol);
    if (!stock) return;

    const holding = tradingSystem.user.portfolio.getHolding(symbol);
    if (!holding || holding.quantity === 0) {
        showToast(`You don't own any shares of ${symbol}.`, 'error');
        return;
    }

    currentSellSymbol = symbol;

    document.getElementById('sell-symbol').textContent = stock.symbol;
    document.getElementById('sell-company').textContent = stock.companyName;
    document.getElementById('sell-price').textContent = formatMoney(stock.currentPrice);
    document.getElementById('sell-owned').textContent = holding.quantity;
    document.getElementById('sell-quantity').value = 1;
    document.getElementById('sell-quantity').max = holding.quantity;
    document.getElementById('sell-error').classList.add('hidden');

    updateSellTotal();

    document.getElementById('sell-modal').classList.remove('hidden');
}

function updateSellTotal() {
    if (!currentSellSymbol) return;

    const stock = tradingSystem.getStock(currentSellSymbol);
    const holding = tradingSystem.user.portfolio.getHolding(currentSellSymbol);
    if (!stock || !holding) return;

    const qty = parseInt(document.getElementById('sell-quantity').value) || 0;
    const price = stock.currentPrice;
    const totalRevenue = round2(price * qty);
    const avgCost = holding.getAvgCost();
    const estimatedPL = round2((price - avgCost) * qty);

    document.getElementById('sell-price-display').textContent = formatMoney(price);
    document.getElementById('sell-qty-display').textContent = qty;
    document.getElementById('sell-total-revenue').textContent = formatMoney(totalRevenue);

    const plEl = document.getElementById('sell-estimated-pl');
    const plSign = estimatedPL >= 0 ? '+' : '';
    plEl.textContent = plSign + formatMoney(estimatedPL);
    plEl.className = estimatedPL >= 0 ? 'text-success' : 'text-danger';

    // Show/hide error
    const errorEl = document.getElementById('sell-error');
    if (qty <= 0) {
        errorEl.textContent = 'Quantity must be at least 1.';
        errorEl.classList.remove('hidden');
    } else if (qty > holding.quantity) {
        errorEl.textContent = `You only own ${holding.quantity} shares of ${currentSellSymbol}.`;
        errorEl.classList.remove('hidden');
    } else {
        errorEl.classList.add('hidden');
    }
}

function sellAll() {
    if (!currentSellSymbol) return;
    const holding = tradingSystem.user.portfolio.getHolding(currentSellSymbol);
    if (holding) {
        document.getElementById('sell-quantity').value = holding.quantity;
        updateSellTotal();
    }
}

function confirmSell() {
    if (!currentSellSymbol) return;

    const qty = parseInt(document.getElementById('sell-quantity').value);

    try {
        const result = tradingSystem.sellStock(currentSellSymbol, qty);
        closeModal('sell-modal');
        const plMsg = result.realizedPL >= 0
            ? ` (P/L: +${formatMoney(result.realizedPL)})`
            : ` (P/L: ${formatMoney(result.realizedPL)})`;
        showToast(result.message + plMsg, 'success');
        navigateTo(currentPage);
    } catch (e) {
        document.getElementById('sell-error').textContent = e.message;
        document.getElementById('sell-error').classList.remove('hidden');
    }
}

// ═══════════════════ MODAL UTILITIES ═══════════════════

function closeModal(modalId) {
    document.getElementById(modalId).classList.add('hidden');
    currentBuySymbol = null;
    currentSellSymbol = null;
}

// Close modals on Escape key
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        closeModal('buy-modal');
        closeModal('sell-modal');
    }
});

// ═══════════════════ PRICE SIMULATION ═══════════════════

function handleSimulateTick() {
    tradingSystem.simulatePriceTick();
    showToast('Market prices updated!', 'info');
    // Re-render current page to reflect new prices
    if (currentPage === 'market') renderMarket();
    else if (currentPage === 'dashboard') renderDashboard();
    else if (currentPage === 'portfolio') renderPortfolio();
    updateNavBalance();
}

/**
 * Starts automatic price updates every 30 seconds.
 */
function startPriceAutoUpdate() {
    if (priceInterval) clearInterval(priceInterval);
    priceInterval = setInterval(() => {
        tradingSystem.simulatePriceTick();
        // Silently re-render if on a price-sensitive page
        if (currentPage === 'market') renderMarket();
        else if (currentPage === 'dashboard') renderDashboard();
        else if (currentPage === 'portfolio') renderPortfolio();
        updateNavBalance();
    }, 30000); // 30 seconds
}

// ═══════════════════ LOGIN / LOGOUT ════════════════════

function handleLogin(event) {
    event.preventDefault();

    const nameInput = document.getElementById('login-name');
    const balanceInput = document.getElementById('login-balance');

    const name = nameInput.value.trim();
    const balance = parseFloat(balanceInput.value) || 100000;

    if (!name) {
        nameInput.focus();
        return;
    }

    try {
        tradingSystem.initializeDefaultStocks();
        tradingSystem.registerUser(name, balance);

        // Show navbar and go to dashboard
        document.getElementById('navbar').classList.remove('hidden');
        navigateTo('dashboard');
        showToast(`Welcome, ${name}! Start trading with ${formatMoney(balance)}.`, 'success');
        startPriceAutoUpdate();
    } catch (e) {
        showToast(e.message, 'error');
    }
}

function handleLogout() {
    if (confirm('Are you sure you want to logout? Your data will be cleared.')) {
        if (priceInterval) clearInterval(priceInterval);
        tradingSystem.logout();
        document.getElementById('navbar').classList.add('hidden');
        document.getElementById('login-name').value = '';
        document.getElementById('login-balance').value = 100000;
        navigateTo('login');
        // Show login page (navigateTo hides all, but login needs to be shown)
        document.getElementById('page-login').classList.remove('hidden');
    }
}

// ═══════════════════ INITIALIZATION ═════════════════════

function init() {
    // Setup navigation click handlers
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            navigateTo(link.dataset.page);
        });
    });

    // Mobile menu toggle
    document.getElementById('nav-toggle').addEventListener('click', () => {
        document.getElementById('nav-links').classList.toggle('open');
    });

    // Try to load saved state
    const loaded = tradingSystem.load();

    if (loaded && tradingSystem.isLoggedIn()) {
        // Resume saved session
        document.getElementById('navbar').classList.remove('hidden');
        navigateTo('dashboard');
        showToast(`Welcome back, ${tradingSystem.user.username}!`, 'info');
        startPriceAutoUpdate();
    } else {
        // Fresh start — show login page
        tradingSystem.initializeDefaultStocks();
        document.getElementById('page-login').classList.remove('hidden');
    }
}

// Start when DOM is ready
document.addEventListener('DOMContentLoaded', init);

package com.codealpha.stocktrading;

import com.codealpha.stocktrading.service.StockTradingSystem;
import com.codealpha.stocktrading.ui.ConsoleMenu;

/**
 * Entry point for the Stock Trading Platform simulator.
 * 
 * Wires together the StockTradingSystem (service layer) and ConsoleMenu
 * (presentation layer), then starts the interactive console loop.
 * 
 * This class is intentionally minimal — it only performs wiring and
 * delegates all logic to the appropriate layers.
 */
public class Main {

    public static void main(String[] args) {
        StockTradingSystem system = new StockTradingSystem();
        ConsoleMenu menu = new ConsoleMenu(system);
        menu.start();
    }
}

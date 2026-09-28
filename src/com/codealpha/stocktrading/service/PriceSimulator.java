package com.codealpha.stocktrading.service;

import com.codealpha.stocktrading.model.Stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Random;

/**
 * Simulates market price movements using a random-walk algorithm.
 * 
 * On each "tick" (called whenever the user returns to the main menu),
 * every stock's price is adjusted by a random percentage between
 * −5% and +5%. A hard floor of $0.01 prevents prices from going
 * to zero or negative.
 * 
 * This keeps the simulation interesting without requiring external data.
 */
public class PriceSimulator {

    private static final BigDecimal MAX_CHANGE_PERCENT = new BigDecimal("0.05"); // ±5%
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");

    private final Random random;

    /**
     * Constructs a PriceSimulator with a default Random instance.
     */
    public PriceSimulator() {
        this.random = new Random();
    }

    /**
     * Applies a random price fluctuation to every stock in the collection.
     * 
     * Algorithm per stock:
     *   1. Generate a random double in [-MAX_CHANGE, +MAX_CHANGE].
     *   2. Multiply current price by (1 + changePercent).
     *   3. Clamp result to MIN_PRICE floor.
     *   4. Update stock's price.
     *
     * @param stocks the collection of all stocks in the market
     */
    public void simulateTick(Collection<Stock> stocks) {
        for (Stock stock : stocks) {
            // Random value in [-0.05, +0.05]
            double changeFraction = (random.nextDouble() * 2 - 1) * MAX_CHANGE_PERCENT.doubleValue();
            BigDecimal multiplier = BigDecimal.ONE.add(BigDecimal.valueOf(changeFraction));

            BigDecimal newPrice = stock.getCurrentPrice()
                    .multiply(multiplier)
                    .setScale(2, RoundingMode.HALF_UP);

            // Enforce minimum price floor
            if (newPrice.compareTo(MIN_PRICE) < 0) {
                newPrice = MIN_PRICE;
            }

            stock.setCurrentPrice(newPrice);
        }
    }
}

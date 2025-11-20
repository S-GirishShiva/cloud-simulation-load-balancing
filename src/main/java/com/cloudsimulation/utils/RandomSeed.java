package com.cloudsimulation.utils;

import java.util.Random;

/**
 * RandomSeed provides centralized random number generation with configurable seed
 * to ensure reproducibility across simulation runs.
 *
 * Critical for deterministic simulation behavior.
 */
public class RandomSeed {
    private static long seed = System.currentTimeMillis();
    private static Random random = new Random(seed);

    /**
     * Sets the random seed for reproducible simulations.
     *
     * @param newSeed Seed value for random number generation
     */
    public static void setSeed(long newSeed) {
        seed = newSeed;
        random = new Random(seed);
    }

    /**
     * Gets the Random instance with configured seed.
     * Always use this method for random number generation to ensure reproducibility.
     *
     * @return Random instance with configured seed
     */
    public static Random getRandom() {
        return random;
    }

    /**
     * Gets the current seed value.
     *
     * @return Current seed value
     */
    public static long getSeed() {
        return seed;
    }
}

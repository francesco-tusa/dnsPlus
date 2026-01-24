package utils;

import java.util.Random;
import java.util.logging.Logger;

/**
 * A centralized source of randomness to ensure simulation reproducibility.
 * All classes requiring random numbers MUST use this instead of new Random().
 */
public class SimulationRandom {
    
    // Use the project's standard logger
    private static final Logger logger = CustomLogger.getLogger(SimulationRandom.class.getName());
    
    private static Random instance;
    private static long currentSeed;

    /**
     * Initializes the random number generator with a specific seed.
     * This should be called by SimConfiguration upon loading.
     */
    public static synchronized void init(long seed) {
        currentSeed = seed;
        instance = new Random(seed);
        // Log to file and console using the standard mechanism
        logger.info("Initialized Global Random Generator with seed: " + seed);
    }

    /**
     * Returns the global Random instance.
     */
    public static Random get() {
        if (instance == null) {
            // Fallback warning if accessed before SimConfiguration loads
            logger.warning("Accessed before init. Using default System.currentTimeMillis() seed.");
            init(System.currentTimeMillis());
        }
        return instance;
    }
    
    public static long getSeed() {
        return currentSeed;
    }
}
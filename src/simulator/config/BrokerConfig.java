package simulator.config;

import java.util.Properties;
import java.util.logging.Logger;
import utils.CustomLogger;

public class BrokerConfig {
    private static final Logger logger = CustomLogger.getLogger(BrokerConfig.class.getName());

    public enum StrategyType { SIMPLE, SMART }

    // Public so factories can switch on it easily
    public final StrategyType strategy;
    
    // Strategy-specific fields are now private to enforce correct usage via getters
    private final double smartThreshold;
    private final boolean intersectionOptimizationEnabled;

    public BrokerConfig(Properties props) {
        String stratStr = props.getProperty("broker.strategy", "SIMPLE").toUpperCase();
        try {
            this.strategy = StrategyType.valueOf(stratStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Config Error: Unknown broker.strategy '" + stratStr + "'. Valid: SIMPLE, SMART");
        }

        // --- Parameter Loading & Alignment ---
        if (this.strategy == StrategyType.SMART) {
            // SMART Strategy: Requires threshold, Intersect Opt is irrelevant
            this.smartThreshold = parseDouble(props, "broker.smartThreshold", "0.5");
            this.intersectionOptimizationEnabled = false; 

            // Validation
            if (this.smartThreshold < 0.0 || this.smartThreshold > 1.0) {
                throw new IllegalArgumentException("Config Error: broker.smartThreshold must be between 0.0 and 1.0.");
            }
            if (props.containsKey("broker.optimization.region_intersection")) {
                logger.warning("Config Warning: 'broker.optimization.region_intersection' is ignored because strategy is SMART.");
            }

        } else {
            // SIMPLE Strategy: Requires Opt flag, Threshold is irrelevant
            this.smartThreshold = -1.0; // Sentinel value
            this.intersectionOptimizationEnabled = Boolean.parseBoolean(
                props.getProperty("broker.optimization.region_intersection", "false")
            );

            if (props.containsKey("broker.smartThreshold")) {
                logger.warning("Config Warning: 'broker.smartThreshold' is ignored because strategy is SIMPLE.");
            }
        }
        
        logger.info("Broker Config Loaded: Strategy=" + strategy + 
                    (strategy == StrategyType.SMART ? ", Threshold=" + smartThreshold : "") +
                    (strategy == StrategyType.SIMPLE ? ", IntersectionOpt=" + intersectionOptimizationEnabled : ""));
    }

    /**
     * Helper method to check if the strategy is SMART.
     * Restored to support logging and logic checks.
     */
    public boolean isSmartStrategy() {
        return strategy == StrategyType.SMART;
    }

    /**
     * Returns the threshold for the SMART strategy.
     * @throws IllegalStateException if called when strategy is not SMART.
     */
    public double getSmartThreshold() {
        if (strategy != StrategyType.SMART) {
            throw new IllegalStateException("Attempted to access Smart Threshold but strategy is " + strategy);
        }
        return smartThreshold;
    }

    /**
     * Returns the optimization flag for the SIMPLE strategy.
     * Returns false by default if strategy is SMART.
     */
    public boolean isIntersectionOptimizationEnabled() {
        if (strategy != StrategyType.SIMPLE) {
            return false;
        }
        return intersectionOptimizationEnabled;
    }

    private double parseDouble(Properties props, String key, String defaultVal) {
        try { return Double.parseDouble(props.getProperty(key, defaultVal)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Config Error: Invalid double for " + key); }
    }
}
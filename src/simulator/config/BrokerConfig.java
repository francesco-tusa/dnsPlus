package simulator.config;

import java.util.Properties;
import java.util.logging.Logger;
import utils.CustomLogger;

public class BrokerConfig {
    private static final Logger logger = CustomLogger.getLogger(BrokerConfig.class.getName());

    public enum StrategyType { SIMPLE, SMART }

    // Public so factories can switch on it easily
    public final StrategyType strategy;
    
    // Strategy-specific fields
    private final double smartThreshold;
    private final boolean intersectionOptimizationEnabled;

    // --- Proximity Routing Brake Settings ---
    public final boolean proximityBrakeEnabled;
    public final int proximityBrakeLimit;
    public final long proximityBrakeIntervalMs;

    public BrokerConfig(Properties props) {
        // 1. Load Universal Properties
        this.strategy = ConfigParser.parseEnum(props, "broker.strategy", StrategyType.class, StrategyType.SIMPLE);
        
        this.intersectionOptimizationEnabled = ConfigParser.parseBoolean(props, "broker.optimization.region_intersection", true);

        // 2. Strategy-Specific Loading
        if (this.strategy == StrategyType.SMART) {
            this.smartThreshold = ConfigParser.parseDouble(props, "broker.smartThreshold", 0.5);
            
            // Validation for Threshold
            if (this.smartThreshold < 0.0 || this.smartThreshold > 1.0) {
                throw new IllegalArgumentException("Config Error: broker.smartThreshold must be between 0.0 and 1.0.");
            }
        } else {
            this.smartThreshold = -1.0; // Sentinel value
            if (props.containsKey("broker.smartThreshold")) {
                logger.warning("Config Warning: 'broker.smartThreshold' is ignored because strategy is SIMPLE.");
            }
        }
        
        // 3. Proximity Brake Settings
        this.proximityBrakeEnabled = ConfigParser.parseBoolean(props, "proximity.brake.enabled", false);
        this.proximityBrakeLimit = ConfigParser.parseInt(props, "proximity.brake.limit", 5);
        this.proximityBrakeIntervalMs = ConfigParser.parseLong(props, "proximity.brake.interval_ms", 1000);

        logger.info("Broker Config Loaded: Strategy=" + strategy + 
                    ", IntersectionOpt=" + intersectionOptimizationEnabled +
                    (strategy == StrategyType.SMART ? ", Threshold=" + smartThreshold : "") +
                    (proximityBrakeEnabled ? String.format(", Brake=[Limit:%d, Int:%dms]", proximityBrakeLimit, proximityBrakeIntervalMs) : ""));
    }

    public boolean isSmartStrategy() {
        return strategy == StrategyType.SMART;
    }

    public double getSmartThreshold() {
        if (strategy != StrategyType.SMART) {
            throw new IllegalStateException("Attempted to access Smart Threshold but strategy is " + strategy);
        }
        return smartThreshold;
    }

    public boolean isIntersectionOptimizationEnabled() {
        return intersectionOptimizationEnabled;
    }
}
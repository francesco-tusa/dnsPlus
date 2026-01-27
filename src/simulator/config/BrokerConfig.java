package simulator.config;

import java.util.Properties;
// Logger removed to silence output
// import java.util.logging.Logger;
// import utils.CustomLogger;

public class BrokerConfig {
    
    public enum StrategyType { SIMPLE, SMART }

    // Region Strategy Settings
    public final StrategyType strategy;
    private final double smartThreshold;
    private final boolean intersectionOptimizationEnabled;
    public final String storeImplementation; // Implementation strategy for the SMART store (LIST or TREE)

    // Proximity Routing Brake Settings
    // (These apply only when running a Proximity/Location Simulation)
    public final boolean proximityBrakeEnabled;
    public final int proximityBrakeLimit;
    public final long proximityBrakeIntervalMs;

    public BrokerConfig(Properties props) {
        // 1. Region Strategy Configuration
        this.strategy = ConfigParser.parseEnum(props, "broker.strategy", StrategyType.class, StrategyType.SMART);
        this.intersectionOptimizationEnabled = ConfigParser.parseBoolean(props, "broker.optimization.region_intersection", true);

        // Smart Threshold Loading
        if (this.strategy == StrategyType.SMART) {
            this.smartThreshold = ConfigParser.parseDouble(props, "broker.smartThreshold", 0.25);
            if (this.smartThreshold < 0.0 || this.smartThreshold > 1.0) {
                throw new IllegalArgumentException("Config Error: broker.smartThreshold must be between 0.0 and 1.0.");
            }
            this.storeImplementation = props.getProperty("broker.strategy.implementation", "TREE");
        } else {
            this.smartThreshold = -1.0; // Sentinel for SIMPLE
            this.storeImplementation = null;
        }
        
        // 2. Proximity Brake Configuration
        this.proximityBrakeEnabled = ConfigParser.parseBoolean(props, "proximity.brake.enabled", true);
        this.proximityBrakeLimit = ConfigParser.parseInt(props, "proximity.brake.limit", 5);
        this.proximityBrakeIntervalMs = ConfigParser.parseLong(props, "proximity.brake.interval_ms", 1000);
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
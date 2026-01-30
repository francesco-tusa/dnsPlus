package simulator.config;

import java.util.Properties;

public class BrokerConfig {
    
    public enum StrategyType { SIMPLE, SMART }

    public final StrategyType strategy;
    public final double smartThreshold;
    public final boolean intersectionOptimizationEnabled;
    public final String storeImplementation; 

    // Proximity Routing Brake Settings
    public final boolean proximityBrakeEnabled;
    public final int proximityBrakeLimit;

    public BrokerConfig(Properties props) {
        this.strategy = ConfigParser.parseEnum(props, "broker.strategy", StrategyType.class, StrategyType.SMART);
        this.intersectionOptimizationEnabled = ConfigParser.parseBoolean(props, "broker.optimization.region_intersection", true);

        if (this.strategy == StrategyType.SMART) {
            this.smartThreshold = ConfigParser.parseDouble(props, "broker.smartThreshold", 0.25);
            this.storeImplementation = props.getProperty("broker.strategy.implementation", "TREE");
        } else {
            this.smartThreshold = -1.0; 
            this.storeImplementation = null;
        }
        
        // --- PROXIMITY BRAKE CONFIG LOGIC ---
        int limit = ConfigParser.parseInt(props, "proximity.brake.limit", 5);
        boolean enabledInConfig = ConfigParser.parseBoolean(props, "proximity.brake.enabled", true);

        // If limit is -1, it overrides the 'enabled' flag to FALSE (NoOp)
        if (limit == -1) {
            this.proximityBrakeEnabled = false;
            this.proximityBrakeLimit = -1; // Sentinel
        } else {
            this.proximityBrakeEnabled = enabledInConfig;
            this.proximityBrakeLimit = limit;
        }
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
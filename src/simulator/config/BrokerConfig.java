package simulator.config;

import java.util.Properties;
import java.util.logging.Logger;
import utils.CustomLogger;

public class BrokerConfig {
    private static final Logger logger = CustomLogger.getLogger(BrokerConfig.class.getName());
    public enum StrategyType { SIMPLE, SMART }

    public final StrategyType strategy;
    public final double smartThreshold;
    
    public BrokerConfig(Properties props) {
        String stratStr = props.getProperty("broker.strategy", "SIMPLE").toUpperCase();
        try {
            this.strategy = StrategyType.valueOf(stratStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Config Error: Unknown broker.strategy '" + stratStr + "'. Valid: SIMPLE, SMART");
        }
        
        this.smartThreshold = parseDouble(props, "broker.smartThreshold", "0.5");
        
        validate();
    }
    
    private void validate() {
        if (strategy == StrategyType.SMART) {
            if (smartThreshold < 0.0 || smartThreshold > 1.0) {
                throw new IllegalArgumentException("Config Error: broker.smartThreshold must be between 0.0 and 1.0. Found: " + smartThreshold);
            }
        }
    }
    
    public boolean isSmartStrategy() {
        return strategy == StrategyType.SMART;
    }
    
    private double parseDouble(Properties props, String key, String defaultVal) {
        try { return Double.parseDouble(props.getProperty(key, defaultVal)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Config Error: Invalid double for " + key); }
    }
}
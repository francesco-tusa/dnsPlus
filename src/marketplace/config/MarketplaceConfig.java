package marketplace.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import simulator.config.ConfigParser;
import utils.CustomLogger;

public class MarketplaceConfig {
    private static final Logger logger = CustomLogger.getLogger(MarketplaceConfig.class.getName());
    
    private static MarketplaceConfig instance;

    // --- Configuration Fields ---
    public final Set<String> allowedCountries;
    
    // Provider Counts
    public final int cloudProviderCount;
    public final int fogProviderCount;
    public final int edgeProviderCount;

    // --- NEW: Aggregation Strategy Toggle ---
    public enum AggregationStrategy {
        HYPERCUBE
    }
    
    public final AggregationStrategy aggregationStrategy;

    public static synchronized MarketplaceConfig get() {
        if (instance == null) {
            instance = new MarketplaceConfig();
        }
        return instance;
    }

    private MarketplaceConfig() {
        Properties props = loadProperties();

        String defaultSlice = "United States";
        String sliceStr = props.getProperty("marketplace.topology.slice", defaultSlice);
        
        if (sliceStr.trim().isEmpty() || sliceStr.equalsIgnoreCase("WORLD")) {
            this.allowedCountries = new HashSet<>(); 
        } else {
            this.allowedCountries = Arrays.stream(sliceStr.split(","))
                                          .map(String::trim)
                                          .map(String::toUpperCase)
                                          .collect(Collectors.toSet());
        }

        this.cloudProviderCount = ConfigParser.parseInt(props, "marketplace.providers.cloud.count", 15);
        this.fogProviderCount = ConfigParser.parseInt(props, "marketplace.providers.fog.count", 50);
        this.edgeProviderCount = ConfigParser.parseInt(props, "marketplace.providers.edge.count", 200);
        
        // Parse the new strategy parameter (Defaults to HYPERCUBE if missing)
        String strategyStr = props.getProperty("marketplace.aggregation.strategy", "HYPERCUBE").toUpperCase();
        AggregationStrategy parsedStrategy = AggregationStrategy.HYPERCUBE;
        try {
            parsedStrategy = AggregationStrategy.valueOf(strategyStr);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid marketplace.aggregation.strategy: " + strategyStr + ". Defaulting to HYPERCUBE.");
        }
        this.aggregationStrategy = parsedStrategy;
        
        logger.info("Marketplace Config Loaded. Slice: " + allowedCountries + " | Strategy: " + this.aggregationStrategy);
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        String filePath = "resources/simulation.properties";
        try (FileInputStream input = new FileInputStream(filePath)) {
            props.load(input);
        } catch (IOException ex) {
            logger.severe("Error loading marketplace configuration: " + ex.getMessage());
        }
        return props;
    }
}
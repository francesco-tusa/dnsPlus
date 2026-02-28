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
    
    // Storage for batch overrides (Parallel to SimConfiguration)
    private static Properties programmedOverrides = null;

    // --- Configuration Fields ---
    public final Set<String> allowedCountries;
    public final int cloudProviderCount;
    public final int fogProviderCount;
    public final int edgeProviderCount;

    /**
     * Resets the configuration and applies overrides for batch parameter sweeps.
     */
    public static synchronized void resetAndOverride(Properties overrides) {
        programmedOverrides = overrides;
        instance = new MarketplaceConfig();
    }

    public static synchronized MarketplaceConfig get() {
        if (instance == null) {
            instance = new MarketplaceConfig();
        }
        return instance;
    }

    private MarketplaceConfig() {
        Properties props = loadProperties();

        // 1. Apply Overrides
        if (programmedOverrides != null) {
            props.putAll(programmedOverrides);
        }

        // 2. Parse Properties
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
        
        logger.info(String.format("Marketplace Config Loaded. Slice: %s | Providers [C:%d, F:%d, E:%d]", 
                allowedCountries, cloudProviderCount, fogProviderCount, edgeProviderCount));
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
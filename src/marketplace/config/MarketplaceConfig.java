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

    public static synchronized MarketplaceConfig get() {
        if (instance == null) {
            instance = new MarketplaceConfig();
        }
        return instance;
    }

    private MarketplaceConfig() {
        Properties props = loadProperties();

        // 1. Parse Allowed Countries (Slice)
        // Default to Trans-Atlantic (US + Major EU) if not specified
        String defaultSlice = "United States";
        String sliceStr = props.getProperty("marketplace.topology.slice", defaultSlice);
        
        if (sliceStr.trim().isEmpty() || sliceStr.equalsIgnoreCase("WORLD")) {
            this.allowedCountries = new HashSet<>(); // Empty = World
        } else {
            this.allowedCountries = Arrays.stream(sliceStr.split(","))
                                          .map(String::trim)
                                          .map(String::toUpperCase)
                                          .collect(Collectors.toSet());
        }

        // 2. Parse Provider Counts
        this.cloudProviderCount = ConfigParser.parseInt(props, "marketplace.providers.cloud.count", 15);
        this.fogProviderCount = ConfigParser.parseInt(props, "marketplace.providers.fog.count", 50);
        this.edgeProviderCount = ConfigParser.parseInt(props, "marketplace.providers.edge.count", 200);
        
        logger.info("Marketplace Config Loaded. Slice: " + allowedCountries);
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
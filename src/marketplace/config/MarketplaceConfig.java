package marketplace.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import marketplace.common.aggregation.AggregationStrategy;
import marketplace.common.aggregation.UnweightedAggregationStrategy;
import marketplace.common.aggregation.WeightedAggregationStrategy;
import marketplace.common.identifiers.PlaintextIdentifierFactory;
import marketplace.common.identifiers.RoutingIdentifierFactory;
import simulator.config.ConfigParser;
import utils.CustomLogger;

public class MarketplaceConfig {
    private static final Logger logger = CustomLogger.getLogger(MarketplaceConfig.class.getName());

    private static MarketplaceConfig instance;
    private static Properties programmedOverrides = null;

    public final Set<String> allowedCountries;
    public final int cloudProviderCount;
    public final int fogProviderCount;
    public final int edgeProviderCount;

    public final String routingStrategy;
    public final String baselineVendorPrefix;

    public final double strictBudgetProbability;
    public final double edgeWorkloadProbability;

    public final AggregationStrategy activeAggregationStrategy;
    public final RoutingIdentifierFactory routingCryptography;

    // --- SPATIAL DISTRIBUTION PHYSICS ---
    public final double edgeRadiusMin;
    public final double edgeRadiusMax;
    
    public final double fogRadiusMean;
    public final double fogRadiusStdDev;
    public final double fogRadiusMinClamp;
    public final double fogRadiusMaxClamp;

    public final double cloudRadiusMean;
    public final double cloudRadiusStdDev;
    public final double cloudRadiusMinClamp;
    public final double cloudRadiusMaxClamp;

    public final boolean enableSpatialClipping;

    // --- MULTI-TENANT CONTINUUM CAPACITIES ---
    public final int edgeTenantCapacity;
    public final int fogTenantCapacity;
    public final int cloudTenantCapacity;

    // --- COLD START PHYSICS ---
    public final int universalWarmCacheSize;

    // --- ML DATASET GENERATION VARIANCES ---
    public final boolean enableQosPolicySkew;
    public final double mlExplorationRate;
    
    // --- DATASETS ---
    public final String azureTraceFilePath;
    public final int azureTraceLimit;

    // --- FL Telemetry ---
    public final boolean collectFlTelemetry;


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

        if (programmedOverrides != null) props.putAll(programmedOverrides);

        String defaultSlice = "United States";
        String sliceStr = props.getProperty("marketplace.topology.slice", defaultSlice);

        if (sliceStr.trim().isEmpty() || sliceStr.equalsIgnoreCase("WORLD")) {
            this.allowedCountries = new HashSet<>();
        } else {
            this.allowedCountries = Arrays.stream(sliceStr.split(","))
                    .map(String::trim).map(String::toUpperCase).collect(Collectors.toSet());
        }

        this.cloudProviderCount = ConfigParser.parseInt(props, "marketplace.providers.cloud.count", 15);
        this.fogProviderCount = ConfigParser.parseInt(props, "marketplace.providers.fog.count", 50);
        this.edgeProviderCount = ConfigParser.parseInt(props, "marketplace.providers.edge.count", 200);

        this.edgeRadiusMin = ConfigParser.parseDouble(props, "marketplace.tier.edge.radius.min", 0.05);
        this.edgeRadiusMax = ConfigParser.parseDouble(props, "marketplace.tier.edge.radius.max", 0.45);

        this.fogRadiusMean = ConfigParser.parseDouble(props, "marketplace.tier.fog.radius.mean", 3.0);
        this.fogRadiusStdDev = ConfigParser.parseDouble(props, "marketplace.tier.fog.radius.stddev", 0.75);
        this.fogRadiusMinClamp = ConfigParser.parseDouble(props, "marketplace.tier.fog.radius.clamp.min", 1.5);
        this.fogRadiusMaxClamp = ConfigParser.parseDouble(props, "marketplace.tier.fog.radius.clamp.max", 5.0);

        this.cloudRadiusMean = ConfigParser.parseDouble(props, "marketplace.tier.cloud.radius.mean", 8.0);
        this.cloudRadiusStdDev = ConfigParser.parseDouble(props, "marketplace.tier.cloud.radius.stddev", 2.0);
        this.cloudRadiusMinClamp = ConfigParser.parseDouble(props, "marketplace.tier.cloud.radius.clamp.min", 5.0);
        this.cloudRadiusMaxClamp = ConfigParser.parseDouble(props, "marketplace.tier.cloud.radius.clamp.max", 15.0);

        this.enableSpatialClipping = ConfigParser.parseBoolean(props, "marketplace.topology.spatial_clipping", true);

        this.strictBudgetProbability = ConfigParser.parseDouble(props, "marketplace.workload.strict_budget.probability", 0.5);
        this.edgeWorkloadProbability = ConfigParser.parseDouble(props, "marketplace.workload.edge.probability", 0.35);

        String strategyType = ConfigParser.parseString(props, "marketplace.aggregation.strategy", "WEIGHTED");
        this.activeAggregationStrategy = "UNWEIGHTED".equalsIgnoreCase(strategyType) ? 
            new UnweightedAggregationStrategy() : new WeightedAggregationStrategy();

        this.routingStrategy = ConfigParser.parseString(props, "marketplace.routing.strategy", "WEIGHTED_UTILITY");
        this.baselineVendorPrefix = ConfigParser.parseString(props, "marketplace.baseline.vendor", "AWS_Cloud");

        String cryptoProp = ConfigParser.parseString(props, "marketplace.routing.cryptography", "plaintext");
        this.routingCryptography = new PlaintextIdentifierFactory();

        this.edgeTenantCapacity = ConfigParser.parseInt(props, "marketplace.tenant.capacity.edge", 10);
        this.fogTenantCapacity = ConfigParser.parseInt(props, "marketplace.tenant.capacity.fog", 100);
        this.cloudTenantCapacity = ConfigParser.parseInt(props, "marketplace.tenant.capacity.cloud", 5000);

        this.enableQosPolicySkew = ConfigParser.parseBoolean(props, "marketplace.workload.policy.skew_enabled", false);
        this.mlExplorationRate = ConfigParser.parseDouble(props, "marketplace.ml.exploration_rate", 0.0);
        
        this.universalWarmCacheSize = ConfigParser.parseInt(props, "marketplace.physics.warm_cache_size", 100);
        
        this.azureTraceFilePath = ConfigParser.parseString(props, "marketplace.workload.traces.file", "resources/azure_marketplace_top1M.csv");
        this.azureTraceLimit = ConfigParser.parseInt(props, "marketplace.workload.traces.limit", 1000); 
        this.collectFlTelemetry = ConfigParser.parseBoolean(props, "marketplace.enableTracing", false);
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (FileInputStream input = new FileInputStream("resources/simulation.properties")) {
            props.load(input);
        } catch (IOException ex) {
            logger.severe("Error loading marketplace configuration: " + ex.getMessage());
        }
        return props;
    }
}
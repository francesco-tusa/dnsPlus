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
import marketplace.workload.topics.FunctionDistributionStrategy;
import marketplace.workload.topics.SingleFunctionDistribution;
import simulator.config.ConfigParser;
import utils.CustomLogger;

public class MarketplaceConfig {
    private static final Logger logger = CustomLogger.getLogger(MarketplaceConfig.class.getName());

    private static MarketplaceConfig instance;
    private static Properties programmedOverrides = null;

    // --- Configuration Fields ---
    public final Set<String> allowedCountries;
    public final int cloudProviderCount;
    public final int fogProviderCount;
    public final int edgeProviderCount;

    // --- Routing Strategy Configuration ---
    public final String routingStrategy;
    public final String baselineVendorPrefix;

    public final double strictBudgetProbability;
    public final double edgeWorkloadProbability;

    public final AggregationStrategy activeAggregationStrategy;

    public final FunctionDistributionStrategy functionDistribution;
    public final RoutingIdentifierFactory routingCryptography;

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

        if (programmedOverrides != null) {
            props.putAll(programmedOverrides);
        }

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

        this.strictBudgetProbability = ConfigParser.parseDouble(props, "marketplace.workload.strict_budget.probability",
                0.5);
        this.edgeWorkloadProbability = ConfigParser.parseDouble(props, "marketplace.workload.edge.probability", 0.35);

        String strategyType = ConfigParser.parseString(props, "marketplace.aggregation.strategy", "WEIGHTED");
        if ("UNWEIGHTED".equalsIgnoreCase(strategyType)) {
            this.activeAggregationStrategy = new UnweightedAggregationStrategy();
        } else {
            this.activeAggregationStrategy = new WeightedAggregationStrategy();
        }

        this.routingStrategy = ConfigParser.parseString(props, "marketplace.routing.strategy", "WEIGHTED_UTILITY");
        this.baselineVendorPrefix = ConfigParser.parseString(props, "marketplace.baseline.vendor", "AWS_Cloud");

        String distributionProp = ConfigParser.parseString(props, "marketplace.workload.distribution", "single");
        if ("pareto".equalsIgnoreCase(distributionProp)) {
            // TODO: ParetoFunctionDistribution
            this.functionDistribution = new SingleFunctionDistribution();
        } else {
            this.functionDistribution = new SingleFunctionDistribution(); // Legacy default
        }

        // 2. Load the Cryptography
        String cryptoProp = ConfigParser.parseString(props, "marketplace.routing.cryptography", "plaintext");
        if ("paillier".equalsIgnoreCase(cryptoProp)) {
            // TODO: PaillierIdentifierFactory
            this.routingCryptography = new PlaintextIdentifierFactory();
        } else {
            this.routingCryptography = new PlaintextIdentifierFactory(); // Legacy default
        }
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
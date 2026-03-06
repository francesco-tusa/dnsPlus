package marketplace.simulations;

import java.util.List;
import java.util.logging.Logger;

import marketplace.analysis.MarketplaceGroundTruthCalculator;
import marketplace.analysis.MarketplaceMetricsCollector;
import marketplace.analysis.MarketplaceMetricsPrinter;
import marketplace.config.MarketplaceConfig;
import marketplace.population.MarketplaceProviderPlacementStrategy;
import marketplace.workload.MarketplaceWorkloadGenerator;
import simulator.config.SimConfiguration;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.population.PopulationBasedPublishersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.population.SubscribersPlacementStrategy;
import simulator.regions.BoundedBroker;
import simulator.simulations.performance.GeoNamesBasedRegionPerformanceSimulation;
import simulator.simulations.performance.metrics.MetricsPrinter;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import simulator.simulations.performance.metrics.groundtruth.GroundTruthCalculator;
import simulator.workload.SubscriptionWorkloadGenerator;
import utils.CsvMetricWriter;
import utils.CustomLogger;

/**
 * Abstract base for all Marketplace Continuum simulations.
 * <p>
 * It wires up the specific Marketplace components:
 * <ul>
 * <li><b>Subscribers:</b> Fixed Providers (Cloud/Fog/Edge)</li>
 * <li><b>Publishers:</b> Population-based Clients</li>
 * <li><b>Routing:</b> Service Matching (QoS + Location)</li>
 * <li><b>Workload:</b> ServiceOffers and ServiceRequests</li>
 * </ul>
 * Subclasses (Concrete Simulations) only need to define the 'main' method 
 * and the specific distribution of Service Requests (generatePublications).
 */
public abstract class AbstractMarketplaceContinuumSimulation extends GeoNamesBasedRegionPerformanceSimulation {

    protected static final Logger logger = CustomLogger.getLogger(AbstractMarketplaceContinuumSimulation.class.getName());

    // ==================================================================================
    //  MARKETPLACE WIRING (Common to all scenarios)
    // ==================================================================================

    @Override
    protected PublishersPlacementStrategy getPublisherPlacementStrategy() {
        return new PopulationBasedPublishersPlacement();
    }

    @Override
    protected SubscribersPlacementStrategy getSubscriberPlacementStrategy() {
        return new MarketplaceProviderPlacementStrategy();
    }
    
    @Override
    protected SubscriptionWorkloadGenerator getWorkloadGenerator() {
        return new MarketplaceWorkloadGenerator();
    }

    @Override
    protected GroundTruthCalculator createGroundTruthCalculator() {
        return new MarketplaceGroundTruthCalculator();
    }

    @Override
    protected MetricsPrinter createMetricsPrinter() {
        return new MarketplaceMetricsPrinter(logger);
    }

    @Override
    protected List<BoundedBroker> getInterestHotspots(BoundedBroker root) {
        // Marketplace doesn't use "Hotspots" because Providers are fixed infrastructure.
        return null;
    }

    @Override
    protected CsvMetricWriter.TraceMetricStrategy getTraceMetricStrategy() {
        return new CsvMetricWriter.MarketplaceTraceStrategy();
    }

    // ==================================================================================
    //  MARKETPLACE SPECIALIZATION: Deep Client Discovery
    // ==================================================================================

    /**
     * Overrides the default leaf-based collection.
     * In the Marketplace, Providers (Subscribers) exist at Root, Admin1, and Leaf levels.
     * We must traverse the entire tree to find them.
     */
    @Override
    protected void collectClients(List<BoundedBroker> ignoredLeafs) {
        logger.info(">>> Marketplace Mode: Performing Deep Tree Scan for Providers/Clients...");
        
        allSubscribers.clear();
        allPublishers.clear();
        
        if (this.rootNode != null) {
            collectRecursive(this.rootNode);
        }
        
        logger.info(">>> Deep Scan Complete. Collected " + allSubscribers.size() + " subscribers (Providers) and " + allPublishers.size() + " publishers (Clients).");
    }

    private void collectRecursive(TreeNode node) {
        if (node == null || node.getChildren() == null) return;

        for (TreeNode child : node.getChildren()) {
            if (child instanceof SubscriberWithLocation) {
                allSubscribers.add((SubscriberWithLocation) child);
            } else if (child instanceof PublisherWithLocation) {
                allPublishers.add((PublisherWithLocation) child);
            } else if (child instanceof BoundedBroker) {
                collectRecursive(child);
            }
        }
    }

    // ==================================================================================
    //  LOGGING & DIAGNOSTICS
    // ==================================================================================

    @Override
    protected void logWorkloadConfiguration() {
        MarketplaceConfig config = MarketplaceConfig.get();
        
        // Print a specialized banner for the FaaS topology and workload
        printBanner("MARKETPLACE TOPOLOGY & WORKLOAD");
        
        logConfigItem("Marketplace Slice", config.allowedCountries);
        logConfigItem("Cloud Providers", config.cloudProviderCount);
        logConfigItem("Fog Providers", config.fogProviderCount);
        logConfigItem("Edge Providers", config.edgeProviderCount);
        logConfigItem("Marketplace Clients", SimConfiguration.get().workload.numberOfReplicas);
        
        // New FaaS Workload Parameters
        logConfigItem("Edge Workload Probability", String.format("%.2f", config.edgeWorkloadProbability));
        logConfigItem("Strict Budget Probability", String.format("%.2f", config.strictBudgetProbability));
    }

    @Override
    protected void logSpecificConfiguration() {
        // 1. Let the parent classes print their standard network routing configs (Smart Broker, etc.)
        super.logSpecificConfiguration(); 
        
        MarketplaceConfig config = MarketplaceConfig.get();
        
        // 2. Print a specialized banner for the Multi-Objective FaaS logic
        printBanner("MARKETPLACE ROUTING LOGIC");
        
        logConfigItem("Marketplace Routing Strategy", config.routingStrategy);
        logConfigItem("Aggregation Strategy", config.activeAggregationStrategy.getClass().getSimpleName());
        logConfigItem("Baseline Vendor Target", config.baselineVendorPrefix);
    }

    @Override
    protected void collectAndPrintMetrics() {
        // Inject the Oracle into the newly created Collector
        MarketplaceMetricsCollector collector = new MarketplaceMetricsCollector((MarketplaceGroundTruthCalculator) truthCalculator);
        
        PerformanceMetricsData collected = collector.collect(this.rootNode, allSubscribers, allPublishers);
        this.lastRunMetrics = collected;
        
        createMetricsPrinter().print(collected);        
    }
}
package marketplace.simulations;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import marketplace.agents.MarketplaceClient;
import marketplace.analysis.MarketplaceGroundTruthCalculator;
import marketplace.analysis.MarketplaceMetricsCollector;
import marketplace.analysis.MarketplaceMetricsPrinter;
import marketplace.config.MarketplaceConfig;
import marketplace.config.factories.MarketplaceComponentFactory;
import marketplace.events.ServiceRequest;
import marketplace.population.MarketplaceClientPopulationPlacement;
import marketplace.workload.ClientDemandGenerator;
import marketplace.workload.ClientDemandProfile;
import marketplace.workload.MarketplaceWorkloadGenerator;
import marketplace.workload.topics.FunctionDistributionStrategy;
import simulator.config.SimConfiguration;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.population.PublishersPlacementStrategy;
import simulator.population.SubscribersPlacementStrategy;
import simulator.regions.BoundedBroker;
import simulator.simulations.performance.GeoNamesBasedRegionPerformanceSimulation;
import simulator.simulations.performance.metrics.MetricsPrinter;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import simulator.simulations.performance.metrics.groundtruth.GroundTruthCalculator;
import simulator.workload.SubscriptionWorkloadGenerator;
import simulator.events.PublicationWithLocation;
import utils.CsvMetricWriter;
import utils.CustomLogger;

public abstract class AbstractMarketplaceContinuumSimulation extends GeoNamesBasedRegionPerformanceSimulation {

    protected static final Logger logger = CustomLogger.getLogger(AbstractMarketplaceContinuumSimulation.class.getName());
    protected final MarketplaceComponentFactory componentFactory;

    public AbstractMarketplaceContinuumSimulation(MarketplaceComponentFactory factory) {
        this.componentFactory = factory;
    }

    protected abstract FunctionDistributionStrategy createDistributionStrategy();
    protected abstract ClientDemandGenerator createDemandGenerator();

    @Override
    protected PublishersPlacementStrategy getPublisherPlacementStrategy() {
        return new MarketplaceClientPopulationPlacement();
    }

    @Override
    protected SubscribersPlacementStrategy getSubscriberPlacementStrategy() {
        return componentFactory.createProviderPlacementStrategy();
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
        return null; 
    }

    @Override
    protected CsvMetricWriter.TraceMetricStrategy getTraceMetricStrategy() {
        return new CsvMetricWriter.MarketplaceTraceStrategy();
    }

    @Override
    protected List<PublicationWithLocation> generatePublications(List<PublisherWithLocation> publishers) {
        logger.info(">>> Generating Marketplace Multi-Objective Workload...");
        List<PublicationWithLocation> requests = new ArrayList<>();
        ClientDemandGenerator demandGenerator = createDemandGenerator();
        FunctionDistributionStrategy distribution = createDistributionStrategy();

        for (int i = 0; i < publishers.size(); i++) {
            MarketplaceClient client = (MarketplaceClient) publishers.get(i);
            long oracleId = distribution.selectClientFunction();
            ClientDemandProfile profile = demandGenerator.generateDemand(i, oracleId);
            ServiceRequest req = client.createServiceRequest(oracleId, profile.constraints(), profile.weights());
            requests.add(req);
        }
        return requests;
    }

    @Override
    protected void collectClients(List<BoundedBroker> ignoredLeafs) {
        logger.info(">>> Marketplace Mode: Performing Deep Tree Scan for Providers/Clients...");
        allSubscribers.clear();
        allPublishers.clear();
        if (this.rootNode != null) {
            collectRecursive(this.rootNode);
        }
        logger.info(">>> Deep Scan Complete. Collected " + allSubscribers.size() + " subscribers and " + allPublishers.size() + " publishers.");
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

    @Override
    protected void logWorkloadConfiguration() {
        MarketplaceConfig config = MarketplaceConfig.get();
        printBanner("MARKETPLACE TOPOLOGY & WORKLOAD");
        logConfigItem("Marketplace Slice", config.allowedCountries);
        logConfigItem("Cloud Providers", config.cloudProviderCount);
        logConfigItem("Fog Providers", config.fogProviderCount);
        logConfigItem("Edge Providers", config.edgeProviderCount);
        logConfigItem("Marketplace Clients", SimConfiguration.get().workload.numberOfReplicas);
        logConfigItem("Edge Workload Probability", String.format("%.2f", config.edgeWorkloadProbability));
        logConfigItem("Strict Budget Probability", String.format("%.2f", config.strictBudgetProbability));
    }

    @Override
    protected void logSpecificConfiguration() {
        super.logSpecificConfiguration(); 
        MarketplaceConfig config = MarketplaceConfig.get();
        printBanner("MARKETPLACE ROUTING LOGIC");
        logConfigItem("Marketplace Routing Strategy", config.routingStrategy);
        logConfigItem("Aggregation Strategy", config.activeAggregationStrategy.getClass().getSimpleName());
        logConfigItem("Baseline Vendor Target", config.baselineVendorPrefix);
    }

    @Override
    protected void collectAndPrintMetrics() {
        MarketplaceMetricsCollector collector = new MarketplaceMetricsCollector((MarketplaceGroundTruthCalculator) truthCalculator);
        PerformanceMetricsData collected = collector.collect(this.rootNode, allSubscribers, allPublishers);
        this.lastRunMetrics = collected;
        createMetricsPrinter().print(collected);        
    }
}
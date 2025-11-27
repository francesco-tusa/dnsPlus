package simulator.simulations.performance;

import java.util.logging.Logger; 
import simulator.config.SimConfiguration;
import simulator.entities.SubscriberWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.workload.LocationWorkloadGenerator;
import simulator.workload.SubscriptionWorkloadGenerator;
import utils.CustomLogger; 

public abstract class AbstractLocationPerformanceSimulation<C extends TopologyConfiguration, F extends AbstractTopologyFactory<C, BoundedBroker>> extends AbstractPerformanceSimulation<C, F> {

    private static final Logger logger = CustomLogger.getLogger(AbstractLocationPerformanceSimulation.class.getName());
    
    private final SubscriptionWorkloadGenerator workloadGenerator = new LocationWorkloadGenerator();

    public AbstractLocationPerformanceSimulation() {
        // No-arg constructor
    }

    @Override
    protected void logSpecificMetrics() {
        long total = SimConfiguration.get().workload.getTotalSubscribers();
        if (total > 0) {
            long matched = allSubscribers.stream().filter(s -> s.getnPublications() > 0).count();
            double rate = (double) matched / total * 100.0;
            logger.info("\n5. LOCATION-SPECIFIC METRICS:");
            logMetricItem("Matched Subscribers", matched);
            logMetricItem("Match Rate", String.format("%.2f%%", rate));
        }
    }
    
    @Override
    protected void executeScenarios() {
        logSectionHeader("Executing Location-Based Performance Scenario");

        if (allSubscribers.isEmpty()) return;

        long totalSubscriptions = allSubscribers.size();
        long interval = Math.max(1, totalSubscriptions / 10);

        logger.info("");
        logger.info(">>> Phase 1: Subscriptions... <<<");
        
        for (int i = 0; i < totalSubscriptions; i++) {
            SubscriberWithLocation sub = allSubscribers.get(i);
            
            SimulationSubscription s = workloadGenerator.generateSubscription(sub, null);
            sub.send(s); 

            if ((i + 1) % interval == 0) {
                logger.info(String.format("  ... %d / %d", (i + 1), totalSubscriptions));
            }
        }

        logger.info("");
        logger.info(">>> Phase 2: Publications... <<<");
        
        for (var pub : allPublishers) {
            simulator.core.Location pubLocation = this.rootNode.getRegion().getRandomLocation();
            pub.send(new simulator.events.PublicationWithLocation(pubLocation));
        }
        
        collectAndPrintMetrics();
    }
}
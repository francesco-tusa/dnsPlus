package simulator.simulations.performance;

import java.util.logging.Logger; // Import Logger
import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.regions.BoundedBroker;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import utils.CustomLogger; // Import CustomLogger

public abstract class AbstractLocationPerformanceSimulation<
    C extends TopologyConfiguration, 
    F extends AbstractTopologyFactory<C, BoundedBroker>
> extends AbstractPerformanceSimulation<C, F> {

    private static final Logger logger = CustomLogger.getLogger(AbstractLocationPerformanceSimulation.class.getName());

    public AbstractLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica, boolean enableCsvOutput) {
        super(numberOfReplicas, subscribersPerReplica, enableCsvOutput);
    }
    
    public AbstractLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica) {
        super(numberOfReplicas, subscribersPerReplica);
    }

    @Override
    protected void executeScenarios() {
        logger.info("\n--- Executing Location-Based Performance Scenario ---");
        if (allSubscribers.isEmpty() || allPublishers.isEmpty()) {
            logger.severe("No subscribers or publishers were created. Cannot run scenarios.");
            return;
        }

        long totalSubscriptions = allSubscribers.size();
        long progressInterval = Math.max(1, totalSubscriptions / 10);
        long nextProgressMark = progressInterval;

        logger.info("\n>>> Phase 1: Subscribers are sending location-based subscriptions... <<<");
        
        for (int i = 0; i < totalSubscriptions; i++) {
            SubscriberWithLocation sub = allSubscribers.get(i);
            // Subscribers send a subscription for their own location
            sub.send(new simulator.events.SubscriptionWithLocation(sub.getLocation())); 

            if ((i + 1) == nextProgressMark || (i + 1) == totalSubscriptions) {
                logger.info(String.format("  ... processed %d / %d subscriptions.", (i + 1), totalSubscriptions));
                nextProgressMark += progressInterval;
            }
        }

        logger.info("\n>>> Phase 2: All service replicas are sending their publications... <<<");
        for (var pub : allPublishers) {
            // Publishers send a publication from a random location
            Location pubLocation = getRandomLocationInRegion(this.rootNode.getRegion());
            pub.send(new simulator.events.PublicationWithLocation(pubLocation));
        }
        
        collectAndPrintMetrics();
    }

    @Override
    protected void collectAndPrintMetrics() {
        super.collectAndPrintMetrics();
        if (getTotalSubscribers() > 0) {
            long matchedSubscribers = allSubscribers.stream().filter(s -> s.getnPublications() > 0).count();
            double matchRate = (double) matchedSubscribers / getTotalSubscribers() * 100.0;
            logger.info(String.format("Subscriber Match Rate: %.2f%% (%d / %d)", 
                                      matchRate, matchedSubscribers, getTotalSubscribers()));
        }
    }
}
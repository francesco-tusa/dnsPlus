package simulator.simulations.performance;

import simulator.entities.SubscriberWithLocation;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;

/**
 * An abstract base class for performance simulations using LOCATION-BASED routing.
 * Now accepts ratio-based parameters and CSV flag.
 */
public abstract class AbstractLocationPerformanceSimulation<
    C extends TopologyConfiguration,
    F extends AbstractTopologyFactory<C, BrokerWithRegion>
> extends AbstractPerformanceSimulation<C, F> {

    /**
     * Main constructor with all flags.
     * @param numberOfReplicas Total number of publisher replicas.
     * @param subscribersPerReplica Number of subscribers for every replica.
     * @param enableCsvOutput True to write raw metrics to CSV files.
     */
    public AbstractLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica, boolean enableCsvOutput) {
        super(numberOfReplicas, subscribersPerReplica, enableCsvOutput); // Pass CSV flag up
    }
    
    /**
     * Constructor without CSV flag (defaults to false).
     */
    public AbstractLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica) {
        this(numberOfReplicas, subscribersPerReplica, false);
    }

    @Override
    protected final void executeScenarios() {
        System.out.println("\n--- Executing Location-Based Performance Scenario ---");
        if (allSubscribers.isEmpty()) {
            System.err.println("No subscribers were created. Cannot run scenarios.");
            return;
        }
        
        System.out.println("\n>>> Phase 1: Subscribers are sending location-based subscriptions... <<<");
        final int PROGRESS_INTERVAL = (int) Math.max(1000, getTotalSubscribers() / 10);
        for (int i = 0; i < allSubscribers.size(); i++) {
            SubscriberWithLocation subscriber = allSubscribers.get(i);
            SubscriptionWithLocation subscription = generateSubscriptionForSubscriber(subscriber);
            subscriber.send(subscription);
            if ((i + 1) % PROGRESS_INTERVAL == 0 || (i + 1) == allSubscribers.size()) {
                System.out.printf("  ... processed %d / %d subscriptions.%n", (i + 1), allSubscribers.size());
            }
        }

        System.out.println("\n>>> Phase 2: All service replicas are sending their publications... <<<");
        for(var publisher : allPublishers) {
            publisher.send(new simulator.events.PublicationWithLocation(publisher.getLocation()));
        }
        
        collectAndPrintMetrics();
    }

    protected SubscriptionWithLocation generateSubscriptionForSubscriber(SubscriberWithLocation subscriber) {
        // Location-based subscriptions are just for the subscriber's own location.
        return new SubscriptionWithLocation(subscriber.getLocation());
    }

    @Override
    protected void collectAndPrintMetrics() {
        super.collectAndPrintMetrics(); // Prints the common overhead and delivery metrics

        // This metric is specific to location-based routing
        long successfulNotifications = 0;
        for (SubscriberWithLocation subscriber : allSubscribers) {
            successfulNotifications += subscriber.getnPublications();
        }

        if (!allSubscribers.isEmpty()) {
            double averagePubsPerSub = (double) successfulNotifications / allSubscribers.size();
            System.out.printf("Average Publications per Subscriber: %.2f\n", averagePubsPerSub);
        }
    }
}
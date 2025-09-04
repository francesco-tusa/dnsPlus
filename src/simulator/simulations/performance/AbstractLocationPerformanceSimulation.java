package simulator.simulations.performance;

import java.util.List;
import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;

/**
 * An abstract base class for performance simulations using LOCATION-BASED routing.
 */
public abstract class AbstractLocationPerformanceSimulation<
    C extends TopologyConfiguration,
    F extends AbstractTopologyFactory<C, BrokerWithRegion>
> extends AbstractPerformanceSimulation<C, F> {

    @Override
    protected final void executeScenarios() {
        System.out.println("\n--- Executing Location-Based Performance Scenario ---");

        if (allSubscribers.isEmpty() || allPublishers.isEmpty()) {
            System.err.println("No subscribers or publishers were created. Cannot run scenarios.");
            return;
        }
        
        System.out.println("\n>>> Phase 1: Subscribers are sending location-based subscriptions... <<<");
        final int PROGRESS_INTERVAL = 10000;
        for (int i = 0; i < allSubscribers.size(); i++) {
            SubscriberWithLocation subscriber = allSubscribers.get(i);
            SubscriptionWithLocation subscription = generateSubscriptionForSubscriber(subscriber);
            subscriber.send(subscription);
            if ((i + 1) % PROGRESS_INTERVAL == 0) {
                System.out.printf("  ... processed %d / %d subscriptions.%n", (i + 1), allSubscribers.size());
            }
        }
        System.out.println("  ... all subscriptions sent.");

        System.out.println("\n>>> Phase 2: All service replicas are sending their publications... <<<");
        for(var publisher : allPublishers) {
            publisher.send(new simulator.events.PublicationWithLocation(publisher.getLocation()));
        }
        
        collectAndPrintMetrics();
    }

    protected SubscriptionWithLocation generateSubscriptionForSubscriber(SubscriberWithLocation subscriber) {
        Location locationOfInterest;
        if (random.nextDouble() < getRemoteInterestProbability()) {
            if (!allPublishers.isEmpty()) {
                locationOfInterest = allPublishers.get(random.nextInt(allPublishers.size())).getLocation();
            } else {
                locationOfInterest = subscriber.getLocation();
            }
        } else {
            locationOfInterest = subscriber.getLocation();
        }
        return new SubscriptionWithLocation(locationOfInterest);
    }
}
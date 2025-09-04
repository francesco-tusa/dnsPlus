package simulator.simulations.performance;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;

/**
 * An abstract base class for performance simulations using REGION-BASED routing.
 */
public abstract class AbstractRegionPerformanceSimulation<
    C extends TopologyConfiguration,
    F extends AbstractTopologyFactory<C, BrokerWithRegion>
> extends AbstractPerformanceSimulation<C, F> {

    protected abstract double getSubscriptionRegionSize();

    @Override
    protected void executeScenarios() {
        System.out.println("\n--- Executing Region-Based Performance Scenario ---");

        if (allSubscribers.isEmpty() || allPublishers.isEmpty()) {
            System.err.println("No subscribers or publishers were created. Cannot run scenarios.");
            return;
        }

        List<BrokerWithRegion> leafBrokers = findLeafBrokers(this.rootNode);
        
        System.out.println("\n>>> Phase 1: Subscribers are sending region-based subscriptions... <<<");
        final int PROGRESS_INTERVAL = 10000;
        for (int i = 0; i < allSubscribers.size(); i++) {
            SubscriberWithLocation subscriber = allSubscribers.get(i);
            SubscriptionWithRegion subscription = generateSubscriptionForSubscriber(subscriber, leafBrokers);
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
    
    protected SubscriptionWithRegion generateSubscriptionForSubscriber(SubscriberWithLocation subscriber, List<BrokerWithRegion> allLeafBrokers) {
        Region subscriptionRegion;
        if (random.nextDouble() < getRemoteInterestProbability()) {
            List<BrokerWithRegion> hubBrokers = findHubs(allLeafBrokers, getNumberOfReplicas());
            BrokerWithRegion remoteHub = hubBrokers.get(random.nextInt(hubBrokers.size()));
            subscriptionRegion = new Region(remoteHub.getRegion());
        } else {
            Location centerOfInterest = subscriber.getLocation();
            subscriptionRegion = new Region(
                new Location(centerOfInterest.getX() - (getSubscriptionRegionSize() / 2),
                             centerOfInterest.getY() - (getSubscriptionRegionSize() / 2), 0),
                new Location(centerOfInterest.getX() + (getSubscriptionRegionSize() / 2),
                             centerOfInterest.getY() + (getSubscriptionRegionSize() / 2), 0)
            );
        }
        return new SubscriptionWithRegion(subscriptionRegion);
    }
    
    protected List<BrokerWithRegion> findHubs(List<BrokerWithRegion> leafBrokers, int numHubs) {
        return leafBrokers.stream()
            .sorted(Comparator.comparingLong(BrokerWithRegion::getInternetPopulation).reversed())
            .limit(numHubs)
            .collect(Collectors.toList());
    }
}
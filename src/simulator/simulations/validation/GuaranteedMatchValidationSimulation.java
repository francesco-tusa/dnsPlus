package simulator.simulations.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import simulator.Location;
import simulator.PublisherWithLocation;
import simulator.SubscriberWithLocation;
import simulator.clients.ProportionalSubscriberGenerator;
import simulator.regions.LeafBrokerWithRegionProcessingRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.GridTopologyGenerator;

/**
 * A simulation designed specifically to validate the correctness of the broker
 * routing and matching logic.
 * <p>
 * This class uses an "omniscient" subscriber model where each subscriber knows
 * the location of its closest service replica and creates a subscription that
 * is guaranteed to match. Therefore, the expected "Subscriber Match Rate" for
 * this simulation is always 100%.
 */
public class GuaranteedMatchValidationSimulation extends AbstractTopologyValidationSimulation<
    GridTopologyConfiguration,
    GridTopologyGenerator
> {

    // --- Simulation Parameters ---
    private static final long TOTAL_SUBSCRIBERS = 1000;
    private static final int NUMBER_OF_REPLICAS = 5;

    // --- Data Collection ---
    private final List<SubscriberWithLocation> allSubscribers = new ArrayList<>();
    private final List<PublisherWithLocation> allPublishers = new ArrayList<>();

    @Override
    protected void executeScenarios() {
        System.out.println("\n--- Running System Validation Scenario ---");

        if (this.rootNode == null) {
            System.err.println("Cannot execute scenarios: Root node is null.");
            return;
        }

        // --- Setup Phase: Create and place all clients ---
        List<LeafBrokerWithRegionProcessingRegion> leafBrokers = this.topologyFactory.getLeafBrokers();
        if (leafBrokers == null || leafBrokers.isEmpty()) {
            System.err.println("Error: No leaf brokers found. Cannot attach clients.");
            return;
        }

        // 1. Create Subscribers
        ProportionalSubscriberGenerator subscriberGenerator = new ProportionalSubscriberGenerator();
        subscriberGenerator.generateAndAttach(this.rootNode, leafBrokers, TOTAL_SUBSCRIBERS);
        collectClients(leafBrokers);

        // 2. Create and place Replicas (Publishers)
        placeReplicas(leafBrokers);

        // --- Execution Phase ---
        System.out.println("\n>>> Subscribers are sending validation subscriptions... <<<");
        for (SubscriberWithLocation subscriber : allSubscribers) {
            SubscriptionWithRegion subscription = generateSubscriptionForSubscriber(subscriber);
            subscriber.send(subscription);
        }

        System.out.println("\n>>> Replicas are sending publications... <<<");
        for (PublisherWithLocation publisher : allPublishers) {
            publisher.send(new simulator.PublicationWithLocation(publisher.getLocation()));
        }

        System.out.println("\n--- Validation Test Complete ---");
        // In a real test suite, you would add assertions here to check the metrics.
        // For now, we can visually inspect the output.
        printAllBrokerSubscriptionTables();
    }

    /**
     * Creates a subscription guaranteed to match the closest service replica.
     */
    private SubscriptionWithRegion generateSubscriptionForSubscriber(SubscriberWithLocation subscriber) {
        PublisherWithLocation closestPublisher = findClosestPublisher(subscriber);
        Location centerOfInterest = closestPublisher.getLocation();

        Region subscriptionRegion = new Region(
            new Location(centerOfInterest.getX() - 5, centerOfInterest.getY() - 5, 0),
            new Location(centerOfInterest.getX() + 5, centerOfInterest.getY() + 5, 0)
        );
        
        return new SubscriptionWithRegion(subscriptionRegion);
    }
    
    /**
     * Places publisher replicas in the most populous regions of the topology.
     */
    private void placeReplicas(List<LeafBrokerWithRegionProcessingRegion> leafBrokers) {
        leafBrokers.sort((b1, b2) -> Long.compare(b2.getInternetPopulation(), b1.getInternetPopulation()));
        
        int hubCount = Math.min(NUMBER_OF_REPLICAS, leafBrokers.size());
        for (int i = 0; i < hubCount; i++) {
            LeafBrokerWithRegionProcessingRegion hub = leafBrokers.get(i);
            Location pubLocation = getRandomLocationInRegion(hub.getRegion());
            PublisherWithLocation replica = new PublisherWithLocation("ValidationReplica-" + i, pubLocation);
            hub.addChild(replica);
            allPublishers.add(replica);
        }
    }

    /**
     * Collects all created clients from the topology into lists for easy access.
     */
    private void collectClients(List<LeafBrokerWithRegionProcessingRegion> leafBrokers) {
        allSubscribers.clear();
        allPublishers.clear();
        for (LeafBrokerWithRegionProcessingRegion leaf : leafBrokers) {
            for (Object child : leaf.getChildren()) {
                if (child instanceof SubscriberWithLocation) {
                    allSubscribers.add((SubscriberWithLocation) child);
                } else if (child instanceof PublisherWithLocation) {
                    allPublishers.add((PublisherWithLocation) child);
                }
            }
        }
    }

    private PublisherWithLocation findClosestPublisher(SubscriberWithLocation subscriber) {
        PublisherWithLocation closest = null;
        double minDistanceSq = Double.MAX_VALUE;
        for (PublisherWithLocation publisher : allPublishers) {
            double distSq = distanceSquared(subscriber.getLocation(), publisher.getLocation());
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                closest = publisher;
            }
        }
        return closest;
    }

    private double distanceSquared(Location l1, Location l2) {
        double dx = l1.getX() - l2.getX();
        double dy = l1.getY() - l2.getY();
        return (dx * dx) + (dy * dy);
    }
    
    private Location getRandomLocationInRegion(Region region) {
        Random rand = new Random();
        double x = region.getBottomLeft().getX() + (region.getTopRight().getX() - region.getBottomLeft().getX()) * rand.nextDouble();
        double y = region.getBottomLeft().getY() + (region.getTopRight().getY() - region.getBottomLeft().getY()) * rand.nextDouble();
        return new Location(x, y, 0);
    }

    public static void main(String[] args) {
        System.out.println("--- Starting System Validation Simulation ---");
        System.out.println(">>> This simulation should result in a 100% match rate if the system is correct. <<<");
        
        GridTopologyConfiguration config = new GridTopologyConfiguration();
        GridTopologyGenerator factory = new GridTopologyGenerator();
        
        GuaranteedMatchValidationSimulation simulation = new GuaranteedMatchValidationSimulation();
        simulation.run(factory, config);
    }
}

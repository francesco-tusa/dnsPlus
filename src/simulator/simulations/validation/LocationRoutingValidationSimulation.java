package simulator.simulations.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.PublisherWithLocation;
import simulator.SubscriberWithLocation;
import simulator.SubscriptionWithLocation;
import simulator.clients.ProportionalSubscriberGenerator;
import simulator.regions.LeafBrokerWithRegionProcessingLocation;
import simulator.regions.Region;
import simulator.topology.grid.GridTopologyConfiguration;
import simulator.topology.grid.LocationGridTopologyGenerator;

/**
 * A simulation to validate that the location-based routing mechanism correctly
 * delivers the geographically closest service replica to each subscriber.
 */
public class LocationRoutingValidationSimulation extends AbstractTopologyValidationSimulation<
    GridTopologyConfiguration,
    LocationGridTopologyGenerator
> {

    // --- Simulation Parameters ---
    private static final long TOTAL_SUBSCRIBERS = 1000;
    private static final int NUMBER_OF_REPLICAS = 5;

    // --- Data Collection ---
    private final List<SubscriberWithLocation> allSubscribers = new ArrayList<>();
    private final List<PublisherWithLocation> allPublishers = new ArrayList<>();
    private final Map<SubscriberWithLocation, PublisherWithLocation> expectedClosestReplica = new HashMap<>();

    @Override
    protected void executeScenarios() {
        System.out.println("\n--- Running Location-Based Routing Validation Scenario ---");

        if (this.rootNode == null) {
            System.err.println("Cannot execute scenarios: Root node is null.");
            return;
        }

        // --- Setup Phase ---
        List<LeafBrokerWithRegionProcessingLocation> leafBrokers = this.topologyFactory.getLeafBrokers();
        if (leafBrokers == null || leafBrokers.isEmpty()) {
            System.err.println("Error: No leaf brokers found. Cannot attach clients.");
            return;
        }
        
        ProportionalSubscriberGenerator subscriberGenerator = new ProportionalSubscriberGenerator();
        subscriberGenerator.generateAndAttach(this.rootNode, leafBrokers, TOTAL_SUBSCRIBERS);
        collectClients(leafBrokers);
        placeReplicas(leafBrokers);

        // --- Pre-calculation Phase ---
        System.out.println("\n>>> Pre-calculating the optimal replica for each subscriber... <<<");
        for (SubscriberWithLocation subscriber : allSubscribers) {
            expectedClosestReplica.put(subscriber, findClosestPublisher(subscriber));
        }

        // --- Execution Phase ---
        System.out.println("\n>>> Subscribers are sending subscriptions... <<<");
        for (SubscriberWithLocation subscriber : allSubscribers) {
            subscriber.send(new SubscriptionWithLocation(subscriber.getLocation()));
        }

        System.out.println("\n>>> Replicas are sending publications... <<<");
        for (PublisherWithLocation publisher : allPublishers) {
            publisher.send(new simulator.PublicationWithLocation(publisher.getLocation()));
        }

        // --- Validation Phase ---
        validateResults();
    }

    private void validateResults() {
        System.out.println("\n--- Validating Results ---");
        int correctMatches = 0;
        int incorrectMatches = 0;
        int noMatches = 0;

        for (SubscriberWithLocation subscriber : allSubscribers) {
            PublicationWithLocation receivedPub = subscriber.getLastReceivedPublication();
            PublisherWithLocation expectedPub = expectedClosestReplica.get(subscriber);

            if (receivedPub == null) {
                noMatches++;
            } else if (receivedPub.getOriginalSource() == expectedPub) { // **THE FIX**
                correctMatches++;
            } else {
                incorrectMatches++;
                System.out.println("  MISMATCH for " + subscriber.getName() + ":");
                System.out.println("    - Expected: " + expectedPub.getName() + " at " + expectedPub.getLocation());
                // **THE FIX**: Cast getOriginalSource() to get the publisher's name
                System.out.println("    - Received: " + ((PublisherWithLocation)receivedPub.getOriginalSource()).getName() + " at " + receivedPub.getLocation());
            }
        }
        
        System.out.println("\n--- Validation Summary ---");
        System.out.println("Total Subscribers: " + allSubscribers.size());
        System.out.println("Subscribers who received the OPTIMAL replica: " + correctMatches);
        System.out.println("Subscribers who received a SUB-OPTIMAL replica: " + incorrectMatches);
        System.out.println("Subscribers who received NO replica: " + noMatches);

        double accuracy = (double) correctMatches / allSubscribers.size() * 100.0;
        System.out.printf("Optimal Routing Accuracy: %.2f%%\n", accuracy);
    }

    private void placeReplicas(List<LeafBrokerWithRegionProcessingLocation> leafBrokers) {
        leafBrokers.sort((b1, b2) -> Long.compare(b2.getInternetPopulation(), b1.getInternetPopulation()));
        int hubCount = Math.min(NUMBER_OF_REPLICAS, leafBrokers.size());
        for (int i = 0; i < hubCount; i++) {
            LeafBrokerWithRegionProcessingLocation hub = leafBrokers.get(i);
            Location pubLocation = getRandomLocationInRegion(hub.getRegion());
            PublisherWithLocation replica = new PublisherWithLocation("Replica-" + i, pubLocation);
            hub.addChild(replica);
            allPublishers.add(replica);
        }
    }

    private void collectClients(List<LeafBrokerWithRegionProcessingLocation> leafBrokers) {
        allSubscribers.clear();
        for (LeafBrokerWithRegionProcessingLocation leaf : leafBrokers) {
            for (Object child : leaf.getChildren()) {
                if (child instanceof SubscriberWithLocation) {
                    allSubscribers.add((SubscriberWithLocation) child);
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
        if (region == null || region.getBottomLeft() == null || region.getTopRight() == null) {
            return new Location(0,0,0); // Fallback
        }
        double x = region.getBottomLeft().getX() + (region.getTopRight().getX() - region.getBottomLeft().getX()) * rand.nextDouble();
        double y = region.getBottomLeft().getY() + (region.getTopRight().getY() - region.getBottomLeft().getY()) * rand.nextDouble();
        return new Location(x, y, 0);
    }

    public static void main(String[] args) {
        System.out.println("--- Starting Location-Based Routing Validation Simulation ---");
        
        GridTopologyConfiguration config = new GridTopologyConfiguration();
        LocationGridTopologyGenerator factory = new LocationGridTopologyGenerator();
        
        LocationRoutingValidationSimulation simulation = new LocationRoutingValidationSimulation();
        simulation.run(factory, config);
    }
}

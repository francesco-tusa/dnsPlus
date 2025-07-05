package simulator.simulations.validation;

import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.PublisherWithLocation;
import simulator.SubscriberWithLocation;
import simulator.topology.random.RegionProcessingRandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;

/**
 * Concrete SimulationRunner for the random region-based topology.
 * This class serves as a sanity check for the random topology generator.
 */
public class RandomTopologyValidationSimulation extends AbstractTopologyValidationSimulation<
    RegionRandomTopologyConfiguration,
    RegionProcessingRandomTopologyGenerator> {
    
    @Override
    protected void executeScenarios() {
        System.out.println("\n--- Running Region Simulation Scenarios (Random Topology) ---");

        SubscriberWithLocation firstSubscriber = findFirstNodeOfType(SubscriberWithLocation.class);
        PublisherWithLocation firstPublisher = findFirstNodeOfType(PublisherWithLocation.class);

        if (firstSubscriber != null) {
            System.out.println("\n>>> Scenario: Subscriber sends subscription <<<");
            Location subLoc = firstSubscriber.getLocation();
            Region subReg = new Region(
                new Location(subLoc.getX() - 5, subLoc.getY() - 5, subLoc.getZ() - 5),
                new Location(subLoc.getX() + 5, subLoc.getY() + 5, subLoc.getZ() + 5)
            );
            SubscriptionWithRegion subscription = new SubscriptionWithRegion(subReg);
            System.out.println(firstSubscriber.getName() + " subscribing to " + subReg);
            firstSubscriber.send(subscription);
            printAllBrokerSubscriptionTables();
        } else {
            System.out.println("Could not find a subscriber to run subscription scenario.");
        }

        if (firstPublisher != null && firstSubscriber != null) {
             System.out.println("\n>>> Scenario: Publisher sends matching publication <<<");
             Location pubLoc = firstSubscriber.getLocation();
             PublicationWithLocation publication = new PublicationWithLocation(pubLoc);
             System.out.println(firstPublisher.getName() + " publishing at " + pubLoc);
             firstPublisher.send(publication);
        } else if (firstPublisher != null) {
             System.out.println("\n>>> Scenario: Publisher sends publication (no specific target) <<<");
             Location pubLoc = firstPublisher.getLocation();
             PublicationWithLocation publication = new PublicationWithLocation(pubLoc);
             System.out.println(firstPublisher.getName() + " publishing at " + pubLoc);
             firstPublisher.send(publication);
        } else {
            System.out.println("Could not find a publisher to run publication scenario.");
        }

        System.out.println("\n--- Region Random Simulation Scenarios Complete ---");
    }

    public static void main(String[] args) {
        int treeDepth = 3;
        int maxBranchingFactor = 4;
        int numRegions = 4;
        int subscribersPerLeaf = 3;
        int publishersPerLeaf = 2;

        System.out.println("--- Starting Simulation Setup ---");
        System.out.println("Parameters (Random Topology):");
        System.out.println("  Tree Depth: " + treeDepth);
        System.out.println("  Max Branching Factor: " + maxBranchingFactor);
        System.out.println("  Number of Region Definitions: " + numRegions);
        System.out.println("  Subscribers per Leaf: " + subscribersPerLeaf);
        System.out.println("  Publishers per Leaf: " + publishersPerLeaf);
        System.out.println();

        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(
                treeDepth, maxBranchingFactor, numRegions, subscribersPerLeaf, publishersPerLeaf);

        RegionProcessingRandomTopologyGenerator factory = new RegionProcessingRandomTopologyGenerator();

        RandomTopologyValidationSimulation simulation = new RandomTopologyValidationSimulation();
        simulation.run(factory, config);
    }
}

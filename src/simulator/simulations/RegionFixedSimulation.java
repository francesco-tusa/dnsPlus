package simulator.simulations;

import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.PublisherWithLocation;
import simulator.SimulationRunner;
import simulator.SubscriberWithLocation;
import simulator.regions.BrokerWithRegionProcessingRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.fixed.FixedTopologyConfiguration;
import simulator.topology.fixed.FixedTopologyGenerator;

/**
 * Concrete SimulationRunner for the fixed manually defined test topology.
 */
public class RegionFixedSimulation extends SimulationRunner<
    FixedTopologyConfiguration,
    BrokerWithRegionProcessingRegion,
    FixedTopologyGenerator> {

    @Override
    protected void executeScenarios() {
        System.out.println("\n--- Running Fixed Topology Test Scenarios ---");

        // Find nodes needed using helper methods from SimulationRunner
        SubscriberWithLocation s2 = findNodeByName("sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s5 = findNodeByName("sub5", SubscriberWithLocation.class);
        PublisherWithLocation p1 = findNodeByName("pub1", PublisherWithLocation.class);
        PublisherWithLocation p2 = findNodeByName("pub2", PublisherWithLocation.class);

        // Check if nodes were found before proceeding
        if (s2 == null || s5 == null || p1 == null || p2 == null) {
            System.err.println("Error: Could not find all required nodes (sub2, sub5, pub1, pub2) for scenarios.");
            return;
        }

        // --- Send Subscriptions ---
        System.out.println("\n>>> Scenario: Sending Subscriptions <<<");
        Region subscription1Region = new Region(new Location(5, 2, 0), new Location(8, 5, 0));
        Region subscription2Region = new Region(new Location(6, 3, 0), new Location(7, 4, 0));
        Region subscription3Region = new Region(new Location(4, 2, 0), new Location(7, 4, 0));
        Region subscription4Region = new Region(new Location(2, 2, 0), new Location(3, 3, 0));
        Region subscription5Region = new Region(new Location(2, 2, 0), new Location(4, 3, 0));

        System.out.println("\nSending Subscription 1 from " + s2.getName() + ": " + subscription1Region);
        s2.send(new SubscriptionWithRegion(subscription1Region));

        System.out.println("\nSending Subscription 2 from " + s2.getName() + ": " + subscription2Region);
        s2.send(new SubscriptionWithRegion(subscription2Region));

        System.out.println("\nSending Subscription 3 from " + s2.getName() + ": " + subscription3Region);
        s2.send(new SubscriptionWithRegion(subscription3Region));

        System.out.println("\nSending Subscription 4 from " + s5.getName() + ": " + subscription4Region);
        s5.send(new SubscriptionWithRegion(subscription4Region));

        System.out.println("\nSending Subscription 5 from " + s5.getName() + ": " + subscription5Region);
        s5.send(new SubscriptionWithRegion(subscription5Region));

        printAllBrokerSubscriptionTables(); // Print tables after subscriptions

        // --- Send Publications ---
        System.out.println("\n>>> Scenario: Sending Publications <<<");
        Location pubLoc1 = new Location(9, 3, 0); // No match expected
        Location pubLoc2 = new Location(7, 3, 0); // Match s2 expected
        Location pubLoc3 = new Location(3, 3, 0); // Match s5 expected
        Location pubLoc4 = new Location(19, 4, 0); // No match expected

        System.out.println("\nSending Pub1 from " + p1.getName() + " at " + pubLoc1);
        p1.send(new PublicationWithLocation(pubLoc1));
        System.out.println("\nSending Pub2 from " + p1.getName() + " at " + pubLoc2);
        p1.send(new PublicationWithLocation(pubLoc2));
        System.out.println("\nSending Pub3 from " + p1.getName() + " at " + pubLoc3);
        p1.send(new PublicationWithLocation(pubLoc3));
        System.out.println("\nSending Pub4 from " + p2.getName() + " at " + pubLoc4);
        p2.send(new PublicationWithLocation(pubLoc4));

        System.out.println("\n--- Fixed Topology Test Scenarios Complete ---");
    }

    public static void main(String[] args) {
        System.out.println("--- Starting Fixed Topology Test Run ---");

        // --- Configuration ---
        FixedTopologyConfiguration config = new FixedTopologyConfiguration();

        // --- Factory ---
        FixedTopologyGenerator factory = new FixedTopologyGenerator();

        // --- Create and Run the Specific Simulation ---
        RegionFixedSimulation simulation = new RegionFixedSimulation();
        simulation.run(factory, config);
    }
}

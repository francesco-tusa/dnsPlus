package simulator.simulations;

import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.PublisherWithLocation;
import simulator.SubscriberWithLocation;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.fixed.FixedTestTopologyConfiguration;
import simulator.topology.fixed.FixedTestTopologyGenerator;

/**
 * Concrete SimulationRunner for the fixed manually defined test topology.
 * This class is designed for unit testing and validation of core logic.
 * It now extends AbstractTopologyValidationSimulation for better classification.
 */
public class FixedTopologyValidationSimulation extends AbstractTopologyValidationSimulation<
    FixedTestTopologyConfiguration,
    FixedTestTopologyGenerator> {

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

        s2.send(new SubscriptionWithRegion(subscription1Region));
        s2.send(new SubscriptionWithRegion(subscription2Region));
        s2.send(new SubscriptionWithRegion(subscription3Region));
        s5.send(new SubscriptionWithRegion(subscription4Region));
        s5.send(new SubscriptionWithRegion(subscription5Region));

        printAllBrokerSubscriptionTables();

        // --- Send Publications ---
        System.out.println("\n>>> Scenario: Sending Publications <<<");
        p1.send(new PublicationWithLocation(new Location(9, 3, 0)));
        p1.send(new PublicationWithLocation(new Location(7, 3, 0)));
        p1.send(new PublicationWithLocation(new Location(3, 3, 0)));
        p2.send(new PublicationWithLocation(new Location(19, 4, 0)));

        System.out.println("\n--- Fixed Topology Test Scenarios Complete ---");
    }

    public static void main(String[] args) {
        System.out.println("--- Starting Fixed Topology Test Run ---");
        FixedTestTopologyConfiguration config = new FixedTestTopologyConfiguration();
        FixedTestTopologyGenerator factory = new FixedTestTopologyGenerator();
        FixedTopologyValidationSimulation simulation = new FixedTopologyValidationSimulation();
        simulation.run(factory, config);
    }
}

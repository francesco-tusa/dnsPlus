package simulator;

import simulator.regions.BrokerWithRegion;
import simulator.regions.BrokerWithRegionProcessingRegion;
import simulator.regions.LeafBrokerWithRegionProcessingRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;

import java.util.List;

/**
 * A class for testing simulation logic with a manually created, static topology.
 * This allows for predictable testing of broker interactions, region propagation,
 * and subscription/publication matching.
 */
public class ManualTopologyTest {

    public static void main(String[] args) {
        System.out.println("--- Starting Manual Topology Test ---");

        // 1. Create Nodes
        Nodes nodes = createNodes();

        // 2. Build Topology Structure
        buildTopologyStructure(nodes);

        // 3. Attach Subscribers and Publishers (Triggers Region Updates)
        attachSubscribersAndPublishers(nodes);

        // 4. Verify Initial Regions (Post-Attachment)
        verifyInitialRegions(nodes);

        // 5. Send Subscriptions and Observe Changes
        sendSubscriptionsAndObserve(nodes);

        // 6. Print All Final Subscription Tables
        printFinalSubscriptionTables(nodes);

        // 7. Send Publications
        sendPublications(nodes);

        System.out.println("\n--- Manual Topology Test Complete ---");
    }

    /**
     * Helper record to hold all created nodes for easy passing between methods.
     * Using specific types for clarity in this manual test setup.
     */
    private record Nodes(
        BrokerWithRegionProcessingRegion root,
        BrokerWithRegionProcessingRegion child1,
        BrokerWithRegionProcessingRegion child2,
        BrokerWithRegionProcessingRegion child3,
        LeafBrokerWithRegionProcessingRegion grandchild1,
        LeafBrokerWithRegionProcessingRegion grandchild2,
        LeafBrokerWithRegionProcessingRegion grandchild3,
        LeafBrokerWithRegionProcessingRegion grandchild4,
        List<SubscriberWithLocation> subscribers,
        List<PublisherWithLocation> publishers
    ) {}

    /**
     * Creates all the broker, subscriber, and publisher nodes for the test
     * by calling type-specific helper methods.
     * @return A Nodes record containing all created nodes.
     */
    private static Nodes createNodes() {
        System.out.println("\n--- Creating Nodes ---");
        BrokerNodes brokerNodes = createBrokers();
        List<SubscriberWithLocation> subscribers = createSubscribers();
        List<PublisherWithLocation> publishers = createPublishers();

        return new Nodes(
            brokerNodes.root, brokerNodes.child1, brokerNodes.child2, brokerNodes.child3,
            brokerNodes.grandchild1, brokerNodes.grandchild2, brokerNodes.grandchild3, brokerNodes.grandchild4,
            subscribers,
            publishers
        );
    }

    /**
     * Helper record specifically for broker nodes created.
     */
    private record BrokerNodes(
        BrokerWithRegionProcessingRegion root,
        BrokerWithRegionProcessingRegion child1,
        BrokerWithRegionProcessingRegion child2,
        BrokerWithRegionProcessingRegion child3,
        LeafBrokerWithRegionProcessingRegion grandchild1,
        LeafBrokerWithRegionProcessingRegion grandchild2,
        LeafBrokerWithRegionProcessingRegion grandchild3,
        LeafBrokerWithRegionProcessingRegion grandchild4
    ) {}

    /**
     * Creates all the broker nodes for the test topology.
     * @return A BrokerNodes record containing the created brokers.
     */
    private static BrokerNodes createBrokers() {
        System.out.println("  Creating Brokers...");
        BrokerWithRegionProcessingRegion root = new BrokerWithRegionProcessingRegion("root");
        BrokerWithRegionProcessingRegion child1 = new BrokerWithRegionProcessingRegion("child1");
        BrokerWithRegionProcessingRegion child2 = new BrokerWithRegionProcessingRegion("child2");
        BrokerWithRegionProcessingRegion child3 = new BrokerWithRegionProcessingRegion("child3");
        LeafBrokerWithRegionProcessingRegion grandchild1 = new LeafBrokerWithRegionProcessingRegion("grandchild1");
        LeafBrokerWithRegionProcessingRegion grandchild2 = new LeafBrokerWithRegionProcessingRegion("grandchild2");
        LeafBrokerWithRegionProcessingRegion grandchild3 = new LeafBrokerWithRegionProcessingRegion("grandchild3");
        LeafBrokerWithRegionProcessingRegion grandchild4 = new LeafBrokerWithRegionProcessingRegion("grandchild4");
        System.out.println("  Brokers created.");
        return new BrokerNodes(root, child1, child2, child3, grandchild1, grandchild2, grandchild3, grandchild4);
    }

    /**
     * Creates all the subscriber nodes for the test.
     * @return A List containing the created subscribers.
     */
    private static List<SubscriberWithLocation> createSubscribers() {
        System.out.println("  Creating Subscribers...");
        List<SubscriberWithLocation> subscribers = List.of(
            new SubscriberWithLocation("sub1", new Location(0, 0, 0)),
            new SubscriberWithLocation("sub2", new Location(5, 2, 0)),
            new SubscriberWithLocation("sub3", new Location(10, 1, 0)),
            new SubscriberWithLocation("sub4", new Location(15, 1, 0)),
            new SubscriberWithLocation("sub5", new Location(4, 3, 0)),
            new SubscriberWithLocation("sub6", new Location(9, 5, 0)),
            new SubscriberWithLocation("sub7", new Location(13, 3, 0)),
            new SubscriberWithLocation("sub8", new Location(20, 5, 0))
        );
        System.out.println("  Subscribers created.");
        return subscribers;
    }

    /**
     * Creates the publisher nodes for the test.
     * @return A List containing the created publishers.
     */
    private static List<PublisherWithLocation> createPublishers() { // Renamed and returns List
        System.out.println("  Creating Publishers...");
        PublisherWithLocation p1 = new PublisherWithLocation("pub1", new Location(7, 7, 0));
        PublisherWithLocation p2 = new PublisherWithLocation("pub2", new Location(18, 4, 0)); // New publisher
        List<PublisherWithLocation> publishers = List.of(p1, p2);
        System.out.println("  Publishers created.");
        return publishers;
    }


    /**
     * Connects the brokers to form the tree hierarchy.
     * @param nodes The Nodes record containing the brokers.
     */
    private static void buildTopologyStructure(Nodes nodes) {
        System.out.println("\n--- Building Topology Structure ---");
        nodes.root.addChild(nodes.child1);
        nodes.root.addChild(nodes.child2);
        nodes.root.addChild(nodes.child3);

        nodes.child1.addChild(nodes.grandchild1);
        nodes.child2.addChild(nodes.grandchild2);
        nodes.child2.addChild(nodes.grandchild3);
        nodes.child3.addChild(nodes.grandchild4);
        System.out.println("Broker hierarchy established.");
    }

    /**
     * Attaches subscribers and publishers to their respective leaf brokers.
     * This action triggers the bottom-up region calculation.
     * @param nodes The Nodes record containing all nodes.
     */
    private static void attachSubscribersAndPublishers(Nodes nodes) {
        System.out.println("\n--- Attaching Subscribers & Publishers ---");
        // Attach subscribers
        List<SubscriberWithLocation> subs = nodes.subscribers;
        nodes.grandchild1.addChild(subs.get(0)); // s1
        nodes.grandchild2.addChild(subs.get(1)); // s2
        nodes.grandchild3.addChild(subs.get(2)); // s3
        nodes.grandchild4.addChild(subs.get(3)); // s4

        nodes.grandchild1.addChild(subs.get(4)); // s5
        nodes.grandchild2.addChild(subs.get(5)); // s6
        nodes.grandchild3.addChild(subs.get(6)); // s7
        nodes.grandchild4.addChild(subs.get(7)); // s8

        // Attach publishers
        List<PublisherWithLocation> pubs = nodes.publishers;
        nodes.grandchild1.addChild(pubs.get(0)); // p1
        nodes.grandchild4.addChild(pubs.get(1)); // p2 attached to grandchild4
        System.out.println("Subscribers and Publishers attached to leaf brokers.");
    }

    /**
     * Prints the regions of brokers at different levels after node attachment.
     * @param nodes The Nodes record containing the brokers.
     */
    private static void verifyInitialRegions(Nodes nodes) {
        System.out.println("\n--- Verifying Initial Regions (Post-Attachment) ---");
        printLeafRegions(nodes.grandchild1, nodes.grandchild2, nodes.grandchild3, nodes.grandchild4);
        printIntermediateRegions(nodes.child1, nodes.child2, nodes.child3);
        printRootRegion(nodes.root);
    }

    /**
     * Sends a series of subscriptions and prints relevant tables to show the effect.
     * @param nodes The Nodes record containing the relevant subscribers and brokers.
     */
    private static void sendSubscriptionsAndObserve(Nodes nodes) {
        System.out.println("\n--- Sending Subscriptions ---");
        // Define subscription regions
        Region subscription1Region = new Region(new Location(5, 2, 0), new Location(8, 5, 0));
        Region subscription2Region = new Region(new Location(6, 3, 0), new Location(7, 4, 0)); // Contained within sub1
        Region subscription3Region = new Region(new Location(4, 2, 0), new Location(7, 4, 0)); // Overlaps sub1 & sub2
        Region subscription4Region = new Region(new Location(2, 2, 0), new Location(3, 3, 0));
        Region subscription5Region = new Region(new Location(2, 2, 0), new Location(4, 3, 0)); // Expands sub4

        // Get specific subscribers from the list
        SubscriberWithLocation s2 = nodes.subscribers.get(1);
        SubscriberWithLocation s5 = nodes.subscribers.get(4);

        // Initial state for a specific path
        System.out.println("\nInitial Tables (Before Subscriptions):");
        printBrokerSubscriptionTable(nodes.grandchild2);
        printBrokerSubscriptionTable(nodes.child2);
        printBrokerSubscriptionTable(nodes.root);

        // Send subscriptions and print tables to observe changes
        System.out.println("\nSending Subscription 1 from " + s2.getName() + ": " + subscription1Region);
        s2.send(new SubscriptionWithRegion(subscription1Region));
        printBrokerSubscriptionTable(nodes.grandchild2);
        printBrokerSubscriptionTable(nodes.child2);
        printBrokerSubscriptionTable(nodes.root);

        System.out.println("\nSending Subscription 2 from " + s2.getName() + ": " + subscription2Region);
        s2.send(new SubscriptionWithRegion(subscription2Region)); // Should update existing entry
        printBrokerSubscriptionTable(nodes.grandchild2);
        printBrokerSubscriptionTable(nodes.child2);
        printBrokerSubscriptionTable(nodes.root);

        System.out.println("\nSending Subscription 3 from " + s2.getName() + ": " + subscription3Region);
        s2.send(new SubscriptionWithRegion(subscription3Region)); // Should expand entry
        printBrokerSubscriptionTable(nodes.grandchild2);
        printBrokerSubscriptionTable(nodes.child2);
        printBrokerSubscriptionTable(nodes.root);

        System.out.println("\nSending Subscription 4 from " + s5.getName() + ": " + subscription4Region);
        s5.send(new SubscriptionWithRegion(subscription4Region));
        printBrokerSubscriptionTable(nodes.grandchild1); // Check other path
        printBrokerSubscriptionTable(nodes.child1);
        printBrokerSubscriptionTable(nodes.root);

        System.out.println("\nSending Subscription 5 from " + s5.getName() + ": " + subscription5Region);
        s5.send(new SubscriptionWithRegion(subscription5Region)); // Should expand entry
        printBrokerSubscriptionTable(nodes.grandchild1);
        printBrokerSubscriptionTable(nodes.child1);
        printBrokerSubscriptionTable(nodes.root);
    }

    /**
     * Prints the final subscription tables for all brokers in the topology.
     * @param nodes The Nodes record containing all brokers.
     */
    private static void printFinalSubscriptionTables(Nodes nodes) {
        System.out.println("\n--- Final Subscription Tables ---");
        List<BrokerWithRegion> allBrokers = List.of(
            nodes.root, nodes.child1, nodes.child2, nodes.child3,
            nodes.grandchild1, nodes.grandchild2, nodes.grandchild3, nodes.grandchild4
        );
        for (BrokerWithRegion broker : allBrokers) {
            printBrokerSubscriptionTable(broker);
        }
    }

    /**
     * Sends a series of publications from the first publisher.
     * @param nodes The Nodes record containing the publishers.
     */
    private static void sendPublications(Nodes nodes) {
        // Get the first publisher from the list
        if (nodes.publishers == null || nodes.publishers.isEmpty()) {
            System.out.println("\nNo publishers available to send publications.");
            return;
        }
        PublisherWithLocation p1 = nodes.publishers.get(0);

        System.out.println("\n--- Sending Publications from " + p1.getName() + " ---");
        // Publication locations
        Location pubLoc1 = new Location(9, 3, 0); // Should not match any subscription
        Location pubLoc2 = new Location(7, 3, 0); // Should match s2's expanded subscription
        Location pubLoc3 = new Location(3, 3, 0); // Should match s5's expanded subscription

        System.out.println("\nSending Publication 1 at " + pubLoc1);
        p1.send(new PublicationWithLocation(pubLoc1));

        System.out.println("\nSending Publication 2 at " + pubLoc2);
        p1.send(new PublicationWithLocation(pubLoc2));

        System.out.println("\nSending Publication 3 at " + pubLoc3);
        p1.send(new PublicationWithLocation(pubLoc3));

        PublisherWithLocation p2 = nodes.publishers.get(1);

        System.out.println("\n--- Sending Publications from " + p2.getName() + " ---");
        Location pubLoc4 = new Location(19, 4, 0);
        System.out.println("\nSending Publication 4 at " + pubLoc4);
        p2.send(new PublicationWithLocation(pubLoc4)); 
    }


    // --- Helper Methods for Printing ---

    private static void printLeafRegions(BrokerWithRegion... brokers) {
        System.out.println("Leaf Broker Regions:");
        for (BrokerWithRegion broker : brokers) {
            System.out.println("  " + broker.getName() + ": " + broker.getRegion());
        }
    }

    private static void printIntermediateRegions(BrokerWithRegion... brokers) {
        System.out.println("Intermediate Broker Regions:");
        for (BrokerWithRegion broker : brokers) {
            System.out.println("  " + broker.getName() + ": " + broker.getRegion());
        }
    }

    private static void printRootRegion(BrokerWithRegion root) {
        System.out.println("Root Broker Region:");
        System.out.println("  " + root.getName() + ": " + root.getRegion());
    }

    private static void printBrokerSubscriptionTable(BrokerWithRegion broker) {
        if (broker != null) {
            broker.printSubscriptionsTable();
        }
    }
}
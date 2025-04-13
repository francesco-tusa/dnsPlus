package simulator;

import java.util.LinkedList;
import java.util.Queue;

import simulator.regions.BrokerWithRegion;
import simulator.regions.BrokerWithRegionProcessingRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.RegionProcessingTopologyGenerator;
import simulator.topology.RegionTopologyConfiguration;
import simulator.topology.TopologyFactory;

public class SimulationMain {

    public static void main(String[] args) {
        // --- Simulation Parameters ---
        int treeDepth = 3;              // Root (0), Intermediate (1), Leaves (2)
        int maxBranchingFactor = 4;     // Max children per non-leaf broker
        int numRegions = 4;             // Distinct region definitions for placement
        int subscribersPerLeaf = 3;     // Subscribers per leaf broker
        int publishersPerLeaf = 2;      // Publishers per leaf broker

        System.out.println("--- Starting Simulation Setup ---");
        System.out.println("Parameters:");
        System.out.println("  Tree Depth: " + treeDepth);
        System.out.println("  Max Branching Factor: " + maxBranchingFactor);
        System.out.println("  Number of Region Definitions: " + numRegions);
        System.out.println("  Subscribers per Leaf: " + subscribersPerLeaf);
        System.out.println("  Publishers per Leaf: " + publishersPerLeaf);
        System.out.println();

        // --- Configuration ---
        RegionTopologyConfiguration config = new RegionTopologyConfiguration(
                treeDepth,
                maxBranchingFactor,
                numRegions,
                subscribersPerLeaf,
                publishersPerLeaf
        );

        // --- Generator ---
        TopologyFactory generator = new RegionProcessingTopologyGenerator();
        TreeNode rootNode = null;

        // --- Generate Topology ---
        System.out.println("--- Generating Topology ---");
        try {
            rootNode = generator.generateTopology(config);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("Topology generation failed: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed error
            System.exit(1);
        }

        if (rootNode == null) {
            System.err.println("Topology generation failed for an unknown reason.");
            System.exit(1);
        }
        System.out.println("--- Topology Generation Complete ---");
        System.out.println();


        // --- Verify Root Type and Print Topology ---
        BrokerWithRegionProcessingRegion rootBroker = null;
        if (rootNode instanceof BrokerWithRegionProcessingRegion) {
             rootBroker = (BrokerWithRegionProcessingRegion) rootNode;
             System.out.println("--- Generated Topology Structure ---");
             printTree(rootBroker, 0);
             System.out.println("----------------------------------");
             System.out.println("Final Root Region: " + rootBroker.getRegion()); // Check final propagated region
             System.out.println();
        } else {
             System.err.println("Generated root node is not of the expected type BrokerWithRegionProcessingRegion.");
             System.exit(1);
        }


        // --- Basic Simulation Example ---
        System.out.println("--- Running Basic Simulation ---");

        // 1. Find a subscriber and a publisher (e.g., the first ones generated)
        SubscriberWithLocation firstSubscriber = findFirstSubscriber(rootBroker);
        PublisherWithLocation firstPublisher = findFirstPublisher(rootBroker);

        if (firstSubscriber != null) {
            // 2. Subscriber sends a subscription
            System.out.println("\n>>> Scenario: Subscriber sends subscription <<<");
            Location subLoc = firstSubscriber.getLocation();
            Region subReg = new Region(
                new Location(subLoc.getX() - 5, subLoc.getY() - 5, subLoc.getZ() - 5),
                new Location(subLoc.getX() + 5, subLoc.getY() + 5, subLoc.getZ() + 5)
            );
            SubscriptionWithRegion subscription = new SubscriptionWithRegion(subReg);
            firstSubscriber.send(subscription);

            System.out.println("\n--- Subscription Tables after Subscribing ---");
            printAllSubscriptionTables(rootBroker);
            System.out.println("-------------------------------------------");

        } else {
            System.out.println("Could not find a subscriber to run simulation scenario.");
        }

        if (firstPublisher != null && firstSubscriber != null) {
             System.out.println("\n>>> Scenario: Publisher sends matching publication <<<");
             Location pubLoc = firstSubscriber.getLocation(); // Use subscriber's location for guaranteed match
             PublicationWithLocation publication = new PublicationWithLocation(pubLoc);
             firstPublisher.send(publication);

        } else if (firstPublisher != null) {
             System.out.println("\n>>> Scenario: Publisher sends publication (no specific target) <<<");
             Location pubLoc = firstPublisher.getLocation();
             PublicationWithLocation publication = new PublicationWithLocation(pubLoc);
             firstPublisher.send(publication);
        }
        else {
            System.out.println("Could not find a publisher to run simulation scenario.");
        }

        System.out.println("\n--- Simulation Complete ---");
    }

    // --- Helper Methods (printTree, findFirstSubscriber, findFirstPublisher, printAllSubscriptionTables) 
    /**
     * Prints the tree structure starting from the given node.
     */
    public static void printTree(TreeNode node, int level) {
        if (node == null) return;
        // Indentation
        for (int i = 0; i < level; i++) System.out.print("  ");

        // Node info - Add level information here
        System.out.print("- [L" + level + "] " + node.getName() + " (" + node.getClass().getSimpleName() + ")"); // Added [L<level>]
        if (node instanceof BrokerWithRegion broker) {
             System.out.print(" Region: " + broker.getRegion());
        } else if (node instanceof SubscriberWithLocation sub) {
             System.out.print(" Location: " + sub.getLocation());
        } else if (node instanceof PublisherWithLocation pub) {
             System.out.print(" Location: " + pub.getLocation());
        }
        System.out.println();

        // Recurse for children
        // Use getChildren() which should be available from TreeNode
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                // Pass the incremented level to the recursive call
                printTree(child, level + 1);
            }
        }
    }

    /**
     * Finds the first SubscriberWithLocation encountered in a Breadth-First Search.
     */
    public static SubscriberWithLocation findFirstSubscriber(TreeNode startNode) {
        if (startNode == null) return null;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(startNode);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current instanceof SubscriberWithLocation sub) {
                return sub;
            }
            if (current.getChildren() != null) {
                for (TreeNode child : current.getChildren()) {
                    queue.offer(child);
                }
            }
        }
        return null; // Not found
    }

     /**
     * Finds the first PublisherWithLocation encountered in a Breadth-First Search.
     */
    public static PublisherWithLocation findFirstPublisher(TreeNode startNode) {
        if (startNode == null) return null;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(startNode);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current instanceof PublisherWithLocation pub) {
                return pub;
            }
            if (current.getChildren() != null) {
                for (TreeNode child : current.getChildren()) {
                    queue.offer(child);
                }
            }
        }
        return null; // Not found
    }

    /**
     * Prints the subscription table for all brokers in the tree.
     */
    public static void printAllSubscriptionTables(TreeNode startNode) {
         if (startNode == null) return;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(startNode);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current instanceof BrokerWithRegion broker) {
                 broker.printSubscriptionsTable(); // Assuming this method exists
            }
            if (current.getChildren() != null) {
                for (TreeNode child : current.getChildren()) {
                    queue.offer(child);
                }
            }
        }
    }
}

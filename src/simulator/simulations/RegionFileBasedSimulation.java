package simulator.simulations;

import java.io.File;

import simulator.SimulationRunner;
import simulator.regions.BrokerWithRegionProcessingRegion;
import simulator.regions.LeafBrokerWithRegionProcessingRegion;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;

/**
 * Concrete SimulationRunner for topologies loaded from a JSON file via FileBasedTopologyGenerator.
 * Assumes the JSON file defines the broker hierarchy. Subscribers and Publishers
 * are NOT automatically created by the FileBasedTopologyGenerator by default.
 */
public class RegionFileBasedSimulation extends
    SimulationRunner<FileBasedTopologyConfiguration,           // Configuration type
                     BrokerWithRegionProcessingRegion,         // Root node type
                     FileBasedTopologyGenerator> {             // Factory type

    @Override
    protected void executeScenarios() {
        System.out.println("\n--- Running Region Simulation Scenarios (File-Based Topology) ---");

        // Find the root broker
        BrokerWithRegionProcessingRegion rootBroker = this.rootNode;
        if (rootBroker != null) {
            System.out.println("Topology Root Broker: " + rootBroker.getName() + ", Region: " + rootBroker.getRegion());
        } else {
            System.err.println("Error: Topology root is null after generation.");
            return;
        }

        // Example: Find the first leaf broker
        LeafBrokerWithRegionProcessingRegion firstLeafBroker = findFirstNodeOfType(LeafBrokerWithRegionProcessingRegion.class);

        if (firstLeafBroker != null) {
             System.out.println("\n>>> Scenario: Example Interaction with a Leaf Broker <<<");
             System.out.println("Found Leaf Broker: " + firstLeafBroker.getName() + ", Region: " + firstLeafBroker.getRegion());

             // --- Placeholder for scenarios involving manually added clients ---
             // Example:
             // SubscriberWithLocation attachedSub = findAttachedSubscriber(firstLeafBroker);
             // if (attachedSub != null) { ... send subscription ... }

             // Print subscription table of this leaf broker
             System.out.println("Subscription table for " + firstLeafBroker.getName() + ":");
             printBrokerSubscriptionTable(firstLeafBroker); // Use local helper

        } else {
            System.out.println("Could not find a LeafBrokerWithRegionProcessingRegion in the generated topology.");
        }

        // Print all broker subscription tables using the helper from the superclass
        System.out.println("\n>>> Scenario: Printing all broker subscription tables <<<");
        printAllBrokerSubscriptionTables(); // Use helper from SimulationRunner

        // --- Placeholder for more complex scenarios ---
        // Manually create/attach clients and initiate interactions here if needed.

        System.out.println("\n--- Region File-Based Simulation Scenarios Complete ---");
    }

    /**
     * Helper method to print the subscription table of a single broker.
     * @param broker The broker whose table should be printed.
     */
    protected void printBrokerSubscriptionTable(BrokerWithRegionProcessingRegion broker) {
        if (broker == null) return;
        System.out.println("  Table for: " + broker.getName());
        // Use getSubscriptionsTable() which should be available from SimulationBroker/BrokerWithRegion
        if (broker.getSubscriptionsTable() == null || broker.getSubscriptionsTable().isEmpty()) {
            System.out.println("    <empty>");
        } else {
            // Assuming getSubscriptionsTable returns Map<TreeNode, SimulationSubscription>
            broker.getSubscriptionsTable().forEach((node, sub) -> {
                System.out.println("    Source: " + (node != null ? node.getName() : "null") + " -> Sub: " + sub);
            });
        }
    }

    @Override
    protected void printTopologyStructure() {
        System.out.println("Topology too big to print structure.");
    }

    


    public static void main(String[] args) {
        String topologyJsonFilePath = "output/geonames_topology.json";

        System.out.println("--- Starting Simulation Setup (File-Based Topology) ---");
        System.out.println("Topology Source File: " + topologyJsonFilePath);
        System.out.println();

        // Check if file exists
        File topologyFile = new File(topologyJsonFilePath);
        if (!topologyFile.exists() || !topologyFile.isFile()) {
             System.err.println("Error: Topology JSON file not found or is not a file: " + topologyJsonFilePath);
             System.err.println("Please ensure the file exists and the path is correct.");
             return;
        }

        // Configuration
        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration(topologyJsonFilePath);

        // Factory
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator();

        // Create and Run Simulation
        RegionFileBasedSimulation simulation = new RegionFileBasedSimulation();
        simulation.run(factory, config); // run() handles generation and scenario execution

        // Note: cleanup() is called automatically by run() in the superclass
    }
}

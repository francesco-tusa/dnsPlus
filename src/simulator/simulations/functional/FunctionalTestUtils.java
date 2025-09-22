package simulator.simulations.functional;

import java.util.Map;
import simulator.core.TreeNode;
import simulator.entities.SimulationBroker;
import simulator.events.SimulationSubscription;
import simulator.regions.BrokerWithRegionProcessingRegion;

/**
 * A utility class containing shared helper methods for running functional tests.
 */
public final class FunctionalTestUtils {

    /**
     * Traverses the broker topology and prints the relevant subscription tables for each broker.
     * For region-based brokers, it prints both the main subscription table and the
     * propagated subscriptions table. For all other broker types, it prints only the main table.
     * @param node The starting node of the topology to print (usually the root).
     */
    public static void printAllSubscriptionTables(TreeNode node) {
        if (node instanceof SimulationBroker broker) {
            // Print the main subscription table for all broker types
            System.out.println("\n--- Subscription Table for: " + broker.getName() + " ---");
            Map<TreeNode, SimulationSubscription> table = broker.getSubscriptionsTable();
            
            if (table.isEmpty()) {
                System.out.println("  (empty)");
            } else {
                String formatString = "  %-20s | %s%n";
                System.out.printf(formatString, "Source Node", "Subscription Details");
                System.out.println("-----------------------+--------------------");
                for (Map.Entry<TreeNode, SimulationSubscription> entry : table.entrySet()) {
                    System.out.printf(formatString, entry.getKey().getName(), entry.getValue());
                }
            }

            // If it's a region-processing broker, also print the propagated subscriptions table
            if (broker instanceof BrokerWithRegionProcessingRegion regionBroker) {
                System.out.println("\n--- Propagated Subscriptions for: " + regionBroker.getName() + " ---");
                Map<TreeNode, SimulationSubscription> propagatedTable = regionBroker.getPropagatedSubscriptions();

                if (propagatedTable.isEmpty()) {
                    System.out.println("  (empty)");
                } else {
                    String formatString = "  %-20s | %s%n";
                    System.out.printf(formatString, "Propagated To", "Subscription Details");
                    System.out.println("-----------------------+--------------------");
                    for (Map.Entry<TreeNode, SimulationSubscription> entry : propagatedTable.entrySet()) {
                        System.out.printf(formatString, entry.getKey().getName(), entry.getValue());
                    }
                }
            }
        }

        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                printAllSubscriptionTables(child);
            }
        }
    }
}
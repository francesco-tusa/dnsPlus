package simulator.simulations.functional;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import simulator.core.TreeNode;
import simulator.entities.SimulationBroker;
import simulator.events.SimulationSubscription;
import simulator.regions.SpatialMatchBroker;
import utils.CustomLogger;

/**
 * A utility class containing shared helper methods for running functional tests.
 */
public final class FunctionalTestUtils {

    private static final Logger logger = CustomLogger.getLogger(FunctionalTestUtils.class.getName());

    /**
     * Traverses the broker topology and prints the relevant subscription tables for each broker.
     * For region-based brokers, it prints both the main subscription table and the
     * propagated subscriptions table. For all other broker types, it prints only the main table.
     * @param node The starting node of the topology to print (usually the root).
     */
    public static void printAllSubscriptionTables(TreeNode node) {
        if (node instanceof SimulationBroker broker) {
            // Print the main subscription table for all broker types
            String mainTableTitle = "\n--- Subscription Table for: " + broker.getName() + " ---";
            logger.info(mainTableTitle);
            
            Map<TreeNode, List<SimulationSubscription>> inputTable = broker.getInputSubscriptions();

            if (inputTable.isEmpty()) {
                logger.info("  (Input Store empty)");
            } else {
                for (Map.Entry<TreeNode, List<SimulationSubscription>> entry : inputTable.entrySet()) {
                    String subDetails;
                    if (entry.getValue().isEmpty()) {
                        subDetails = "[Empty List]";
                    } else if (entry.getValue().size() == 1) {
                        subDetails = entry.getValue().get(0).toDisplayString();
                    } else {
                        subDetails = entry.getValue().size() + " entries (Multi-Region)";
                    }
                    logger.info(String.format("  FROM %-20s | %s", entry.getKey().getName(), subDetails));
                }
            }

            // 2. Print Output Store (Uniformly via SimulationBroker)
            Map<TreeNode, List<SimulationSubscription>> outputTable = broker.getPropagatedSubscriptions();

            if (!outputTable.isEmpty()) {
                logger.info("  --- Propagated Subscriptions ---");
                for (Map.Entry<TreeNode, List<SimulationSubscription>> entry : outputTable.entrySet()) {
                    String subDetails;
                    if (entry.getValue().isEmpty()) {
                        subDetails = "[Empty List]";
                    } else if (entry.getValue().size() == 1) {
                        subDetails = entry.getValue().get(0).toDisplayString();
                    } else {
                        subDetails = entry.getValue().size() + " entries";
                    }
                    logger.info(String.format("  TO   %-20s | %s", entry.getKey().getName(), subDetails));
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

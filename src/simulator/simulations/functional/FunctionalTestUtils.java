package simulator.simulations.functional;

import java.util.Map;
import java.util.logging.Logger;
import simulator.core.TreeNode;
import simulator.entities.SimulationBroker;
import simulator.events.SimulationSubscription;
import simulator.regions.BrokerWithRegionProcessingRegion;
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
            
            Map<TreeNode, SimulationSubscription> table = broker.getSubscriptionsTable();
            
            if (table.isEmpty()) {
                logger.info("  (empty)");
            } else {
                String formatString = "  %-20s | %s";
                logger.info(String.format(formatString, "Source Node", "Subscription Details"));
                logger.info("-----------------------+--------------------");
                for (Map.Entry<TreeNode, SimulationSubscription> entry : table.entrySet()) {
                    logger.info(String.format(formatString, entry.getKey().getName(), entry.getValue()));
                }
            }

            // If it's a region-processing broker, also print the propagated subscriptions table
            if (broker instanceof BrokerWithRegionProcessingRegion regionBroker) {
                String propagatedTableTitle = "\n--- Propagated Subscriptions for: " + regionBroker.getName() + " ---";
                logger.info(propagatedTableTitle);
                
                Map<TreeNode, SimulationSubscription> propagatedTable = regionBroker.getPropagatedSubscriptions();

                if (propagatedTable.isEmpty()) {
                    logger.info("  (empty)");
                } else {
                    String formatString = "  %-20s | %s";
                    logger.info(String.format(formatString, "Propagated To", "Subscription Details"));
                    logger.info("-----------------------+--------------------");
                    for (Map.Entry<TreeNode, SimulationSubscription> entry : propagatedTable.entrySet()) {
                        logger.info(String.format(formatString, entry.getKey().getName(), entry.getValue()));
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

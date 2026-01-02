package simulator.tests.framework;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import simulator.core.TreeNode;
import simulator.entities.SimulationBroker;
import simulator.events.SimulationSubscription;
import utils.CustomLogger;

public final class FunctionalTestUtils {

    private static final Logger logger = CustomLogger.getLogger(FunctionalTestUtils.class.getName());

    public static void printAllSubscriptionTables(TreeNode node) {
        if (node instanceof SimulationBroker broker) {
            String mainTableTitle = "\n--- Subscription Table for: " + broker.getName() + " ---";
            logger.info(mainTableTitle);
            
            Map<TreeNode, List<SimulationSubscription>> inputTable = broker.getInputSubscriptions();
            if (inputTable.isEmpty()) {
                logger.info("  (Input Store empty)");
            } else {
                for (Map.Entry<TreeNode, List<SimulationSubscription>> entry : inputTable.entrySet()) {
                    String subDetails = entry.getValue().isEmpty() ? "[Empty List]" : 
                        (entry.getValue().size() == 1 ? entry.getValue().get(0).toDisplayString() : entry.getValue().size() + " entries");
                    logger.info(String.format("  FROM %-20s | %s", entry.getKey().getName(), subDetails));
                }
            }

            Map<TreeNode, List<SimulationSubscription>> outputTable = broker.getPropagatedSubscriptions();
            if (!outputTable.isEmpty()) {
                logger.info("  --- Propagated Subscriptions ---");
                for (Map.Entry<TreeNode, List<SimulationSubscription>> entry : outputTable.entrySet()) {
                    String subDetails = entry.getValue().isEmpty() ? "[Empty List]" : 
                        (entry.getValue().size() == 1 ? entry.getValue().get(0).toDisplayString() : entry.getValue().size() + " entries");
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